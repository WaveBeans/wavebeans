# CSV file output

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Writing Samples](#writing-samples)
- [Writing FFT samples](#writing-fft-samples)
- [User defined CSV output](#user-defined-csv-output)
- [Controlling output](#controlling-output)
  - [Noop signal](#noop-signal)
  - [Flush signal](#flush-signal)
  - [Open and close gate signals](#open-and-close-gate-signals)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

One of the very valuable outputs for further analysis by third party tools, either visualization or converting low level data to something WaveBeans doesn't support out of the box, is CSV file. CSV file is simple comma-separated value file, which is easy to read or work with. 

WaveBeans support output to CSV of predefined type in specific columnar format or you may define your own function on predefined type or user defined type.

## Writing Samples

One of the predefined and most simple types is Sample. The CSV output of such type has two columns: time and value. The time value is integer value of the current time, the unit is defined as a parameter, precision up to nano-seconds. The value is double of current sample value (usually between `-1.0` and `1.0`, however is not limited here), the precision is 10^-10 (10 digits after dot).

To generate such output, on the stream of samples `BeanStream<Sample>` call `toCsv()` function, specifying:
* `uri` - URI to the file to store it. *required*
* `timeUnit` - the unit time to use to print first column. By default, it is milliseconds. The type is `java.util.concurrent.TimeUnit`, supported every time unit enumerated there. *optional*
* `encoding` - what encoding to use when interpret string value to bytes. By default, `UTF-8`. Most of the time you won't need to change it. *optional*

As an example, let's store one second of 440Hz sine into a file:

```kotlin
import java.util.concurrent.TimeUnit.NANOSECONDS

440.sine()
   .trim(1000)
   .toCsv(
        uri = "file:///path/to/file.csv",
        timeUnit = NANOSECONDS
   )
```

*Note: Don't forget to follow general rules to [execute the stream](../../exe/)*

The generated output will look like this (first few rows):

```csv
time ns,value
0,1.0000000000
22675,0.9980356644
45351,0.9921503750
68027,0.9823672529
90702,0.9687247330
113378,0.9512764123
136054,0.9300908393
158730,0.9052512453
181405,0.8768552168
204081,0.8450143126
226757,0.8098536250
249433,0.7715112889
272108,0.7301379387
294784,0.6858961165
317460,0.6389596342
... SKIPPED ...
```

**Low-level API**

Samples to CSV is just a convenient wrapper around [user defined CSV output](#user-defined-csv-output), in order to use that API you may use function `io.wavebeans.lib.io.SampleCsvFn`, which has `timeUnit` as a parameter.

## Writing FFT samples

Another type supported out-of-the-box is FFT sample. FFT sample is more complicated than just sample and can be represented only as a pivotal table, where the columns are frequencies and rows are values for each time markers. Each row is a single FFT calculation of the specified window. Also, FFT stream consist of two sub-streams -- magnitude and phase, you need to specify explicitly which one needs to be written.


In the example below, the FFT of size 128 is generated over 101 samples of the 440Hz sine input, and stored twice into different files -- one for magnitude, another for phase.
 
```kotlin
val fft = 440.sine()
        .trim(1000)
        .window(101)
        .fft(128)

fft.magnitudeToCsv(
        uri = "file:///path/to/file.magnitude.csv"
) // this is the first output

fft.phaseToCsv(
        uri = "file:///path/to/file.phase.csv"
) // this is the second output
```

*Note: Don't forget to follow general rules to [execute the stream](../../exe/readme.md), and bear in mind, in this example two separate outputs were created. *

Both method of writing magnitude or phase into CSV file support following parameters:
* `uri` - URI to the file to store it. *required*
* `timeUnit` -- the first column of the table is integer value of time markers, that flag allows you to specify which units to use to output this value. The type is `java.util.concurrent.TimeUnit`, supported every time unit enumerated there. Default value is milliseconds. *optional*
* `encoding` - what encoding to use when interpret string value to bytes. By default, `UTF-8`. Most of the time you won't need to change it. *optional*

**Low-level API**

As any other API within WaveBeans framework, CSV output of FFT sample is just a wrapper around a class. You may create the instance of this class by specifying the stream it needs to read from and a set of parameters.

The parameters are create via instantiating class `io.wavebeans.lib.io.CsvFftStreamOutputParams`:
* `uri` -- the location of the file to write to. Should be valid URI, for file in local file system use scheme `file://` and then absolute path.
* `timeUnit` -- the unit to use for time markers. The type is `java.util.concurrent.TimeUnit`, supported every time unit enumerated.
* `isMagnitude`. The boolean value of either the magnitude should be written (`true`) or phase (`false`).
* `encoding` - what encoding to use when interpret string value to bytes. By default, `UTF-8`. Most of the time you won't need to change it.

The input stream should be the type of `FftSample` -- `BeanStream<FftSample>`.

So for example to store magnitude of the FFT of 440Hz sine you would need to write the code like:

```kotlin
val fftStream = 440.sine()
        .trim(1000)
        .window(101)
        .fft(128)

CsvFftStreamOutput(fftStream, CsvFftStreamOutputParams(
        uri = "file:///path/to/file.magnitude.csv", 
        timeUnit = MILLISECONDS,
        isMagnitude = true
))
```

## User defined CSV output

If you want to customize CSV output for predefined type or store your own type you may define a function that will implement that custom logic. CSV consists of two main parts: header and body, both of them should be specified. 

The `header` is just a list of string that will be concatenated to comma-separated string, and that will be the first line. To write a body of a CSV file you would need to implement a function which serializes the sample or some other element to a list of string tokens, that will represent a row. This function is called `elementSerializer`.

The function has 3 parameters:
1. Index of the sample of `Long` type. This is numeric number of the sample, starting from 0 monotonically growing to "infinity".
2. Sample rate of the output. It corresponds to the one you're running the whole stream with. Can be used for example to calcualte the time based on the sample index, for that purpose you may use helper function `io.wavebeans.lib.SampleUtilsKt.samplesCountToLength(samplesCount: Long, sampleRate: Float, timeUnit: TimeUnit): Long` which converts number of samples to its time marker in specified time unit.
3. The actual value of the sample or any other element you're working with. 

There are two main approaches of defining a function for the output:
1. Lambda function for the cases where it is not dependent on outside parameters, and the only parameters it needs are function parameters
2. Class extending `Fn` with input type parameter `T=Triple<Long, Float, Sample>` and output type parameter `R=List<String>`.

For more information regarding defining function follow appropriate [functions section](../functions.md).

Let's, for example, create a function that stores sample stream which was windowed as 2 into different columns.

Using lambda it'll look like this:

```kotlin
import java.util.concurrent.TimeUnit.MILLISECONDS

440.sine()
        .trim(1)
        .window(2)
        .toCsv(
                uri = "file:///path/to/file.csv",
                header = listOf("time ms", "sample#1", "sample#2"),
                elementSerializer = { (idx, sampleRate, window) ->
                    listOf(
                            samplesCountToLength(idx, sampleRate, MILLISECONDS).toString(),
                            String.format("%.10f", window.elements.first()),
                            String.format("%.10f", window.elements.drop(1).first())
                    )
                }
        )
```

Let's image we want to bypass the time unit of the output as a parameter and modify example above to use a function as a class.

```kotlin
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

class CsvFn(parameters: FnInitParameters) : Fn<Triple<Long, Float, Window<Sample>>, List<String>>(parameters) {

    constructor(timeUnit: TimeUnit) : this(FnInitParameters().addObj("timeUnit", timeUnit) { it.name })

    override fun apply(argument: Triple<Long, Float, Window<Sample>>): List<String> {
        val (idx, sampleRate, window) = argument
        val tu = initParams.obj("timeUnit") { TimeUnit.valueOf(it) }
        return listOf(
                samplesCountToLength(idx, sampleRate, tu).toString(),
                String.format("%.10f", window.elements.first()),
                String.format("%.10f", window.elements.drop(1).first())
        )
    }
}

val timeUnit = MILLISECONDS

440.sine()
        .trim(1)
        .window(2)
        .toCsv(
                uri = "file:///path/to/file.csv",
                header = listOf("time ${timeUnit.abbreviation()}", "sample#1", "sample#2"),
                elementSerializer = CsvFn(timeUnit)
        )

```

When you run either one of approaches the output would look like this:

```csv
time ms,sample#1,sample#2
0,1.0000000000,0.9980356644
0,0.9921503750,0.9823672529
0,0.9687247330,0.9512764123
0,0.9300908393,0.9052512453
0,0.8768552168,0.8450143126
0,0.8098536250,0.7715112889
0,0.7301379387,0.6858961165
0,0.6389596342,0.5895128895
0,0.5377501426,0.4838747522
0,0.4280983770,0.3706401441
0,0.3117257880,0.2515867637
0,0.1904593378,0.1285836598
0,0.0662028189,0.0035618889
0,-0.0590930346,-0.1215158010
0,-0.1834611718,-0.2446857840
0,-0.3049491062,-0.3640143837
0,-0.4216495683,-0.4776282305
0,-0.5317304484,-0.5837436722
0,-0.6334635592,-0.6806947761
0,-0.7252517671,-0.7669594825
0,-0.8056540662,-0.8411835001
0,-0.8734082006,-0.9022015676
```

## Controlling output

The same stream parts can be stored into different files if that is needed. The example of such cases: you want to cut the signal into let's say equal-sized parts, or detect the silence and store samples into multiple files removing the silence on the way.

In order to do that you need to wrap the object with [Managed](../readme.md#managed-type) class and then whenever you feel it is the time -- send the flush signal. For convenience, you may use function `io.wavebeans.lib.io.AbstractWriterKt.withOutputSignal` on top of any non-nullable type, though sometimes compiler can't interfere the types, and you would need to specify them explicitly:

```kotlin
// from Sample type and no arguments for `NoopOutputSignal` signal
sample.withOutputSignal<Sample, Unit>(NoopOutputSignal)

// from Int type and new instance of custom type `ArgumentType`of argument for `FlushOutputSignal` signal 
int.withOutputSignal<Int, ArgumentType>(FlushOutputSignal, ArgumentType("some-value"))
```

To be able to output `Managed` stream into csv-file you need to call one of the csv output functions (see above) specifying the suffix function that translates the argument into a string, for example:

```kotlin
stream.toCsv(
      uri = "file:///output/dir/my.csv",
      header = listOf("time sec", "value"),
      elementSerializer = { (i, sampleRate, sample) ->
          listOf(String.format("%.3f", i / sampleRate), String.format("%.10f", sample))
      },
      suffix = { "-${it ?: 0}" }
)
```

* For `Sample` type you omit the element serializer, and use the regular one.
* The file name is augmented with suffix: `file:///path/to.csv` becomes `file:///path/to${suffix}.csv`, the suffix is generated by the provided function.
* The argument is provided at the moment when the signal is fired.

### Noop signal

As the stream becomes "managed" the signal must be specified, if nothing needs to be performed you still forced to specify something. `io.wavebeans.lib.io.AbstractWriterKt.NoopOutputSignal` is not handled by the output and completely ignored, use it every time you don't want to affect the stream. 

### Flush signal

The flush signal `io.wavebeans.lib.io.AbstractWriterKt.FlushOutputSignal` allows you to tell the output to immediately close and flush the current buffer and start a new one.  


### Open and close gate signals

The gate allows you to define if the output should be stored or ignored. When the gate is opened, all samples which are coming in are stored in the buffer, when the gate is closed the current buffer (if it's not empty) flushed on the disk. The following coming in samples are ignored unless the next open gate signal is emitted. When the output is created, the gate is already opened.

To open the gate send `io.wavebeans.lib.io.AbstractWriterKt.OpenGateOutputSignal`, to close `io.wavebeans.lib.io.AbstractWriterKt.CloseGateOutputSignal`. The same consequent signals has no effect. I.e. if the gate is already opened, the open gate signal will remain the gate opened, and the signal is completely omitted, even monitoring metrics are not affected. The similar is true for close gate signal.