Finite converters
=========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

All WaveBeans streams supposed to be infinite during processing, however certain inputs like [wav-files](wav-file.md) are finite by their nature and that conflict needs to be resolved. For that particular purpose there is an abstraction that allows you to convert such finite stream into infinite one based on defined strategy. For example, you may replace all samples with zeros which are out of range of the source stream -- basically replace with silence, so it won't affect any other stream while you're mixing the up together for instance.

The conversion is recommended but not required, as many operations are designed to match the lengths of the finite streams and assume Zero samples automatically or ask to provide one.

At the moment, only one converter is supported out of the box which fills with provided value everything what is out of the range of the source stream, it is called `AfterFilling`. To convert finite stream to infinite one, you can call method `stream(converter)` on the `FiniteStream<T>` instance providing the converted. For example to convert `FiniteStream<Sample>` to infinite one `BeanStream<Sample>` with filling of zero samples at the end:

```kotlin
someFiniteStream.stream(AfterFilling(ZeroSample))
```

Also some of the API way call this method implicitly, for example, you can read wav-file and convert it to a infinite stream at once:

```kotlin
wave("file:///path/to/file.wav", AfterFilling(ZeroSample))
```

*Note: That functionality will be reworked in the near future to provide more transparent and flexible API.*