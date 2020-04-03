# User defined functions

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Function input and output type](#function-input-and-output-type)
- [Lambda function](#lambda-function)
- [Function as class](#function-as-class)
  - [FnInitParameters](#fninitparameters)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

For various cases you may not be satisfied with built-in functions provided by WaveBeans framework. To solve that issue you may define functions of your own. There are two way to define function, with one main difference -- whether or not you need to pass by some parameters from configuration runtime to execution runtime.

## Function input and output type

Each function must define the type of input and output. The type (both input and output) can be only one, to bypass or return a few different parameters as one you may use tuples for [two](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html) or [three](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/index.html) parameters or [data classes](https://kotlinlang.org/docs/reference/data-classes.html) for more.

Each operation has predefined type of input and output so you won't probably need to define it yourself, just follow the signature defined by the operation.

As the input parameter is the only one, there is a trick you may use to work with the parameters if they are defined as Pair, Triple, or Data class to keep your code cleaner. There is [destruction operation](https://kotlinlang.org/docs/reference/multi-declarations.html).

For example within lambda expression:

```kotlin
{ parameter -> parameter.first * parameter.second } // if you use parameter directly
{ it.first * it.second }                            // or implicitly,
{ (sample, multiplier) -> sample * multiplier }     // instead you may destruct it and give them proper name, 
                                                    // by specifying them inside parenthesises
```

To use within class definition:

```kotlin
fun apply(argument: Pair<Sample, Double>): Sample { // `argument` type is specified explicitly 
    val (sample, multiplier) = argument             // destruct it
    return sample * multiplier                      // apply the operation by using variable proper naming
}
```

For more information please follow [Kotlin documentation](https://kotlinlang.org/docs/reference/multi-declarations.html).

## Lambda function

If you don't need to bypass any parameters to execution runtime, you can define the function very shortly with so called [lambda-function](https://kotlinlang.org/docs/reference/lambdas.html#lambda-expressions-and-anonymous-functions). Inside lambda function the operand can be used as `it`, or defined explicitly. 
 
For example to define [map function](operations/map-operation.md):
 ```kotlin
    440.sine()
        .map { it * 2 } // `it` is defined by default
        .map { sample -> sample / 2 } // or you may define operand name explicitly 
```

In this case if you'll try to bypass parameter outside of the lambda expression and try to execute the stream, you'll get an exception with message like
`Wrapping function $clazzName failed, perhaps it is implemented as inner class and should be wrapped manually`. That'll highlight that you can't define the function that way and you need to define [proper class](#class-function).

This way is very compact and most of the time parameters contain everything that is required to perform the operation.
 
## Function as class

This is the most cumbersome way to define the function but at the same time the most flexible. You can define a function as a class, but keep in mind that shouldn't be the inner class or anonymous class. Also, to bypass parameters you would need to be able to serialize them into string representation. There are functions defined for primitive types, for your own classes you would need to do it on your own 

So, to define function as class you need to extend `Fn<T,R>` abstract class. That class has `initParameters` as constructor parameter, which is used to bypass parameters into the function body during execution. The class must have at least one constructor defined with no parameters -- meaning no parameters required, or with `initParameters` with type `io.wavebeans.lib.FnInitParameters`. However for convenience and readability it is recommended to provide second constructor that has parameters you want to bypass into execution runtime.

As an example let's define a [map function](operations/map-operation.md) that changes an amplitude of the audio stream by defined value:

```kotlin
class ChangeAmplitudeFn(parameters: FnInitParameters)  // there should be at least one constructor defined this way
: Fn<Sample, Sample>(parameters) {                     // extend Fn<T,R> class, Sample is input (T) and output (R) 
                                                       // types of the function.

    constructor(factor: Double)                        // for convenience let's define  proper constructor
      : this(FnInitParameters().add("factor", factor)) // and build parameters for our function 

    override fun apply(argument: Sample): Sample {     // here is the body of the function
        val factor = initParams.double("factor")       // extracting the double value of the factor parameter
        return argument * factor                       // and simply multiply sample by the specified factor,
                                                       // that changes its amplitude.
    }
}

// apply created function on the stream.
stream.map(ChangeAmplitudeFn(2.0))
```

### FnInitParameters

Type `io.wavebeans.lib.FnInitParameters` is the specific class that is used to bypass parameters from configuration runtime to execution runtime. For transferring all values should be serialized into strings.

There is an API for handling primitive types and their collections:
```kotlin
FnInitParameters()
    .add("double", 1.0) // will be stored as double string "1.0"
    .add("int", 123) // will be stored as int string "123"
    .add("string", "some_string") // will be stored as is
    .addStrings("strings", listOf("string1", "string2")) // will be stored as comma-separated strings "string1,string2"
    .addDoubles("doubles", listOf(1.0, 2.0)) // will be stored as comma separated double string "1.0,2.0"
    .addInts("ints", listOf(1, 2)) // will be stored as comma separated double string "1,2"
```
And it works similar wth floats and longs.

To store an object or any other type you would need to specify the stringifier that converts an object to a string.

```kotlin
FnInitParameters()
    .addObj("timeUnit", TimeUnit.MILLISECONDS) { it.name } // stringifying simple but different type
    .addObj("pairOfLongs", Pair(1L, 2L)) { "${it.first}:${it.second}" } // stringifying complex type
    .addObj("myListOfInts", listOf(1, 2, 3)) { it.joinToString(",") { it.toString() } } // stringifying collections your way
```

As you probably noticed, API of parameters allows you to specify parameters one by one without storing the result in interim variable, so these coding styles has same result:

```kotlin
// defining parameters with storing in interim variable
val p = FnInitParameters()
p.add("timeValue", 1)
p.addObj("timeUnit", TimeUnit.MILLISECONDS) { it.name }
MyFn(p)

// specifying parameters sequentially
MyFn(FnInitParameters()
    .add("timeValue", 1)
    .addObj("timeUnit", TimeUnit.MILLISECONDS) { it.name }
)
```

To read parameters you would need to specify explicitly what you want get. Keep in mind, some of the methods may work for different values stored as they are interchangeable in some sense (i.e. you can get int as double). All parameters are nullable, but you can ask for non-nullable value, you would need to specify it explicitly.

Primitive types:
```kotlin
val double = initParams.double("double") // get non-nullable double value
val doubleOrNull = initParams.doubleOrNull("double") // get nullable double value
val doubles = initParams.doubles("doubles") // get non-nullable list of doubles 
val doublesOrNull = initParams.doublesOrNull("doubles") // get nullable list of doubles
```
It works similar for float, int and long.

For getting an object, similar way to specifying stringifier you would need to specify objectifier that parses the value. You may get an object as nullable or not as well:
```kotlin
val timeUnit = initParams.obj("timeUnit") { TimeUnit.valueOf(it) }
val pairOfLongs = initParams.objOrNull("pairOfLongs") {
    val (first, second) = it.split(":").map { it.toLong() }.take(2)
    Pair(first, second)
}
val myListOfInts = initParams.obj("myListOfInts") { it.split(",").map { it.toInt() } }
```
