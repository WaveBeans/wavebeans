# Execution

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Single-threaded mode](#single-threaded-mode)
- [Multi-threaded mode](#multi-threaded-mode)
- [Distributed mode](#distributed-mode)
  - [Overseer](#overseer)
  - [Facilitators](#facilitators)
  - [Serialization](#serialization)
  - [Fault-tolerance](#fault-tolerance)
- [Using writers](#using-writers)
- [Using sequence](#using-sequence)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

WaveBeans supports a few ways to launch the stream: in single threaded mode, in multi-threaded mode on one machine or in distributed mode (*EARLY ALPHA*) on a few machines. Despite the fact that this approach API is in active development and may change in the future, but it's definitely won't change drastically.

This article covers execution inside your applications, also you may consider launching via [command line interface](../cli/readme.md) which supports most of that functionality seamlessly.

It is recommended to read that section fully from top to bottom as each new section relies on what been said in the previous one.

## Single-threaded mode

It's the most straightforward way to execute a stream or a few. It runs all streams one-by-one in the order they've defined in the list you provide.

Here is the comprehensive list of actions:

1. You need to store all your stream outputs into a list. It's easy to identify them, the stream output is the reference to the stream that is return by operations like `toCsv()` or `toWav()` or similar.
    
```kotlin
val stream1 = 440.sine().trim(1000).toCsv()
val stream2 = 880.sine().trim(1000).toCsv()

val outputs = listOf(stream1, stream2)
```

2. Instantiate the `SingleThreadedOverseer` providing the list of the outputs as a constructor parameter:

```kotlin
val overseer = SingleThreadedOverseer(outputs)
```

3. Invoke `eval()` function of the created overseer object specifying the desired sample rate as a [Float number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html), i.e. 44100Hz as `44100.0f`. The method call returns the list of [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) objects that allows you to wait for results, one Future correspond to one output. The easiest way would be just to wait for all futures to resolve. You may check the results when the future object is resolved when `get()` function is called, it returns the `ExecutionResult` value. That class has field `finished` and it is `true` if the output was evaluated successfully, otherwise, if it is `false`, the execution of the output has failed and it's worth to check the `exception` field.

```kotlin
overseer.eval(44100.0f).all { it.get().finished }
```

4. When the overseer has finished evaluating, it needs to be closed:

```kotlin
overseer.close()
```
    
Now when we know all the steps, it's better to provide a short code snippet to summarize everything, assuming the `outputs` and `sampleRate` variables are already defined:

```kotlin
SingleThreadedOverseer(outputs).use { overseer ->
    val results = overseer.eval(sampleRate).map { it.get() }
    if (!results.all { it.finished }) {
        println("Execution failed.")
        results.mapNotNull { it.exception }
            .forEach { it.printStackTrace(System.err) }
    }
}
```

Internally, `SingleThreadedOverseer` creates a single-threaded execution pool, so your main thread remains runnable, that's why to avoid further running we're blocking the current thread on get calls to futures. You may implement it differently, just keep that in mind.

## Multi-threaded mode

Multi-threaded mode allows you to parallelize your execution by partitioning the stream and running using multiple threads. It is launched exactly the same as single-threaded mode, the only difference is you need to instantiate a different type of overseer specifying number of partitions to split by, and the number of threads to execute the streams on:

```kotlin
val overseer = MultiThreadedOverseer(outputs, threads, partitions)
```

A little more details about how that's happening. While streams are being prepared to run in parallel mode, the Topology is built in the following way:
1. All operations in the stream including inputs and outputs are represented as Beans -- the atomic entity in the topology. Beans are connected to each other, providing pieces of data when it is requested. 
2. All beans that can be partitioned are being split up, so the same bean could process different operations in parallel by processing different parts of the stream, some beans which doesn't allow partitioning will merge the parts together.
3. After all beans are defined they are being grouped into Pods. A pod is similar entity to a bean, works very similarly, but pod groups one or more beans together, and encapsulates the communication over the network if required.
4. A set of pods is being deployed onto a Bush which is controlled by the Gardener. In multi-threaded mode you would need to define amount of threads for this bush, and only one bush is being used in that case. Multiple amount of bushes are reserved for distributed deployment. 

The amount of pods and amount of threads may not correspond to each other, moreover you wouldn't even benefit from it as threads won't be 100% utilized. The model uses asynchronous mechanism of execution splitting up evaluation in small tasks, though it may even run this way on a single thread. The number of threads should be around the number of partitions or less, however that always depends on the topology.

There is one important thing that is drastically different between the single-threaded and multi-threaded executions you always need to keep in mind. During single threaded execution the inputs are being read or operations are being executed exactly the same number of times as you have outputs, as each output is processed separately one-by-one. In multi-threaded environment, the Pods implement the mechanism that once the data passed through the Bean, the same piece will be returned to any other bean which may claim it. That's very important difference should be kept in mind while developing your own functions, operations and inputs, especially if it has some state to manage.

## Distributed mode

Distributed mode allows to spread the execution across multiple servers in the same network. It is similar to multi-threaded execution but has a few key differences: the data is transfered over the network, hence it should be serializable, all participants in the flow is recommended to run the very same version to avoid any glitches. 

A few things you need to get acquinted before the start. Firstly, in distributed modes there are two main participants: Facilitator and Distributed Overseer (simply Overseer further down). Secondly, the actual execution happens across a few nodes, so for the storage always use something that is accessible from any of your machines, i.e. NFS, HDFS, S3, etc.

The distributed mode is very similar to multi-threaded one, with only exception that in distributed mode multiple threads additionally communicate with each other over the network.

### Overseer

Overseer distributes the work and assigns different parts across existing Facilitators. When the execution started, it controls its status. Overseer can be launched anywhere, just need to make sure all Facilitators are directly accessible for it over TCP protocol. It requires a little bit of resources, but is required to be up and running while the job is being evaluated. You can run a few Overseeers against the same Facilitator set at the same time if you're willing too.

To start the overseer you need to instantiate the Distributed Overseer in your app providing, in addition to regular list of outputs, the amount of partitions to split execution to, and a list of Facilitator locations:

```kotlin
val overseer = DistributedOverseer(
        outputs,
        listOf(
                "http://10.0.0.1:4000",
                "http://10.0.0.2:4000"
        ),
        10
)
```

The rest is nothing special and no different than multi-threaded or single-threaded execution.

### Facilitators

Facilitator is launched as a separate process within isolated JVM, so you can limit its CPU and memory usage (i.e. if you're running them inside the Docker container). To start the facilitator process once you downloaded the binaries of the framework, launch the `wavebeans-facilitator` specifying the configuration file as a parameter, to start Facilitators on several machines launch the command on all of them:

```bash
wavebeans-facilitator facilator.conf
```

You'll see the port it is listening on when it is started:

```text
Listening on port 4000
```

The configuration file is in the HOCON format and has the following format:

```hocon
facilitatorConfig {
    listeningPortRange: {start: 4000, end: 4000}
    threadsNumber: 1
}
```

It requires two items:
* `listeningPortRange` -- the range of ports to choose from during start up. Chooses the first random port if it's not occupied. If you want to specify specific port, make `start` and `end` the same.
* `threadsNumber` -- the number of worker threads to be used to execute the stream. It may use some more threads for different purposes, but they are not going to be that heavily occupied like that one. Consider specifying no more than you have CPU cores/vCPUs available.

The rest of the items are optional and always can be found if called for help:

```bash
wavebeans-facilitator --help
``` 

### Serialization

To be able to transfer objects over the network, they are needed to be serialized. All main types are already made serializable, but if you define your own type you need to make it serializable yourself. 
 
If you're using primitive types like String or Integer, you don't need to define its serialization routine. 

If you use classes which uses primitive values or another serizalizable class as its fields, it is enough to mark such class with `kotlinx.serialization.Serializable` annotation:

```kotlin
import kotlinx.serialization.*

@Serializable
class InnerType(val field: String)

@Serializable
data class MyType(val field1: Int, val field2: InnerType)
```

For more serialization techniques follow official documentation of [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization)

### Fault-tolerance

In current version of Distributed mode there is no recovery from any failure, the execution needs to be restarted from scratch and any outputs that didn't finish the execution are lost. That is essential for that type of execution, but still work in progress.

## Using writers

One of the low-level ways to read from the stream is to use writers provided by `StreamOutput` interface. You can create a writer by calling appropriate method and while you're calling the `write()` method it'll perform the iterations. 

If `write()` method call returns `false` that means the stream is over. It may not finish forever if the stream you're reading from is infinite, so you'll decide on your own when it should be finished. Writers asks for the sample rate it should use to evaluate the stream.

Once the stream is finished or you decided to finish it, you must call the `close()` method to flush all interim buffers. `Writer` implements `Closeable` interface. For clarity about that interim buffers, for example wav file output writes the content length in the header of the file, so you need to know upfront amount of bytes to be written, which in case of infinite streams are not defined, so it writes content in a temporary file and then copies over during close action when the stream is finished and its length is defined. That is just one of the examples, there might be different circumstances, or it might not be that critical, but anyway don't forget to close it.

Every time you create a writer, the stream will be evaluated from the beginning. 

Mechanism of writers is used while you're evaluating streams in any mode -- either single or multi threaded mode, so everything written here is very important to understand while for example developing your own writer, not only using it. When you use that approach you're kinda not limited to single-threaded execution, however it's not recommended to use it in multi-threaded environment as is. By default, all beans are not developed in thread-safe manner.

Here is an example of evaluating using writers:

```kotlin
val stream = 440.sine().trim(1000).toCsv("file:///some.file")

stream.writer(sampleRate).use { writer ->
    while (writer.write()) {
        // usually nothing to do here
    }
}

// or calling everything explicitly, always consider using `.use{ } `
// or at least `try-finally` statements instead
val writer = stream.writer(sampleRate)
while (writer.write()) {  }
writer.close()
```

## Using sequence
 
The most low-level way to read the stream is provided by any `BeanStream<T>` of any type. The method called `asSequence()` which has one parameter -- sample rate. When it is called the bean data will be returned as [Kotlin Sequence](https://kotlinlang.org/docs/reference/sequences.html), while reading data from the last bean, it'll automatically read the data from all over-lying beans.

That approach is used to connect beans to each other among other approaches listed above. Effectively when you're defining operations they are being nested and stored forming some sort of topology, and then when you call `asSequence()` the bean automatically calls the same method of all immediately connected beans, and they do the same, and so on and so forth, until it reached inputs which will start producing the pieces of data. That piece of data is being passed through all beans in a reverse way, each bean will try to do something with it and providing its output to the underlying being. And finally you'll get that drastically changed piece of data as an item of the sequence, which you can process further applying any operations provided by Kotlin Sequence SDK.

If the stream is infinite, the Kotlin Sequence will also not limit it by itself, you'll need to do it on your own by using appropriate API.

Another important thing to bear in mind, sequences are starting evaluation only when the terminal action is specified. Hence despite the fact that the sequences are going to be initiated across all related beans, it won't start actual processing without terminal action like `toList()` or similar.  

Here is an example:

```kotlin
// stream. Let's keep it simple
val stream = 440.sine() // BeanStream<Sample>

// create a reqular Kotlin Sequence
val sequence = stream.asSequence(sampleRate) // Sequence<Sample>

// take 1000 samples and store it to a list
sequence.take(1000).toList() // List<Sample>

// that will never finish as there is no limits on the stream
sequence.toList()
```
