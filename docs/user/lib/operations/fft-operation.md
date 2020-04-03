FFT operation
==========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Window Functions](#window-functions)
- [FFT Sample](#fft-sample)
- [Storing to CSV](#storing-to-csv)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Overview
--------

Within WaveBeans library you may do an FFT analysis on the stream. To start an FFT stream you need first to convert Sample stream (`BeanStream<Sample>`) to windowed sampled stream (`BeanStream<Window<Sample>>`) using [window operation](window-operation.md) and that sample array will be an input of the FFT stream. The size of the FFT you'll define when creating the FFT stream, it should be the power of 2 and larger then the size of the underlying window.

While performing the FFT analysis, the input window is split in the middle and both parts are swapped, also the sample array is aligned with zeros to desired length of power of two - applied zero padding. FFT is performed using [iterative method](https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm#Data_reordering,_bit_reversal,_and_in-place_algorithms).

To apply FFT on sample stream you need:
1. [Window it](window-operation.md);
2. apply FFT.

Let's get an example:

```kotlin
440.sine()
    .window(401)
    .fft(512)
```

That stream will calculate FFT based on 401 samples, but before FFT calculation it will be aligned with zero-padding to 512 samples. And each 401 samples of source stream will return one FFT sample, and while stream lasts it generates the stream of FFT samples -- `BeanStream<FftSample>`, in fact it generates forward [STFT](https://en.wikipedia.org/wiki/Short-time_Fourier_transform).

Window Functions
--------

It is usual to apply the window function over the input on FFT calculation. In order to do this you just need to add an extra call in the chain to the function you want to apply:

```kotlin
// use one of the predefined functions
440.sine()
        .window(401)
        .hamming()
        .fft(512)

// or define your own
440.sine()
        .window(401)
        .windowFunction { (i, n) ->
            val halfN = n / 2.0
            sampleOf(1.0 - abs((i - halfN) / halfN))
        }
        .fft(512)

```

For more details follow [mapping with window function documentation](map-window-function.md)

FFT Sample
--------

`io.wavebeans.lib.stream.fft.FftSample` is a complex object that provides access to needed FFT calculations. Out of it you can get:
1. `time` -- time marker of the sample;
2. `binCount` -- number of bins of this FFT calculation, i.e. 512, 1024;
3. `sampleRate` -- sample rate which was used to calculate the FFT;
4. `fft` -- the list of complex numbers which is calculated FFT. 

To extract magnitude and phase use respective methods: `magnitude()` and `phase()`. Both methods return double values only from positive half, as FFT calculation is symmetric. Magnitude is returned in logarithmic scale. Also you may calculate the exact frequencies for bins by calling `frequency()` method. 

The list of `FftSample` methods:
1. `magnitude()` -- a sequence of magnitude values for that FFT sample.
2. `phase()` -- a sequence of phase values for that FFT sample. 
3. `frequency()` -- a sequence of frequencies for that FFT sample.  
4. `bin(frequency: Double)` -- an index of bin for specified frequency.

In the stream you may run further analysis, for example using [`map()` operation](map-operation.md) to extract some values, i.e. in the example below, we're getting the magnitude value for all FFT samples at around frequency 440Hz, where it should have the maximum value.

```kotlin
440.sine()
    .trim(100)
    .window(401)
    .fft(512)
    .map { it.magnitude().drop(it.bin(440.0)).first() }
``` 

Storing to CSV
---------

Stream of FFT samples support [output to CSV](../outputs/csv-outputs.md).