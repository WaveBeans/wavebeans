API reference
==========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Inputs](#inputs)
- [Outputs](#outputs)
- [Operations](#operations)
- [Types](#types)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Overview
---------

WaveBeans provides the one atomic entity called a Bean which may perform some operations. They are a few different types:

1. `SourceBean` -- the bean that has only output, the one the whole stream can read from. Such beans are [Inputs](#inputs), for example.
2. A `Bean`, which can have one or more input or outputs. This basically are operator that allows you perform an operation on sample, convert sample to something else, alter the stream, or merge different streams together. One operation at once, though the operation may do a lot of computations at once, not just one.
3. `SinkBean` -- the bean has no outputs, this is the ones that dumps the audio samples onto disk or something like this, so called [outputs](#outputs)

The samples are starting their life in SourceBean then by following a mesh of other Beans which changes them are getting stored or distributed by SinkBean.

WaveBeans uses declarative way to represent the stream, so you first define the way the samples are being altered or analyzed, then it's being executed in most efficient way. That means, that effectively SinkBean are pulling data out of the stream, and all computations are happened on demand at the time they are needed. Such stream is called `BeanStream<T>`, it has a type parameters which represent what is inside the stream, i.e. `BeanStream<Sample>` is the stream of samples. The type `T` is non-nullable.

Inputs
--------

Inputs allow you to generate some data that the whole other stream will then process, alter, slice and dice. Whatever is required. You can choose to read the input from file like WAV file, or you may generate the input based on some mathematical function like sine.

There a few different types of inputs, you may read more in specific:

* [sine](inputs/sines.md)
* [wav-files](inputs/wav-file.md)
* [custom defined function](inputs/function-as-input.md)

Also, as all streams in WaveBeans considered to be infinite, there is extra functionality to convert finite streams like wav-files, read more about [finite converters](inputs/finite-converters.md)

Outputs
--------

Outputs serve two main purposes: define what to do with all that sampled data -- where to store it or hand it over to, and it is a terminal action it allows to launch all the computations through all defined operations. Frankly speaking, you may define your low level kinda output at any operation, but just calling `asSequence()` which returns you a regular Kotlin Sequence and read the stream as is; just remember, in most cases streams are considered to be infinite, so just regular call `toList()` will never finish.

Outputs consume specific type and and in some case it may work only with specific type of `BeanStream`.

Output provides access to `Writer` which performs all the operations. Writer has only one method `write()` which when called performs one iteration across the whole stream -- i.e. generate the sine of 440Hz and then reduce the sine amplitude and store 1 second of resulted audio stream to the file. Method call return boolean value stating if that was the last iteration or there's more to do. You must close the output to make sure all buffers are flushed and everything is committed as requested. For most of the ouput types, you may close the output regardless if it finishes its work. 

The definition of the output would like this:

```kotlin
val output = 440.sine()
    .map { it / 2 }
    .trim(1000)
    .toMono16BitWav("file:///sine440.wav")
```

And then to execute single output you may create a writer and call `write()`method while it returns `true`. It requires to specify the sample rate of the whole stream:

```kotlin
output.writer(44100.0f).use { writer ->
    while(writer.write()) {}
}
```

Here is the list of supported outputs at the moment:

* [CSV](outputs/csv-outputs.md)
* [WAV](outputs/wav-output.md) 
* [/dev/null](outputs/dev-null-output.md)  

Operations
--------

To connect inputs and outputs feeling the stream with the meaning, you'll always define a set of operations. That allows you to change the stream characteristics, merge different streams together, form a new types of the streams and convert it back. A list of available operations sometimes depend on the type of the stream, however there are operations that may work with any type and even convert it to a different type.

To use an operation you can call specific method on the stream, i.e. here the map operation called which changes the type of the string to -1 or 1 depending on the input value:

```kotlin
440.sine() // create a stream of samples
    .map { if (it > 0) 1 else -1} // calling an operation on the stream.
```

The list of supported operations are:

* [Arithmetic operations](operations/arithmetic-operations.md)
* Specific operations for stream of type `Sample`
    * [change amplitude](operations/change-amplitude-operation.md) -- change the value of the sample by scalar value.
    * [trim](operations/trim-operation.md) -- cutting the infinite stream to become finite.
* Specific operations for stream of type `Window<Sample>`
    * [converting to FFT](operations/fft-operation.md) -- running FFT analysis on the stream.
* Operations for any stream regardless of the type:
    * [map](operations/map-operation.md) -- changing the input object or converting it to a different type.
    * [merge](operations/merge-operation.md) -- merging two different streams of the same type to one stream.
    * [projection](operations/projection-operation.md) -- getting a sub-stream of the stream
    * [window](operations/window-operation.md) -- grouping a sequence of objects of defined length to handle them all at once. 
    

Types
--------

Each `Bean` has input and output type, sometimes they might the same, sometimes might be different. Even more, the flexibility of using Beans, you can use any types of your own.

In that section, it'll be covered what types are provided out of the box and what API they do have.

**Sample**

Sample is first-class citizen in WaveBeans. It holds the smallest piece of information available -- an audio sample. Internally, sample represented as 64-bit floating point value, its value is usually between -1 and 1 (including), which is basically amplitude of the signal at the certain point of time. So any input signal regardless of its type is being converted to this range, i.e. if the signal is represented as a 16-bit integer, its values are between -32768 and 32767, that ismapped to dynamic range from -1 to 1. But at the same time that means, while the stream is being processed the amplitude might be times higher than 1.0 without any cropping. However, you have to remember to return it back to range (-1, 1) before saving to certain formats like wav-files which doesn't support that.

You can create sample out of pretty much every number by using `sampleOf()` function. Depending on the type of the input variable it'll be represented as with certain bit depth within the range (-1.0,1.0).

| Bit depth      | Kotlin type to use | Values range                                | Code example                   |
|----------------|--------------------|---------------------------------------------|--------------------------------|
| 8 bit          | `Byte`             | (-128, 127)                                 | `sampleOf(100.toByte())`       |
| 16 bit         | `Short`            | (-32768, 32767)                             | `sampleOf(100.toShort())`      |
| 24 bit         | `Int`              | (-8388607, 8388608)                         | `sampleOf(100, as24bit = true)`|
| 32 bit         | `Int`              | (-2147483648, 2147483647)                   | `sampleOf(100)`                |
| 64 bit         | `Long`             | (-9223372036854775808, 9223372036854775807) | `sampleOf(100L)`               |

Also you may create a sample out of floating point types `Float` (32 bit) and `Double` (64 bit) which won't be converted to a (-1,1) range and will be used as is, but it is also limited according to [floating point storage rules](https://en.wikipedia.org/wiki/Floating-point_arithmetic#Range_of_floating-point_numbers).

Of course there are methods to convert back from sample according to its desired representation: `Sample.asLong()` to 64 bit representation, `Sample.asInt()` to 32 bit representation, `Sample.as24BitInt()` to 24 bit representation, `Sample.asShort()` to 16 bit representation, and `Sample.asByte()` or `Sample.asUnsignedByte()` to 8 bit representation.

`Sample` type supports all basic arithmetic operations like sum, subtract, multiply and divide, including if sample is `null` -- it replaces it with `ZeroSample` which is also defined as globally accessible constant:

```kotlin
val a = sampleOf(0.1)
val b = sampleOf(0.2)
val c: Sample? = null

a + b          // = "0.3"
a + c          // = "0.1"
a - b          // = "-0.1"
a - c          // = "0.1"
a * b          // = "0.02"
b * c          // = "0.0"
b * ZeroSample // = "0.0"
b / a          // = "2.0"
```

Also you may compare samples between each other and with numeric constants, however it doesn't support nullable samples, so you would need to replace it yourself:

```kotlin
val a = sampleOf(0.1)
val b = sampleOf(0.2)
val c: Sample? = null
val d = sampleOf(0.2)

a >= b               // = false
a > c ?: ZeroSample  // = true
a == b               // = false
d == b               // = true
d != a               // = true
d == 0.2             // = true
ZeroSample == 0.0    // = true
a - b < 0            // = true
```

**Window<T>**

Windows are used to group a set of values to behave as one single value and be processed all at once during one iteration. For example samples, all at once within one operation. The good usage example might be grouping `Sample`s into groups of 512-ish `Sample`s, to perform the FFT computation.

Window type has type-parameter as it can hold any type, even another Window. The only restriction, that type should be non-nullable.

The following information you can find inside the Window. It doesn't include only information about the window values, but also stores attributes the window was created with:
* `size` -- the size of the window it was created with.
* `step` -- the step the window is moving on each iteration. If the window is fixed, meaning windows has no intersection while moving, it should have the same value as size. If it has value less than size that means consequent windows will have shared elements. If that value is greater then size, that means windows are not intersecting but some of the values are dropped between iterations.
* `elements` -- the elements of this window. The amount of elements must be less or equal to size. If elements has less values than size, missed elements will be replaced by call to zeroEl function.
* `zeroEl` -- if elements has not enough element during some operations, it'll be replace by zero elements generated by this function.

The window has certain restrictions:
* the `size` and `step` should be greater or equals to 1
* you can't create fully empty window without any elements, at least one element should be there.

You can create the Window of certain by calling the constructor of this class, for `Sample` type there is a helper method. The following code is similar:

```kotlin
val samples = listOf(0.1, 0.2, 0.3, 0.4).map { sampleOf(it) }

// creating a window using constructor
Window(2, 2, samples) { ZeroSample }

// creating a window using built-in helper function
Window.ofSamples(2, 2, samples)
```

**FftSample**

FftSample is the type used by FftStream and is converted by variety of ways from a bunch of Samples. It is a complex object that consists of following fields:
* `time` -- the time marker of this sample, in nano seconds.
* `binCount` -- number of bins of this FFT calculations, i.e. 512, 1024
* `sampleRate` -- sample rate which was used to calculate the FFT
*  `fft` -- calculated FFT. 

There is also `magnitude()` and `phase()` methods to extract magnitude and phase respectively. `frequency()` method allows to extract the exact values of frequencies from bins.

Internally, FftSample type uses `ComplexNumber` to store calculated FFT samples, which is wrapper around two double values one of each represents real part and another represents imaginary part. All basic arithmetic operations are also implemented as well as simple way to create complex number out of any number. Comparison is implemented as comparison the real part first and if it's equals than comparison the imaginary part.

```kotlin
val a = 1.r     // create complex number with zero imaginary part
val b = 2.i     // create complex number with zero real part
val c = 3 + 4.i // create complex number with non-zero both parts

a + b //  1 + 2i
c - a //  2 + 4i
c - 3 //      4i
b * c // -8 + 6i
-a    // -1
a > b // true
```

**User defined type**

Using certain operations you can convert one type to another, and that new type can be used further down by the stream and then be converted to the one which is supported out of the box or used down to the SinkBean and be stored using that type. Or you can even return that type out of the Input and use all the way through to the end of the stream. Or use within windowing, or convert the FftSample directly. It's not limited, it's up to your requirement and understanding.

However there is a nuance. In distributed mode you may require to define that type in certain way, please follow the [documentation](../exe/readme.md).

Here is an example to illustrate how it may look like. It generates a set of samples based on cosine mathematical function, then group it by 10 samples, and get 2 first samples out of the group, and then outputs it to a CSV file with three columns: time, first sample and second sample.

```kotlin
data class TwoSamples(val sample1: Sample, val sample2: Sample)

input { (x, _) -> sampleOf(cos(x.toDouble())) }
        .trim(1)
        .window(10)
        .map { window -> TwoSamples(window.elements.first(), window.elements.drop(1).first()) }
        .toCsv(
                uri = "file:///path/to/file.csv",
                header = listOf("time sec", "sample1", "sample2"),
                elementSerializer = { (idx, sampleRate, value) ->
                    listOf(
                            String.format("%.5f", idx / sampleRate),
                            String.format("%.10f", value.sample1),
                            String.format("%.10f", value.sample2)
                    )
                }
        )
```

It will generate something like this

```csv
time sec,sample1,sample2
0.00000,1.0000000000,0.5403023059
0.00002,-0.8390715291,0.0044256980
0.00005,0.4080820618,-0.5477292602
0.00007,0.1542514499,0.9147423578
0.00009,-0.6669380617,-0.9873392775
```