Map operation
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Using as lambda function](#using-as-lambda-function)
- [Using as class](#using-as-class)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Overview
--------

Map operation allows you to either alter the value of the object you're working with, or completely change the type of the stream. There is a set of types which are already define and you can work with, or you may define your own, please follow the [Types section of API reference document](../#types).

There are two main ways to define the map operation: via Lambda function or defining a proper class. The main difference is either you want to bypass any parameters or if the data provided inside the function is enough for you. Please follow [functions documentation](../functions.md) to get more information about nuances and limitations of each specific approach.

Using as lambda function
-------

To define a map operation using Lambda function you just need to call `map { }` on your stream. Let's for example make a function that changes the amplitude of the input stream. Usually you use [predefined change amplitude function](change-amplitude-operation.md) to achieve this, but as this operation is simple either ways, let's do it by calling map operation-- we double the sample value. 

```kotlin
440.sine()
    .map { it * 2 }
``` 

In this example, the input stream has type `BeanStream<Sample>`, hence it works with type `Sample`, which has defined all operations with scalar. So, to change the amplitude, we just change the value of the sample by multiplying it by two.

Let's have another example. Say we want to make sure the stream has no values more than 1 by modulus. And if the value is more than 1 let's just crop it to 1. You would need to use it before using any output like WAV to avoid overflow artifacts. Of course this is not the best function in practice, but for the sake of example it's good enough.

```kotlin
import kotlin.math.*

440.sine(amplitude = 2.0)
            .map { if (abs(it.asDouble()) > 1) sampleOf(sign(it.asDouble())) else it }
```

Here the sine is artificially generated with larger amplitude to have values more than 1. Also we're using some Kotlin SDK function from `math` packet, so we need to add an appropriate import. Inside the map function, we are checking the absolute value of the sample, and if it's bigger than 1 get call sign() which return -1 or 1 depending on the sign, which exactly what we need. Then convert it to a sample, otherwise return it untouched. The sample is converted explicitly to its double value, but double is already used as internal representation, however it is cleaner to call that method for the sake of compatibility with the future API.

Map function can also be used to convert one type to another. It is done the very same way, however as the return object you just define the object of a different type and the stream will convert to that type, and further down you'll be working with that type. Let's convert the stream to an int value which defines the sign of the input.

```kotlin
440.sine()
    .map { if (it > 0) 1 else  -1}
``` 

In that example the stream from the type `BeanStream<Sample>` is converted to `BeanStream<Int>` and instead of working with Sample you'll work with their signs only, and for example you may [merge](merge-operation.md) the stream with another stream and use that side effect that the sign will be changing with frequency 440Hz. 

Using as class
--------

When the function needs some arguments to be bypassed outside, or you just want to avoid defining the function in inline-style as the code of the function is too complex, you may define the map function as a class. First of all please follow [functions documentation](../functions.md).

Map operation converts some value `T` to some value `R`, so the type arguments of the class `Fn` correspond one-to-one with the map function.

Let's create a function that similar to example with lambda function above returns the sign of the sample, however ,instead of returning 1 or -1, applies the multiplier we provide, basically return some `value` with plus or minus sign. The class would look like this:

```kotlin
class SignFn(initParameters: FnInitParameters) : Fn<Sample, Int>(initParameters) {

    constructor(value: Int) : this(FnInitParameters().add("value", value))

    override fun apply(argument: Sample): Int {
        val value = initParams.int("value")
        return if (argument > 0) value else -value
    }
}
```

For the sake of convenience, as suggested in [functions reference](../functions.md), the secondary constructor defined to encapsulate logic of serialization of parameters to string inside the class.

Right now, to use that function within stream it as simple as instantiating the class with specific parameters using `map()` operation:

```kotlin
    440.sine()
            .map(SignFn(42))
```

*Note: when trying to run that examples do not forget to [trim](trim-operation.md) the stream and define the output.*

