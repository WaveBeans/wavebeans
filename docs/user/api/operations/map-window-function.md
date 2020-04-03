# Map with Window function

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Stream of `Sample` type](#stream-of-sample-type)
- [Stream of any type](#stream-of-any-type)
- [Low Level API](#low-level-api)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

There are several cases that you may need to change every single element of the `Window<T>` using some function. Mainly it is used for merging the stream of windowed sample with a [window function](https://en.wikipedia.org/wiki/Window_function). The [`map`](map-operation.md) operation between two windows is limited to the same size windows, so it is not that simple to generate correctly configured stream. 

That operation implementation doesn't do anything you wouldn't be able to do on your own by implementing it directly, though it is always nice to have a better API.

Basically what this operation does, if you have source windowed stream `w`, and the function `f` with corresponding to the size of the window length, it multiplies corresponding values:

```text
(w[0], w[1], w[2], w[3]) \
                           => (w[0]*f[0], w[1]*f[1], w[2]*f[2], w[3]*f[3])
(f[0], f[1], f[2], f[3]) /
``` 

For convenience it is implemented for `Sample` type, but it can be used with any type of the stream. The functions work as an extension of `BeanStream<Window<Sample>>` and return the same type of the stream.

## Stream of `Sample` type

To multiply the source windowed stream with window function you need to use `.windowFunction()` on the stream which was already windowed. It gets either as a parameter lambda function `{ (i, n) -> sampleOf(...) }` or a class `Fn<Pair<Int, Int>, Sample>`, what are the the differences and limitations of both approaches please follow [functions documentation](../functions.md).

The arguments of the generation function are:
1. The index of the sample in the window (`Int`)
2. The overall number of samples in the window (`Int`) 

```kotlin
// via lambda
440.sine()
        .window(401)
        .windowFunction { (i, n) ->
            val halfN = n / 2.0
            sampleOf(1.0 - abs((i - halfN) / halfN))
        }

// via class definition
class TriangularFn: Fn<Pair<Int, Int>, Sample>() {
    override fun apply(argument: Pair<Int, Int>): Sample {
        val (i, n) = argument
        val halfN = n / 2.0
        return sampleOf(1.0 - abs((i - halfN) / halfN))
    }
}

440.sine()
        .window(401)
        .windowFunction(TriangularFn())
```

Or there is a few predefined window functions:

* [rectangular](https://en.wikipedia.org/wiki/Window_function#Rectangular_window)
* [triangular](https://en.wikipedia.org/wiki/Window_function#Triangular_window)
* [blackman](https://en.wikipedia.org/wiki/Window_function#Blackman_window)
* [hamming](https://en.wikipedia.org/wiki/Window_function#Hann_and_Hamming_windows)

Any of them is called similarly to `.windowFunction()`:

```kotlin
440.sine()
        .window(401)
        .rectangular()

// or 
440.sine()
        .window(401)
        .triangular()

// or 
440.sine()
        .window(401)
        .blackman()

// or 
440.sine()
        .window(401)
        .hamming()
```

## Stream of any type

While it is very convenient to work with `Sample`, any other type allows to do the same. Though the API is a little more cumbersome -- in addition to generation function you need to specify the multiplication function, both of each are regular [WaveBeans functions](../functions.md).

Here is an example of applying the window function over the stream of `Long`:

```kotlin
input { (i, _) -> i }     // the type of the stream here is BeanStream<Long>
    .window(5) { 0L }     // windowing transforms to the BeanStream<Window<Long>> 
    .windowFunction(      // we'll double each elements inside the window
        func = { 2 },
        multiplyFn = { (a, b) -> a * b }
    )
``` 

## Low Level API

Low Level API of the operation is the function implementation `io.wavebeans.lib.stream.window.MapWindowFn` which gets two parameters: generation function and multiplication function.

**Generation function**

The generation function is intended to generate the window based on the source window size and has the following arguments:
 * Input type is `Pair<Int, Int>`, the first is the current index of the value, the second is the overall number of samples in the window.
 * Output type is `T`, is the value of the window on the specified index.

**Multiply function**

The multiply function defines how tow multiply two values coming from the stream and the window:
 * The input type is `Pair<T, T>`, which is a pair of sample to multiply, the first is coming from the stream, the second is coming from the window function.
 * The output type if `T` which is the result of multiplication.

Example of functions (working with `Sample` type):

```kotlin
val windowFunction: Fn<Pair<Int, Int>, Sample> = Fn.wrap { (i, n) ->
    // triangular window function
    val halfN = n / 2.0
    sampleOf(1.0 - abs((i - halfN) / halfN))
}

val multiplyFn: Fn<Pair<Sample, Sample>, Sample> = Fn.wrap { (a, b) ->
    a * b
}
```

Thus the usage of them is as simple as calling it via [`.map()`](map-operation.md) operation: 

```kotlin
440.sine()
    .window(401)
    .map(MapWindowFn(windowFunction, multiplyFn))
```