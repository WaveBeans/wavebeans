Trim operation
=======

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Most of the streams within WaveBeans are infinite, but at some point you may want to get some limited output to the file or something. For that purpose you need to convert the infinite stream to a finite one. This is what the trim operation is aimed for.

The `trim` operation has the `length` as a required parameter and `timeUnit` as an optional parameter which is by default is `TimeUnit.MILLISECONDS`:

```kotlin
// limit the stream to get 500ms out of it, milliseconds is provided by default.
stream.trim(500)
// limit the stream to get 10 seconds out of it, seconds are to be provided explicitly.
stream.trim(10, TimeUnit.SECONDS)
```
