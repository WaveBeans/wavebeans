Change amplitude operation
========

To change amplitude for each sample on the stream you may use this simple operation. As internal representation of sample allows you to have amplitude more than absolute value of one, you can multiply or divide safely, however don't forget to normalize the stream before the output using certain sinks. You may this operation on any stream with type `Sample` -- `BeanStream<Sample>`.

The multiplier (or divisor) is double scalar value following the formula: `sample[x] * a` or `sample[x] / b`, the operation is not commutative in this case. API allow to use any numeric value which will automatically be converted into double representation. 

To change amplitude on the stream you may use one of the following methods:

```kotlin
val stream = 440.sine() // initial stream is defined here, let's use simple sine.

val multipliedByTwoStream = stream * 2 // you may ultiply by any scalar numeric value, i.e. integer
val multipliedByOPointOneStream = stream * 0.1 // or here is double
val dividedByTenStream = stream / 10 // division works similar
val aFewOperationsStream = stream * 0.1 / 10 * 100.0 // you may specify a few operations sequentially

val newStream = stream.changeAmplitude(2.0) // or use function on the stream
```

If you want to do something more complex you may consider using [map function](map-operation.md).
