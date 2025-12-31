Function as an Input
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Syntax](#syntax)
- [Low-level API](#low-level-api)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Predefined Inputs are very handy but unfortunately not all the time. There is always a use case that is not covered in the framework or won't be covered at all. For such purposes WaveBeans supports inputs as custom functions. It has some limits though which is related to the way WaveBeans is being executed, you may read more about such limitations in [functions reference](../functions.md), that won't be covered here, as it's not related to the input itself.

Inputs are being generated based on two values: 
* Sample index which is growing every time the function is called, from 0 to basically infinity (2^63 to be exact, or Long.MAX_VALUE)
* Sample rate, which is desired sample rate that the stream is working in. It is expected that the input will adopt it, or just simply throw an exception, if conversion is not supported.

Function technically can return any type, however some operators are defined only for specific types, so be aware.

Syntax
-----

There are two ways to create an input which is inherited from two ways you can define the function itself. 

**No parameters function**

The first one is allowed if you don't need to use any external parameters during runtime, in this case let's create a sine with amplitude 1 and frequency 440 Hz. It creates the infiite stream:

```kotlin
import kotlin.math.* // we're going to use some Kotlin SDK functionality

input { (sampleIndex, sampleRate) -> sampleOf(1.0 * cos(sampleIndex / sampleRate * 2.0 * PI * 440.0))}
``` 

If you want to create a finite stream, after some time you may return `null`, which will highlight that the stream is over:

```kotlin
input { (sampleIndex, sampleRate) -> if (sampleIndex < 10) sampleOf(sampleIndex) else null }
```

Note: here we've used helper function `sampleOf()` which converts any numeric type to internal representation of `Sample`.

**Parameterized function**

If you want to create an input that expect some parameters or data during runtime, you should use `ExecutionScope`. Let's take a look at the example. Let's say you want to define the sine input but frequency and amplitude are defined by parameters.

```kotlin
input(executionScope { 
    add("freq", 440.0) 
    add("amp", 1.0)
}) { (sampleIndex, sampleRate) ->
    val freq = parameters.double("freq")
    val amplitude = parameters.double("amp")
    sampleOf(amplitude * cos(sampleIndex / sampleRate * 2.0 * PI * freq))
}
``` 

That approach is very flexible as you basically can do whatever you want and even call third party libraries methods.

Low-level API
-------

As any input that one has lower level API which is just class `Input<T>`, where `T` is the type of the produced output. It works with a generation function of type `(Long, Float) -> T?`.

```kotlin
val inputFn = InputFn(frequency = 440.0, amplitude = 1.0)
Input(InputParams(
        generator = { idx, fs -> inputFn(idx, fs) }
))
```
