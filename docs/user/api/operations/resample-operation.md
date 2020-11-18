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

By default, the pretty simple algorithm is used. [TODO provide description]. It requires to specify the reduce function when downsampled, that converts the list of samples into a singular sample. It is defined for [`Sample`](../readme.md#sample) as an average of all values, but you would need to specify [the function](../functions.md) explicitly for any other type, otherwise it'll fail in runtime. Assuming you'll use the built-in resample functions:

```kotlin
wave("file:///sound-22050.wav")
    .resample(resampleFn = SimpleResampleFn { it.last() }) // taking last element of the list instead of average of all values.
    .toMono16BitWav("file:///sound-44100.wav") // and process with 44.1 KHz sample rate
```

The [function](../functions.md) takes `List<T>` as an argument and expects to return `T`, where `T` is the non-nullable type of sample.

As you noticed, specifying different function as `resampleFn` argument you may implement your own resampling function. That function has an argument `io.wavebeans.lib.stream.ResamplingArgument<T>` and expects to return the kotlin sequence of type `Sequence<T>`. The sequence is expected to be resampled and then it can be matched with the streams of the same sample rate, regardless if that not might be 100% true. It is not expected that the length of resulted stream is divisable by the resampling factor.

The `ResamplingArgument` has following fields:
* `inputSampleRate` (Float) -- the sample rate the operation should treat the incoming data with.
* `outputSampleRate` (Float) -- the sample rate the operation is expected to return the data with.
* `inputOutputFactor` (Float) -- the difference factor between the input and output sample rates.
* `inputSequence` (Sequence<T>) -- the data sequence of the input to read from.

## Important notes

It's worth to remember that all streams you run operations on should have the same sample rate. If that is not true, during the initialization of the stream you'll get the exception with message like `The stream should be resampled from 8000.0Hz to 16000.0Hz before writing`. Also, when streams are being merged the output sample rate should match both inputs sample rate.

Some inputs, like generation of sine, are adopting automatically to the provided sample rate. At this point, following inputs do not require the resampling:

* [Function as input](../inputs/function-as-input.md) unless you specify the sample rate explicitly via the parameter.
* [Sines](../inputs/sines.md).
* [List as input](../inputs/list-as-input.md).

Currently, the resampling is not partitioned during execution, so it's a good idea to apply them right after the inputs or before the outputs to avoid unnecessary split-merging process while executed in multi-threaded or distributed mode. 
