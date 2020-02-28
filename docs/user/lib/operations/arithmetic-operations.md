Arithmetic operations
========

There are a few arithmetic operations are defined for certain types. It is mainly the syntactic sugar for [map](map-operation.md) and [merge](merge-operation.md) operations, anyway can be useful sometimes to use therm instead.

Sample stream operations
---------

Sample stream `BeanStream<Sample>` you can sum, subtract, multiply and divide with another sample stream:

```kotlin
sampleStream1 + sampleStream2
sampleStream1 - sampleStream2
sampleStream1 * sampleStream2
sampleStream1 / sampleStream2
```

That means both stream are going to be merged together and specified operation will be used as a merge operation.

Windowed sample stream operations
--------

Windowed sample stream `BeanStream<Window<Sample>>` you canl sub, subtract, multiply and divide with another stream as well as with scalar value. 

```kotlin
windowSampleStream1 + windowSampleStream2
windowSampleStream1 - windowSampleStream2
windowSampleStream1 * windowSampleStream2
windowSampleStream1 / windowSampleStream2

windowSampleStream1 + scalarValue
windowSampleStream1 - scalarValue
windowSampleStream1 * scalarValue
windowSampleStream1 / scalarValue
```

Arithmetic operation over two stream is a wrapper over merge operation, as limitation for merging windowed stream window should have similar characteristics. Operation with scalar is a wrapper over map operation.