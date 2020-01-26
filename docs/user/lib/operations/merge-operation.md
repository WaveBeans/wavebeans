Merge operation
========

One of the most important base functions of WaveBeans is merge function, it allows you to make one out of two streams using an operation like sum, subtract or your own. Though, it works only with stream of the same type, but you can always [remap](map-operation.md) one of the streams for types to match. Despite the fact that merge stream works with the same stream types as inputs, it may return a different stream type as output.

The merge function is implemented as a [regular function](../functions.md) with the input type `(Pair<T?, T?>)` and output type `R`, so the signature of merge function looks like this:

```kotlin
val fn: (Pair<T?, T?>) -> R = { a -> a.first + a.second }
//or
val fn: (Pair<T?, T?>) -> R = { (a, b) -> a + b}
```

The input type is a pair of nullable types `T`, where `T` might be a `Sample`, `FftSample`, `Int` or something else. The `Pair` is a tuple of two elements, which can be conveniently destructured via putting two variables into parentheses `(a,b)` as in the example above. The output type is non-nullable and can be any.

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

**Using as a class**

When the function needs some arguments to be bypassed outside, or you just want to avoid defining the function in inline-style as the code of the function is too complex, you may define the merge function as a class. First of all please follow [functions documentation](../functions.md).
 
As mentioned above the signature of the merge function is input type `Pair<T?,T?>` and the output type is `R`. Let's create an operation that sums two streams but keeps the value not more than specified value.

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

*Note: when trying to run that examples do not forget to [trim](trim-operation.md) the stream and define the output.*