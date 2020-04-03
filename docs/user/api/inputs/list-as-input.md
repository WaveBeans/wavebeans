List as Input
========
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Whenever you want to generate the input based on some predefined value such as list of values, you may that approach and convert any `List<T>` into a stream with corresponding type T. The main difference between List as Input and [input as function](function-as-input.md) is that all values are available at the time the stream starts, but not being evaluated.

You can convert any `List<T>` to `BeanStream<T>` by simply calling `input()` function of the list:

```kotlin
listOf(1, 2, 3).input()

// or something more complex

(0..44100).map { sin(44100.0 / it) }.input()
```

It's important to remember, that list is being evaluated before the stream itself and in case of distributed executions the result should be propagated across all executors, so being lean about using such inputs is essential.