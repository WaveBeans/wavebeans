WAV File input
=======

The [WAV](https://en.wikipedia.org/wiki/WAV) is very popular format to store uncompressed audio as file. Currently WaveBeans supports only files with single channel -- mono. The sampling rate and bit depth can be any.

Worth to mention, wav-input doesn't support resampling internally, so it should correspond with stream sample rate or be resampled.

Also as any stream in WaveBeans considered to be infinite and wav-file is not, to apply full API capabilities you would need to convert it using [finite-converters](finite-converters.md)

Syntax
----

To read the file it is as easy as call the function `wave`, currently only full URLs are supported, so in order to specify file in the local file system you would need to specify protocol `file://` and then absolute path for the file. Please be aware that the name and path of the file is OS dependent and might be even case-sensitive.

```kotlin
wave("file:///path/to/file.wav") // for unix-like systems

wave("file://c:\\path\\to\\file.wav") // for windows systems
```

Using that API we can convert the file to infinite stream by defining the strategy for reading data when it's got rolled out, in this case we'll just fill the stream with zeros when the main stream is over:

```kotlin
wave("file:///path/to/file.wav", ZeroFilling())
```

**Low-level API**

As any other stream in the system, API mentioned above is just a wrapper around classes which handle all the logic. To create an instance with the same effect you would need to to this:

```kotlin
import java.net.URI

WavFiniteInput(WavFiniteInputParams(
    uri = URI("file:///path/to/file.wav")
))
```

That input implements `io.wavebeans.lib.io.FiniteInput` interface, so it should be converted for most use cases using [finite-converters](finite-converters.md).
