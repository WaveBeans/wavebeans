# Flatten operation

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Using for the SampleVector](#using-for-the-samplevector)
- [The Window is quite different](#the-window-is-quite-different)
- [Non-iterable types](#non-iterable-types)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

Flatten operation allows you to convert the stream of iterable elements, like `BeanStream<List<T>>`, to a stream of the elements themselves `BeanStream<T>`. It achieves with iterating over the elements in the container and once the container is empty the next one is being read from the stream.

For example, let's consider the stream of list of integers, and flatten it:

```text
{ [1, 2, 3], [4, 5, 6], [7, 8, 9], ... } ==(flatten)==> { 1, 2, 3, 4, 5, 6, 7, 8, 9, ... }
```

To perform a flatten operation just call the `.flatten()` method on the stream. The operation is very useful after the initial stream was windowed, perhaps processed in such batches and the output should be generated as a regualr non-batch stream. The `flatten` method is available for any stream which elements implement the `Iterable<T>` interface.

```kotlin
440.sine() // BeanStream<Sample>
    .window(32) // BeanStream<Window<Sample>>
    .map { window ->
        // do something within a batch 
        val a = window.elements.map { it.asDouble() }.average()
        (0 until window.size).map { sampleOf(a) }
    } // BeanStream<List<Sample>>
    .flatten() // BeanStream<Sample>
```

Flatten operation supports the lists or collections of any sizes, though if it there is an iterable of infinite size it'll never get to the next element as well as the stream will never end by itself. Also, if it hits on the empty list it'll be simply skipped.

```text
{ [1, 2], [3], [], [4, 5], [6, 7, 8, 9], [], ... } ==(flatten)==> { 1, 2, 3, 4, 5, 6, 7, 8, 9, ... }
```

Flattening is performed only on one level, so if the stream is a list of lists or similar, you would need to perform the flattening for each level explicitly:

```text
{ [[1, 2, 3], [4, 5, 6]], [[7, 8, 9]], ... }
    ==(flatten)==> { [1, 2, 3], [4, 5, 6], [7, 8, 9], ... }
    ==(flatten)==> { 1, 2, 3, 4, 5, 6, 7, 8, 9, ... }
```

## Using for the SampleVector

[SampleVector](../readme.md#samplevector), which is technically is just an array of samples, can be flattened as well. Though some operations (i.e. [table](../outputs/table-output.md#sample-type) or [wav](../outputs/wav-output.md#performance-boost) outputs) support working with it as with usual sample keeping it slightly more optimized than a singular sample, that's why it better to use such operations instead of flattening.

If that is not an option, just call `flatten()` method on the stream:

```kotlin
input { (i, _) -> sampleVectorOf(
         sampleOf(sin(i)), 
         sampleOf(cos(i)), 
         sampleOf(sin(i) * cos(i))
    ) } // BeanStream<SampleVector>
    .flatten() // BeanStream<Sample>
```

## The Window is quite different

[Window](window-operation.md) is also some sort of container if iteraable elements, and there is an API to support flattening out of the box using the very same `flatten()` method. 

```kotlin
440.sine() // BeanStream<Sample>
    .window(32) // BeanStream<Window<Sample>>
    .flatten()  // BeanStream<Sample>
```

Though there is one attribute of window which makes it stand out, and this is a `step`. Step can make windows overlap between each other, and while flattening you need to resolve that. For that purpose you may specify the `overlapResolve` [function](../functions.md) that gets the pair of elements to somehow get a overlapped one, you

```kotlin
val input = input { (i, _) -> i} // BeanStream<Long>
            .window(64, 32) // BeanStream<Window<Long>>

// resolve as a sum of overlapping elements
input.flatten { (a, b) -> a + b } // BeanStream<Long>

// resolve as an average of overlapping elements
input.flatten { (a, b) -> (a + b) / 2L } // BeanStream<Long>

// resolve by taking only the first element
input.flatten { (a, _) -> a } // BeanStream<Long>
```

The need for the function is checked only in runtime, so if you're sure it won't be called you may not specify it. As a general rule specify it if `step < size`, if `step == size || step > size` you may omit it.

As a remark, if `step > size` you'll see the zero elements of the window in the flattened stream.

## Non-iterable types

You may call the `flatten()` method on each stream which element extends the `Iterable<T>` interface, but sometimes it is convenient to extract the iterable out of another type, at the same time the [`map()`](map-operation.md) seems superfluous. For that purpose you may use the `flatMap()` method specifying the [function](../functions.md) to provide mapping to iterable entity. It'll do the mapping and flatten operation at the same time:

```kotlin
input { (i, _) -> Pair(i, i * 2)} // BeanStream<Pair<Long>>
    .flatMap { listOf(it.first, it.second).map { sampleOf(it) }  } // BeanStream<Sample>

// or similar by functionality but not by execution details
input { (i, _) -> Pair(i, i * 2)} // BeanStream<Pair<Long>>
    .map { listOf(it.first, it.second).map { sampleOf(it) }  } // BeanStream<List<Sample>>
    .flatten() // BeanStream<Sample>
```

The main difference between using `map+flatten` and `flatMap`, that in distributed/multi-threaded execution the `map+flatten` is treated as two separate operations, which requires making sure the object between map and flatten is serializable, and in some case will be actually transferred over the network. Also, at the moment, the `flatten` is non-parallelized operation, but `map` is. That all may have some performance impact.
