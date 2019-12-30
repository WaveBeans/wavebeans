Sine input
=========

WaveBeans supports generating of sinusoid of specific frequency as well as sweep sinusoid which changes it frequency from one to another within specific time range. These inputs as they are just a mathematical function support any sampling rate, and resampling happens internally.

Sine
------

To generate sinusoid of specific frequency you can use the following syntax:

* generate sinusoid if the the frequency can be represented as integer number: 

```kotlin
440.sine()
```

* or there is another way to define it using any number as a source of frequency value:

```kotlin
440.12345.sine() // the frequency of the sinusoid will be 440.12345Hz

1e-6.sine() // the frequency of the sinusoid will be 10^-6

1e6.sine() // the frequency of the sinusoid will be 10^6
```

**Amplitude**

By default, the sine is generated with amplitude `1.0` which can be changed as a parameter:

```kotlin
440.sine(amplitude = 0.5) // you can use named parameters

440.sine(0.5) // or you can specify it like this, as amplitude is the first argument if the sine function
```

**Offset**

Also you may define the time offset of the sine:

```kotlin
440.sine(timeOffset = 0.5) // you may use named parameter

440.sine(1.0, 0.5) // or you can specify it like this, as amplitude is the first argument 
                   // and time offset is the second you would need to specify them both.
```

**Lower level API**

That API is just a wrapper around class implementation `io.wavebeans.lib.io.SineGeneratedInput` which you may instantiate as well having the same result. You would need just to pass some of the parameters as an object of type `io.wavebeans.lib.io.SineGeneratedInputParams`:

```kotlin
SineGeneratedInput(SineGeneratedInputParams( // specifying everything as named parameters
        frequency = 440.0,
        amplitude = 1.0,
        timeOffset = 0.0,
        time = null
))

SineGeneratedInput(SineGeneratedInputParams(440.0, 1.0)) // providing only required parameters -- frequency and amplitude

```

Sine sweep
------

To generate sine sweep, which is sine that changes its frequency from one value to another within time period, you may use the following API:

```kotlin
(440..880).sineSweep(1.0, 2.0) // specifying required parameters amplitude and time to get from start to end frequency

(440..880).sineSweep(amplitude = 1.0, time = 2.0) // or the same but specifying as named parameters
```

**Offset**

You can also tweak similar to regular sine the time offset (in seconds) to change the phase of the sine, by default it is `0`:

```kotlin
(440..880).sineSweep(1.0, 2.0, timeOffset = 1.0) 
```

**Sweep delta**

And also you may change the frequency value it'll be changed by evenly. Note: make sure sample rate allows this, it shouldn't be less than (1 / sample rate), by default it is `0.1`:

```kotlin
(440..880).sineSweep(1.0, 2.0, sweepDelta = 0.01) 
```

**Lower level API**

The same as sin API it is just a wrapper around proper class instantiation, so you can always do this with the same effect:

```kotlin
SineSweepGeneratedInput(SineSweepGeneratedInputParams(
        startFrequency = 440.0,
        endFrequency= 880.0,
        amplitude = 1.0,
        time = 2.0,
        timeOffset = 0.0,
        sweepDelta = 0.1
))
```