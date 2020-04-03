Dev-null output
=======

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

As you know in order the stream to be processed it requires to have output. Sometimes for any reason you're not interested in the results of the stream but won't the stream to pass through all beans anyway. For tht purpose you may use dev-null output which initiates the stream computation s any other output would but the result of the stream is discarded right away. It is called after approach in linux-like systems when the output has to be disregarded.

This output has no parameters and side effects.

```kotlin
440.sine()
        .trim(1)
        .toDevNull()
```

*Note: Don't forget to follow general rules to [execute the stream](../../exe/readme.md)*
