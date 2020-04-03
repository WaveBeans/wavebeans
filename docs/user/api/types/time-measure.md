Time-measure type
======

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Time measure type allows you to specify different time markers or time length. 

The class you would need to instantiate is `io.wavebeans.lib.TimeMeasure`, though there are a set of helper function that helps you to create the instance of the class out of any number:

```kotlin
1.ns       // 1 nanoseconds
2e3.us     // 2 * 10^3 microseconds
(1 + 3).ms // (1 + 3) milliseconds
2.1.s      // 2 seconds (!!!) 
2.1e3.ms    // but 2100 milliseconds 
3L.m       // 3 minutes
6.h        // 6 hours
1_000.d    // 1000 days
```

**Note:** Despite the fact that the time measure can be created from any number, its internal representation is long value, so when you create it ouf of double or float number the integer part will only be used.

With time measures you could use some arithmetic operations like sum or subtract. You can use them regardless of the actual time unit you've created the measure with, but the resulting time measure will be in nanoseconds:

```kotlin
2.s + 100.ms // is 2.1 * 10^9 nanoseconds = 2.1 seconds
119.s - 2.m  // is -1 * 10^9 nanoseconds = 1 second
```

Similar you can compare time measure with each other regardless of the time unit it was created with:

```kotlin
1.s < 1200.ms        // true
5.d == (5 * 86400).s // true
4.s >= 10e6.us       // false
```