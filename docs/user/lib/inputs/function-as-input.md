Function as an Input
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Function as an Input](#function-as-an-input)
  - [Syntax](#syntax)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Predefined Inputs are very handy but unfortunately not all the time. There is always a use case that is not covered in the framework or won't be covered at all. For such purposes WaveBeans supports inputs as custom functions. It has some limits though which is related to the way WaveBeans is being executed, you may read more about such limitations in [functions reference](../functions.md), that won't be covered here, as it's not related to the input itself.

Inputs are being generated based on two values: 
* Sample index which is growing every time the function is called, from 0 to basically infinity (2^63 to be exact, or Long.MAX_VALUE)
* Sample rate, which is desired sample rate that the stream is working in. It is expected that the input will adopt it, or just simply throw an exception, if conversion is not supported.

Function technically can return any type, however some operators are defined only for specific types, so be aware.

Syntax
-----

There are two ways to create an input which is inherited from two ways you can define the function itself. 

**No parameters function**

The first one is allowed if you don't need to use any external parameters during runtime, in this case let's create a sine with amplitude 1 and frequency 440 Hz. It creates the infiite stream:

```kotlin
import kotlin.math.* // we're going to use some Kotlin SDK functionality

input { (sampleIndex, sampleRate) -> sampleOf(1.0 * cos(sampleIndex / sampleRate * 2.0 * PI * 440.0))}
``` 

If you want to create a finite stream, after some time you may return `null`, which will highlight that the stream is over:

```kotlin
input { (sampleIndex, sampleRate) -> if (sampleIndex < 10) sampleOf(sampleIndex) else null }
```

Note: here we've used helper function `sampleOf()` which converts any numeric type to internal representation of `Sample`.

**Parameterized function**

If you want to create an input that expect some parameters or data during runtime, you would need to define a class which extend generic `Fn` class, serialize all parameters and they'll be passed over to the function during runtime. Let's take a look at the example. Let's say you want to define the sine input but frequency and amplitude are defined by parameters.


Let's define a class first:

1. You need to extend `Fn<T,R>` class, however you would to define properly type-parameters. The `T` which is type of input data is defined by input itself and is tuple `Pair<Long, Float>` -- sample index and sample rate respectively, and the `R` is the resulting type, which in our case will be `Sample?` as it should be nullable. That means we need to extend the class `Fn<Pair<Long, Float>, Sample?>`, `T == Pair<Long, Float>` and `R == Sample?`.
2. The `Fn<T, R>` is abstract class that requires serialized parameters to be passed, also the function class should have one constructor with the same parameter to be valid. So, let's just create one default constructor as by requirement of abstract function, and another constructor which we'll use further for the sake of convenience.
3. The body of our function is the `apply()` method. The parameters inside the body are accessed via property `initParams`. The `argument` has the input value of the function which is in our case sample index and sample rate bypassed as a tuple.  

```kotlin
import kotlin.math.* // we're going to use some Kotlin SDK functionality

class InputFn(initParams: FnInitParameters) // the default constructor let's leave as by requirement of the class 
: Fn<Pair<Long, Float>, Sample?>(initParams) { // extend the function class with exact type parameters

    // for convenience let's have another constructor, which encapsulates all the serialization
    constructor(frequency: Double, amplitude: Double) : this(
        FnInitParameters() // create an instance of parameters container 
            .add("frequency", frequency) // put the value of frequency under the key `frequency`
            .add("amplitude", amplitude) // put the value of amplitude under the key `amplitude`
    )

    // implement a body of the function
    override fun apply(argument: Pair<Long, Float>): Sample? {
        val (sampleIndex, sampleRate) = argument // destructure the tuple for convenience
        val frequency = initParams.double("frequency") // get the frequency parameter as double
        val amplitude = initParams.double("amplitude") // get the amplitude parameter as double
        // do the computation, which is also regular double value
        val sineX = amplitude * cos(sampleIndex / sampleRate * 2.0 * PI * frequency)
        // return it as sample
        return sampleOf(sineX)
    }
}
```

Then we can use that class at any place of the program like this:

```kotlin
input(InputFn(frequency = 440.0, amplitude = 1.0)) // using naming parameters

input(InputFn(440.0, 1.0)) // or just specifying both of the parameters one by one
``` 

That approach is more cumbersome but very flexible as you basically can do whatever you want and even call third party libraries methods.

**Low-level API**

As any input that one has lower level API which is just class `Input<T>`, where `T` is the type of the produced output. Also it works with instances of `Fn` only, so you have two ways to instantiate it:

1. Define a class which extends `Fn` and pass an instance of it, let's use our `InputFn` class from previous part:

    ```kotlin
    Input(InputParams(
            InputFn(frequency = 440.0, amplitude = 1.0)
    ))
    ```
 
2. You can wrap lambda expression using `Fn.wrap()` method, it'll do the trick, but you'll loose the ability to bypass parameters inside the function:

    ```kotlin
    Input(InputParams(
            Fn.wrap<Pair<Long, Float>, Sample?> { (sampleIndex, sampleRate) -> sampleOf(sampleIndex) }
    ))
    ```
