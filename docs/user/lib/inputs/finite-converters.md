Finite converters
=========

All WaveBeans streams supposed to be infinite during processing, however certain inputs like [wav-files](wav-file.md) are finite by their nature and that conflict needs to be resolved. For that particular purpose there is an abstraction that allows you to convert such finite stream into infinite one based on defined strategy. For example, you may replace all samples with zeros which are out of range of the source stream -- basically replace with silence, so it won't affect any other stream while you're mixing the up together for instance.

It is required for some of the cases and implemented only for type `Sample` currently. Also, only one converter is supported out of the box which fills with zeros everything what is out of the range of the source stream -- `ZeroFilling`. To convert finite stream to infinite one, you can call method `sampleStream()` on the FiniteSampleStream instance:

```kotlin
someFiniteStream.sampleStream(ZeroFilling())
```

Also some of the API way call this method implicitly, for example, you can read wav-file and convert it to a infinite stream at once:

```kotlin
wave("file:///path/to/file.wav", ZeroFilling())
```

*Note: That functionality will be reworked in the near future to provide more transparent and flexible API.*