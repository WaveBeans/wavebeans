WAV-file output
=========

The [WAV](https://en.wikipedia.org/wiki/WAV) is very popular format to store uncompressed audio as file. Currently WaveBeans supports only files with single channel -- mono. The sampling rate and bit depth can be any. It can be used only to store stream of samples -- `BeanStream<Sample>`.

Writing to wave file is 2 step process:
1. While the stream is being processed, the sample is stored as a temporary file. That means you could run the stream as long as you need and you don't need to define the length of the output beforehand.
2. The final wave file is formed when you're attempting to close the output. It is very important to remember this -- you always need to close the output before trying to find the file in the file system.

To store the stream into a wav file you would need to call one of the following function, each function defines specific parameters of the container.
1. Mono 8 bit -- `toMono8bitWav("file:///path/to/file.wav")` 
2. Mono 16 bit -- `toMono16bitWav("file:///path/to/file.wav")` 
3. Mono 24 bit -- `toMono24bitWav("file:///path/to/file.wav")` 
4. Mono 32 bit -- `toMono32bitWav("file:///path/to/file.wav")`

```kotlin
440.sine()
    .trim(1000)
    .toMono16bitWav("file:///path/to/file.wav")
```

*Note: Don't forget to follow general rules to [execute the stream](../../exe/readme.md)*

**Low-level API**

As any other API within WaveBeans framework, WAV output is just a wrapper around a class. You may create the instance of this class by specifying the stream it needs to read from and a set of parameters.

The parameters are create via instantiating class `io.wavebeans.lib.io.WavFileOutputParams`:
* `uri` -- the location of the file to write to. Should be valid URI, for file in local file system use scheme `file://` and then absolute path.
* `bitDepth` -- how many bites per sample to use when storing into a file. The type is `io.wavebeans.lib.BitDepth`. Supported bit rates are 8, 16, 24, 32, and kinda 64 (can store it but AFAIK it is not officially supported and hence no one understands this format).
* `numberOfChannels` -- The number of channels to store, should be greater or equal to 1. Currently, stream can work only with one channel, so the value different to 1 (Mono) will produce unexpected results.

The input stream should be the type of `Sample` -- `BeanStream<Sample>`.

So for example to store 440Hz sine you would need to write the code like:

```kotlin
val stream = 440.sine()
        .trim(1000)

WavFileOutput(stream, WavFileOutputParams(
    uri = "file:///path/to/file.wav", 
    bitDepth = BitDepth.BIT_32,
    numberOfChannels = 1
))
```
