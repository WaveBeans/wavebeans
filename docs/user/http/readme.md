# HTTP API

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [In-application usage](#in-application-usage)
- [Table Service](#table-service)
  - [Builtin type support](#builtin-type-support)
    - [`FftSample` schema](#fftsample-schema)
    - [`Window<T>` schema](#windowt-schema)
    - [`List<T>` schema](#listt-schema)
  - [Custom types](#custom-types)
    - [Custom serializer](#custom-serializer)
  - [Measuring](#measuring)
- [Audio Service](#audio-service)
- [Distributed mode](#distributed-mode)
- [Helper Types](#helper-types)
  - [Time Measure](#time-measure)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

WaveBeans framework allows expose to access some of the data to be expose via HTTP API. While the stream is being evaluated you may start the HTTP Server. It is possible via [CLI](../cli/#http-api) or in your application.

Http Service provides access to different underlying service implementations which has different purpose with REST API:
* [Table Service](#table-service): querying [tables](../api/outputs/table-output.md).
* [Audio Service](#audio-service): streaming sample data from tables.

If everything is good, the service returns 200 OK HTTP code.

## In-application usage

HTTP library is provided separately to avoid increasing artifact size without necessity. So first of all you need to add it as a dependency, i.e. for Gradle:

```groovy
dependecies {
    implementation "io.wavebeans:http:$wavebeans_version"
}
```

And on your application launch (or when you actually need it) start the WbHttpService providing the port you want to run it on, by default it'll start on port 8080. `WbHttpSerrice` implements `Closeable` interface, and calling `close()` when you finish is essential.

```kotlin
WbHttpService(serverPort = 12345).use {server ->
    server.start()

    // can do something here while the server is running
}

// or in more manual mode

// start the server
val server = WbHttpService().start()
//run queries over HTTP using any HTTP client
server.close()
```

`start()` method also has parameter `andWait` that allows to control whether the current thread will be freed up for further actions.

## Table Service

Table Service allows you to query data being stored in [tables](../api/outputs/table-output.md). All methods are start with `/table` path, then the name of the table is specified `/table/{tableName}` and then after it the method with its parameters.

Following methods available, mostly this methods are exposed from [table query functionality](../api/outputs/table-output.md#querying):

* Getting last Interval: `/table/{tableName}/last?interval={interval}[&sampleRate={sampleRate}]`:
    * `tableName` -- the name of the table to query, if the table can't be found the API call with return `404 Not Found` HTTP code.
    * `interval` -- the interval you're requesting, the type of [TimeMeasure](#time-measure). If malformed you'll see `400 Bad Request` HTTP code.
* Getting specific time range: `/table/{tableName}/timeRange?from={from}&to={to}[&sampleRate={sampleRate}]`:
    * `tableName` -- the name of the table to query, if the table can't be found the API call with return `404 Not Found` HTTP code.
    * `from`, `to` -- the from and to values of the interval you're requesting, the type of [TimeMeasure](#time-measure). If malformed you'll see `400 Bad Request` HTTP code.

Both endpoints return stream as new-line separated JSON objects like this:

```json
{"offset":368162653061,"value":0.6921708580045118}
{"offset":368162675736,"value":0.6455957967580076}
{"offset":368162698411,"value":0.5964844019471909}
{"offset":368162721086,"value":0.5450296159930959}
{"offset":368162743761,"value":0.4914335880185192}
{"offset":368162766436,"value":0.43590687909747267}
{"offset":368162789111,"value":0.3786676351923965}
{"offset":368162811786,"value":0.3199407308930018}
{"offset":368162834461,"value":0.2599568846828487}
```

The `offset` is the time marker of the sample in nanoseconds, the value is serialized as JSON for the sample value. For the type `Sample` it is simply `double` value. If you store the custom value in the table before returning it as a part of HTTP API you need to make it serializable. 

### Builtin type support

The following builtin types are supported out of the box for convenience:
 
 * `FftSample`
 * `Window<T>`, where `T` must be either builtin type or any other custom serializable type
 * `List<T>`, where `T` must be either builtin type or any other custom serializable type
 
All types follow the same pattern which is new-line separated json objects, but has different value schemas:

```json
{ 
  "offset": 123,// the offset as Long number in nanoseconds
  "value": ...  // value specific schema
}
```

#### `FftSample` schema

The FFT Sample returned as full object with every single value calculated (e.g. magnitude, phase, etc), despite the fact in the program usage it is being calculated on the fly. This is how the object looks like:
 
```json
{ 
    "index": 0,                 // Int value of the index of the FFT sample
    "binCount":   4,            // Int value of the bins count
    "samplesCount": 2,          // Int number of samples the FFT is calculated on
    "sampleRate": 44100.0,      // Float number of sample rate the stream is being evaluated with
    // for the folowing arrays the index correponds to phase and magnitude arrays
    "magnitude": [1.0,2.0,3.0,4.0], // array of Double values of magnitude values of the FFT
    "phase":     [1.0,2.0,3.0,4.0], // array of Double values of phase values of the FFT
    "frequency": [1.0,2.0,3.0,4.0], // array of double of frequencies values  
    "time": 0                       // Long value of the time marker of the sample, in nano seconds
}
```

#### `Window<T>` schema

If the stream is some windowed serializable type `T`, it'll return the following schema:

```json
{
    "size": 4, // Int value of the window size
    "step": 2, // Int value of the windown step
    "elements": [obj1, obj2, obj3, obj4], // array of objects which are serialized according to their own rules
    "sampleType": "my.application.MySample" // the string qualifying the class of the sample (which is T)
```

The type `T` must be either builtin type or custom type with provided mechanism of serialization as by [next section](#custom-types)

#### `List<T>` schema

If the stream is some list of serializable type T, it'll return the objects as an array:

```json
[
  obj1,
  obj2,
  obj3,
  obj4
]
```

### Custom types

In order to be used custom types needs to be made serializable. If you're using primitive types like String or Integer, you don't need to define its serialization routine. 

If you use classes which uses primitive values as its fields it is enough to mark such class with `kotlinx.serialization.Serializable` annotation:

```kotlin
import kotlinx.serialization.* // import package however it is not required if you'll specify it explicitly with the annotation

@Serializable // specify the annotation over your class
data class MyType(val field1: Int, val field2: String)
```

For more serialization techniques follow official documentation of [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization)

#### Custom serializer

If for any reason using `@Serializable` doesn't work for you, you can develop your own serializer and register it for your class.

Let's consider having a simple class `B` which has only one field:

```kotlin
data class B(val v: String)
```

The serialization routine is defined as per [`kotlinx.serialization documentation`](https://github.com/Kotlin/kotlinx.serialization). Two methods are required to be implemented: `descriptor` and `serialize()`, `deserialize()` is not used, so it is up to you to implement it if you need it for any other purpose.

```kotlin
class BSerializer : KSerializer<B> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("B") {
        element("v", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): B = 
        throw UnsupportedOperationException("Don't need it")

    override fun serialize(encoder: Encoder, value: B) {
        val s = encoder.beginStructure(descriptor)
        s.encodeStringElement(descriptor, 0, value.v)
        s.endStructure(descriptor)
    }
}
```

Once the serializer is defined, you need to register it with the `JsonBeanStreamReader`.

```kotlin
JsonBeanStreamReader.register(B::class, BSerializer())
```

### Measuring

The table service has time as an argument, but the data in stream is in samples interpreted according to defined sample rate. Moreover, some of the streams group samples together working with grouped sample as one complex sample. Also you may define the type which is not known to the system and it is impossible to measure it automatically. HTTP Table service uses similar to [projection operation](../api/operations/projection-operation.md) way of measurement, please follow [appropriate section](../api/operations/projection-operation.md#working-with-different-types) for more details.

## Audio Service

Audio service allows you to get access to table of [Samples](../api/readme.md#sample) (or [SampleVectors](../api/readme.md#samplevector)) and convert it to a well known format like WAV on the fly. Currently only streaming use case is available.

To call the specific table to do an audio streaming, call the API over HTTP by path `/audio/{tableName}/stream/{format}`, where `tableName` is the name of the table you want to stream from, and `format` is the desired streaming format, at the moment `wav` only is supported.

The full signature is:

```text
/audio/{tableName}/stream/{format}?bitDepth={bitDepth}&limit={limit}
```

Additional useful parameters:
* `bitDepth` -- either 8, 16, 24, 32 or 64. The number of bits per sample to stream. FYI, wav-format support up to 32 bits per sample. By default, it is 16 bit.
* `limit` -- interval to limit by, follow [Time Measure](#time-measure) rules. By default, it is unlimited as not specified. 

Current considerations:
* The wav format requires to specify the length in the header. To make streaming possible the length value is populated with `Int.MAX_VALUE`. Depending on the number of bits and channels it may last for a few days nonstop. Then most players just stop playing sound, however the actual data is being transferred normally. 

## Distributed mode

While running in distributed mode, all data is being fetched from Facilitators, so the HTTP service needs to know where these services are, and where specific resources (i.e. tables) are located. To be able to do that you should enable `HttpCommunicator` on the service, and tell the location (address and port) of itself to the Distributed Overseer. When the topology is planned and distributed (and even re-planned) Overseer will tell where to find what and HTTP service will be able to call specific API in order to fetch needed data.

To start the HTTP service with Communicator enabled just specify its port among other parameters:

```kotlin
WbHttpService(
    serverPort = 12345, 
    communicatorPort = 4000
).start()
```

Then while launching the `DistributedOverseer` specify it as a `httpLocation` parameter, it'll do the rest:

```kotlin
DistributedOverseer(
  outputs = listOf(output1, output2),
  facilitatorLocations = listOf("facilitator1", "facilitator2"),
  httpLocations = listOf("httServiceNode:communicatorPort")
)
```

The port should be accessible only for Overseer and Facilitators, and shouldn't be shared outside, gRPC protocol is used for communication. The public port is still `serverPort` where it is accessible over HTTP protocol.

## Helper Types

### Time Measure

Time measure is the type that allows you specify the time markers. It looks like this: `100ms` -- 100 milliseconds, `1.0d` -- 1 day, etc. It consists of two parts number and time unit which is not split up by anything. The number defines the amount of time within defined unit, it is always integer, so if it is defined as double only the integer part will be used.

Number formats, examples:
* `100`, `100L` -- any integer and long values.
* double `100.2` or as float `100.2f`, however will be rounded down to `100`
* `1e2` -- the value is double, but has only integer part so will be interpreted as `100`
* `1.2e2` -- the value is double as well, but keeping in mind mantias the value will be interpreted as `1.2 * 100 = 120.0 = 120`
* `-100`, `-1.2` -- all negatives are also supported.

The second part is time unit which is 1 or 2 latin symbols, case doesn't matter:
* `ns` -- nanoseconds
* `us` -- microseconds
* `ms` -- milliseconds
* `s` -- seconds
* `m` -- minutes
* `h` -- hours
* `d` -- days
