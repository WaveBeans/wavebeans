Merge operation
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Handling streams of different lengths](#handling-streams-of-different-lengths)
- [Using with two different input types](#using-with-two-different-input-types)
- [Using as a class](#using-as-a-class)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Overview
--------

One of the most important base functions of WaveBeans is merge function, it allows you to make one out of two streams using an operation like sum, subtract or your own. Though, it works only with stream of the same type, but you can always [remap](map-operation.md) one of the streams for types to match. Despite the fact that merge stream works with the same stream types as inputs, it may return a different stream type as output.

The merge function is implemented as a [regular function](../functions.md) with the input types `(Pair<T1?, T2?>)` and output type `R`, so the signature of merge function looks like this:

```kotlin
val fn: (Pair<T1?, T2?>) -> R = { it.first + it.second }
//or
val fn: (Pair<T1?, T2?>) -> R = { (a, b) -> a + b}
```

The input type is a pair of nullable types `T1` and `T2`, where either `T1` or `T2` might be a `Sample`, `FftSample`, `Int` or something else, and both type can be different. The `Pair` is a tuple of two elements, which can be conveniently destructured via putting two variables into parentheses `(a,b)` as in the example above. The output type is non-nullable and can be any.

To apply merge operation on the stream just call `merge()` method and specify the second stream of the same type and merge function:

```kotlin
440.sine()
    .merge(880.sine()) { (a, b) -> a + b}
```

Here the argument of the function is destructured in place of regular argument function. In this case, both streams are sample streams `BeanStream<Sample>`, nullable type `Sample?` defines the sum operation, so you won't need to think about it. This is using merge operation with **lambda function**.

The very same can be achieved with just calling sum operation directly on the stream, as sum operation for sample streams is defined in the library:

```kotlin
440.sine() + 880.sine() 
```

Such shortcut operations are defined for streams of samples and windowed samples, as well as other [arithmetic operations](arithmetic-operations.md).

Handling streams of different lengths
-----------

Not all streams are the same length, some of them are infinite, some of them are finite. Handling of that situation properly is up to developer. For that purpose the operands of the merge function are nullable. When one of the operand is null that means that the stream the operand is coming from is over. Though the another stream is not over yet. It's up to you to resolve that, but if you're sure that both streams will never finish or they exactly the same length, you may convert it non-nullable types by simply using Kotlin function `requireNotNull()` which will throw an exception if the operand is not null, but at the same time allows you to treat it as non-nullable variable further.

```kotlin
infiniteStream1.merge(infiniteStream2) { (a, b) ->
    requireNotNull(a)
    requireNotNull(b)
    a * b // no need for extra null-checks, as multiplication may not defined for nullable types  
}
``` 

Using with two different input types
-----------

As was mentioned the merge operation may have two arguments if the types which are different. In the following example two streams are merged together which results in the third type. Schematically it may look like: `BeanStream<Int> + BeanStream<Float> -> BeanStream<Long>`.

```kotlin
input { (idx, _) -> idx.toInt() } // -> BeanStream<Int>
        .merge(
            input { (idx, _) -> idx.toFloat() } // -> BeanStream<Float>
        ) { (a, b) ->
            requireNotNull(a)
            requireNotNull(b)
            a.toLong() + b.toLong()
        } // -> BeanStream<Long>
```

Using as a class
----------

When the function needs some arguments to be bypassed outside, or you just want to avoid defining the function in inline-style as the code of the function is too complex, you may define the merge function as a class. First of all please follow [functions documentation](../functions.md).
 
As mentioned above the signature of the merge function is input type `Pair<T1?,T2?>` and the output type is `R`. Let's create an operation that sums two streams but keeps the value not more than specified value.

The class operation looks like this:

```kotlin
class SumSamplesSafeFn(initParameters: FnInitParameters) : Fn<Pair<Sample?, Sample?>, Sample>(initParameters) {

    constructor(maxValue: Sample) : this(FnInitParameters().add("maxValue", abs(maxValue.asDouble())))

    override fun apply(argument: Pair<Sample?, Sample?>): Sample {
        val maxValue = sampleOf(initParams.double("maxValue"))
        val (a, b) = argument
        val sum = a + b
        return when {
            sum > maxValue -> maxValue
            sum < -maxValue -> -maxValue
            else -> sum
        }
    }
}
```

And this is how it's called:

```kotlin
440.sine()
        .merge(880.sine(), SumSamplesSafeFn(sampleOf(1.0)))
```

This class uses helper function `sampleOf()` which converts any numeric type to internal representation of sample, please read more about in [types section](../readme.md#types)

*Note: when trying to run that examples do not forget to [trim](trim-operation.md) the stream and define the output.*