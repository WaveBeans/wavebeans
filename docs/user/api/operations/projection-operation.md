Projection operation
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Working with different types](#working-with-different-types)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Overview
--------

When you're working with the stream sometimes you may need to get the subset of the stream. For this purpose projection of the stream is built. For a positioning a time scale is used. When defining a projection you need to specify the time marker of the start, which can be 0 for beginning of the stream or any other time value more than 0; the end marker is optional, you may leave it `null` if you want to have unlimited at the end stream.

To use a projection on the stream call `rangeProjection()` function, you would need to specify `start` (`value > 0`), `end` (`null` or `value > start`) and `timeUnit` (`TimeUnit.MILLISECONDS` is default value):

```kotlin
val stream = anyStream()
// start on 100 ms and not limit at end end
stream.rangeProjection(100)
// start on 100 ms and limit with 200 ms at the end
stream.rangeProjection(100, 200)
// start on the beginning and limit with 200 nanoseconds at the end
stream.rangeProject(0, 200, TimeUnit.NANOSECONDS)
// or use named parameters
stream.rangeProjection(
    start = 100,
    timeUnit = TimeUnit.NANOSECONDS
)
```

Worth to mention, if you limit the stream at the end, you'll effectively convert it to a finite stream, however it's not actually a real finite stream. There is a way to convert it to a proper `FiniteStream` so then you can use [finite converters](../inputs/finite-converters.md) to use it as a infinite stream if required. For that purpose you may use [trim](trim-operation.md) operation with the exactly the same length you have made the projection with, it'll convert it to a finite stream:

```kotlin
val stream = anyStream()

// let's define the length upwards so we won't mess with parameters
val length = 50

// here it is effectively finite stream, but not actually
val rangeStream = stream.rangeProjection(100, 100 + length, MILLISECONDS)

// it is a proper finite stream
val finiteRangeStream = rangeStream.trim(length)

// use zero filling conversion to an infinite stream
val infiniteRangeStream = finiteRangeStream.sampleStream(ZeroFilling()) 
```

Working with different types
----------

Projection operation is defined for `Sample` and `Window<Sample>` types out of the box, but it's not limited to them. Only thing you need to keep in mind, that projection calculates time when the stream is being executed and the sample rate is provided, so it needs a way to convert the size of your type to samples to correctly calculate time markers, i.e. for `Sample` the size is always 1, for windowed samples the size is th size of the window step.

To use your own type you need to define how to measure it

One way is to implement the `io.wavebeans.lib.stream.Measured` interface for you class:

```kotlin
data class DoubleSample(val one: Sample, val two: Sample) : Measured {
    override fun measure(): Int = 2
}
```

Another way is to register it before it's being executed, preferrably to be used for the classes you can't extend like SDK classes:

```kotlin
data class DoubleSample(val one: Sample, val two: Sample)
SampleCountMeasurement.registerType(DoubleSample::class) { 2 }
```

And now you can use it:

```kotlin
440.sine().window(2)
        .map { DoubleSample(it.elements[0], it.elements[1]) }
        .rangeProjection(100, 200)
```

If you won't register the type, during execution you'll have an exception like `class my.wavebeans.DoubleSample is not registered within SampleCountMeasurement, use registerType() function or extend your class with Measured interface`

The following types are built-in:
* `Number` -- always return 1 
* `Sample` -- always return 1 
* `FftSample` -- measured as the `window.step` it is built on top of. 
* `List<T>` -- measured as a sum of length of all corresponding elements of type `T`. Doesn't support nullable elements, will throw an exception.
* `Window<T>` -- measured as `sizeOfTheSample * window.step`, where `sizeOfTheSample` is measure of the first element.