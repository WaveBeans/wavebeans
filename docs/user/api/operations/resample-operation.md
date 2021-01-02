# Resample operation

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Match the output sample rate](#match-the-output-sample-rate)
- [Resample for processing in-between](#resample-for-processing-in-between)
- [Resampling algorithm](#resampling-algorithm)
- [Important notes](#important-notes)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

Resample allows you to change the sample rate of the stream, for the cases like, there are several wav-inputs with different sample rate you'd like to mix, or you higher or lower sample rate required for the processing, but resulting stream should be stored with certain sample rate.

To cover all these cases there is a single `.resample()` method. It is available for any type of the stream, though for some cases you would need to specify `reduce` function, which creates one sample out of many, on your own.

## Match the output sample rate

By default, it'll match the sample rate it has up-stream to what it has down-stream, i.e. let's assume the wav-file is stored with sample rate 22050 Hz, but the output will be processed with 44100 Hz. To match them just put a resample call between the input and output definitions:

```kotlin
wave("file:///sound-22050.wav")
    .resample()
    .toMono16BitWav("file:///sound-44100.wav") // and process with 44.1 KHz sample rate
```

## Resample for processing in-between

If the stream should be processed with lower (higher) sample rate, you may specify `to` and specify the sample rate explicitly. Afterwards, it's probably a good idea to resample it without argument to match the sampling rate of the output. If `to` is `null` then it is considered as default behavior.

```kotlin
wave("file:///sound-22050.wav")
    .resample(to = 11025.0f) // starting here the sample rate is lower
    .map { it * 2 }
    .resample() // make sure it'll be resample with whatever it will be processed with.
    .toMono16BitWav("file:///sound-44100.wav") // and process with 44.1 KHz sample rate
```

## Resampling algorithm

There are built-in implementations for resampling functions that you may use, see the explanation further down. 

Also, you may specify a [function](../functions.md) as a `resampleFn` argument to implement your own resampling algorithm. That function takes an argument of type `io.wavebeans.lib.stream.ResamplingArgument<T>` and expects to return the kotlin sequence of type `Sequence<T>`. The sequence is expected to be resampled and then it can be matched with the streams of the same sample rate, regardless if that not might be 100% true. It is not expected that the length of resulted stream is divisable by the resampling factor, it just assumed matched.

The `ResamplingArgument` has following fields:
* `inputSampleRate` (Float) -- the sample rate the operation should treat the incoming data with.
* `outputSampleRate` (Float) -- the sample rate the operation is expected to return the data with.
* `inputOutputFactor` (Float) -- the difference factor between the input and output sample rates.
* `inputSequence` (Sequence<T>) -- the data sequence of the input to read from.

### A simple algorithm for integer resampling factor

By default, for the any type different from [Sample](../readme.md#sample) pretty simple algorithm is used. Simple resample function `SimpleResampleFn` upsamples via duplicating samples, and downsamples by windowing and then reducing down with `reduceFn` down to one sample of the type `T`. This method supports only integer resampling factor (direct or reversed -- upsampling or downsampling accordingly).  Reduce function `reduceFn` is called only during downsamping and should convert the List<[T]> to the singular value [T].
 
Downsampling colls reduce function on each group of `1.0 / [ResamplingArgument.inputOutputScaleFactor]` elements:

```text
[ 1 1 2 2 3 3 4 4 5 5 ] --(x0.5)--> reduceFn(::average) --> [ 1 2 3 4 5 ]
```

Upsampling duplicates elements for [ResamplingArgument.inputOutputScaleFactor] times:

```text
[ 1 2 3 4 5 ] --(x2)--> [ 1 1 2 2 3 3 4 4 5 5 ]
```

It requires to specify the reduce function when downsampled, that converts the list of samples into a singular sample. It is defined for [`Sample`](../readme.md#sample) as an average of all values, but you would need to specify [the function](../functions.md) explicitly for any other type, otherwise it'll fail in runtime. Assuming you'll use the built-in resample functions:

```kotlin
wave("file:///sound-22050.wav")
    .resample(resampleFn = SimpleResampleFn { it.last() }) // taking last element of the list instead of average of all values.
    .toMono16BitWav("file:///sound-44100.wav") // and process with 44.1 KHz sample rate
```

The [function](../functions.md) takes `List<T>` as an argument and expects to return `T`, where `T` is the non-nullable type of sample.

### A sinc interpolation

The sinc interpolation algorithm is based on ideas of [Whittaker–Shannon interpolation formula](https://en.wikipedia.org/wiki/Whittaker%E2%80%93Shannon_interpolation_formula). For samples there is a built in implementation which is used by default, it can be created with a function call `io.wavebeans.lib.stream.SincResampleFnKt.sincResampleFunc`. It has the only parameter `windowSize` that is used to calculate the certain sample sinc values, the higher value the better results, but it also means the signal will be delayed by that amount of samples, as well as more CPU cycles will be used for the specific sample, as usual it is a trade off between the quality, performance and signal delay.

For other type you would need to implement a few simple functions, and provide `windowSize`. On high level the functions are called this way:

```kotlin
val y /** represents the output interface */
fun h() /** represents the filter function, that calculates the needed values of sinc */

// read the initial vector
var x = createVectorFn(windowSize, inputSequenceIterator)
// calculate the first sample and return it
y += applyFn(x, h())

// while the vector do not represent end of signal repeat
while (isNotEmptyFn(x)) {
  // calculate new offset
  val offset /** the delta is calculated here based on input-output scale factor and current stream position */
  // if offset got changed we need to extract next vector
  if (offset > 0)
    x = extractNextVectorFn(windowSize, offset, x, inputSequenceIterator)
  // calculate the sample based on current vector and return it
  y += applyFn(x, h())
}
```

The functions are:

* `createVectorFn` - function of two parameters that create a container of type [L] of desired size (1) out of iterator with elements of type [T] (2). The function called only once when the initial window is being read from the input sequence.
* `extractNextVectorFn` - function of one argument of type [ExtractNextVectorFnArgument] to extract next container of type [L] out of provided window. The function is called every time the [ExtractNextVectorFnArgument.offset] is changed.
* `isNotEmptyFn` - checks if the container is not empty. The current container is provided via the argument. Returns `true` if the container is not empty which lead to continue processing the stream, otherwise if `false` the stream will end.
* `applyFn` - function convolve the filter `h` which is a sum of corresponding `sinc` functions values in time markers of each sample of the window. Expected to return the sum of elements of vector of type [L] as singular element of type [T], i.e. if `x` is a vector, `h` is a filter, and `*` is convolution operation, the result expected to be: `(h * x).sum()`

For example this is how it is implemented for [`Sample`](../readme.md#sample) type:

```kotlin
SincResampleFn(
            windowSize = windowSize,
            createVectorFn = { (size, iterator) ->
                sampleVectorOf(size) { _, _ ->
                    if (iterator.hasNext()) iterator.next() else ZeroSample
                }
            },
            extractNextVectorFn = { a ->
                val (size, offset, window, iterator) = a
                sampleVectorOf(size) { i, n ->
                    if (i < n - offset) {
                        window[i + offset]
                    } else {
                        if (iterator.hasNext()) iterator.next()
                        else ZeroSample
                    }
                }

            },
            isNotEmptyFn = { vector -> vector.all { it != ZeroSample } },
            applyFn = { (x, h) -> (h * x).sum() }
    )
```
 
## Important notes

It's worth to remember that all streams you run operations on should have the same sample rate. If that is not true, during the initialization of the stream you'll get the exception with message like `The stream should be resampled from 8000.0Hz to 16000.0Hz before writing`. Also, when streams are being merged the output sample rate should match both inputs sample rate.

Some inputs, like generation of sine, are adopting automatically to the provided sample rate. At this point, following inputs do not require the resampling:

* [Function as input](../inputs/function-as-input.md) unless you specify the sample rate explicitly via the parameter.
* [Sines](../inputs/sines.md).
* [List as input](../inputs/list-as-input.md).

Currently, the resampling is not partitioned during execution, so it's a good idea to apply them right after the inputs or before the outputs to avoid unnecessary split-merging process while executed in multi-threaded or distributed mode. 
