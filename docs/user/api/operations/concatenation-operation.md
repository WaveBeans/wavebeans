Concatenation operation
=====

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Whenever you need to concatenate stream one after another you may use this operator. The leading stream though should be finite and be the type of `FiniteStream<T>`, another stream operand should be finite or infinite but the same type. Depending on the second argument type, the resulting stream will be either finite of type `FiniteStream<T>`, or infinite of type `BeanStream<T>`. 

This is what is happening schematically, the stream are being read one after another:

```text
[1, 2, 3, 4] .. [10, 12, 13] -> [1, 2, 3, 4, 10, 12, 13] 
```

The inline operator `..` concatenate two streams, you may use them one-by-one as many as you like to concatenate more stream altogether:

```
// finite streams
val a = listOf(1, 2).input()
val b = listOf(3, 4, 5).input()
val c = listOf(10, 20, 30).input()
// infinite stream
val d = listOf(100, 200).input().stream(AfterFilling(0))

(a..b).asSequence(44100.0f).toList()
// results in stream [1, 2, 3, 4, 5]

(b..c).asSequence(44100.0f).toList()
// results in stream [3, 4, 5, 10, 20, 30]

(a..b..c).asSequence(44100.0f).toList()
// results in stream [1, 2, 3, 4, 5, 10, 20, 30]

(a..b..c..d).asSequence(44100.0f).take(15).toList()
// results in stream [1, 2, 3, 4, 5, 10, 20, 30, 100, 200, 0, 0, 0, 0, 0]

(d..c).asSequence(44100.0f).take(15).toList()
// won't compile as the first operand `d` is infinite stream 
```

If you want to do a finite stream from the infinite one, you may use [trim operation](trim-operation.md):

```kotlin
val sine1 = 440.sine().trim(3)
val sine2 = listOf(0.1, 0.2, 0.3).map { sampleOf(it) }.input()

(sine1..sine2).asSequence(1000.0f).toList()
// results in stream [1.0, -0.9297764858882515, 0.7289686274214119, 0.1, 0.2, 0.3]
```