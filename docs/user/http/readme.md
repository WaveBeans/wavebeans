# HTTP API

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [In-application usage](#in-application-usage)
- [Table Service](#table-service)
  - [Custom types](#custom-types)
- [Helper Types](#helper-types)
  - [Time Measure](#time-measure)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

WaveBeans framework allows expose to access some of the data to be expose via HTTP API. While the stream is being evaluated you may start the HTTP Server. It is possible via [CLI](../cli/readme.md#http-api) or in your application.

Http Service provides access to different underlying service implementations which has different purpose with REST API:
* [Table Service](#table-service): querying [tables](../lib/outputs/table-output.md).

If everything is good, the service returns 200 OK HTTP code.

## In-application usage

HTTP library is provided separately to avoid increasing artifact size without necessity. So first of all you need to add it as a dependency, i.e. for Gradle:

```groovy
dependecies {
    implementation "io.wavebeans:http:$wavebeans_version"
}
```

And on your application launch (or when you actually need it) start the HttpService providing the port you want to run it on, by default it'll start on port 8080. `HttpSerrice` implements `Closeable` interface, and calling `close()` when you finish is essential.

```kotlin
HttpService(serverPort = 12345).use {server ->
    server.start()

    // can do something here while the server is running
}

// or in more manual mode

// start the server
val server = HttpService().start()
//run queries over HTTP using any HTTP client
server.close()
```

## Table Service

Table Service allows you to query data being stored in [tables](../lib/outputs/table-output.md). All methods are start with `/table` path, then the name of the table is specified `/table/{tableName}` and then after it the method with its parameters.

Following methods available, mostly this methods are exposed from [table query functionality](../lib/outputs/table-output.md#querying):

* Getting last Interval: `/table/{tableName}/last/{interval}/{sampleRate?}`:
    * `tableName` -- the name of the table to query, if the table can't be found the API call with return `404 Not Found` HTTP code.
    * `interval` -- the interval you're requesting, the type of [TimeMeasure](#time-measure). If malformed you'll see `400 Bad Request` HTTP code.
    * `sampleRate` -- is optional parameter which defines which sample rate to use to form the stream. By default 44100. In this case only affects the offset time values.
* Getting specific time range: `/table/{tableName}/timeRange/{from}/{to}/{sampleRate?}`:
    * `tableName` -- the name of the table to query, if the table can't be found the API call with return `404 Not Found` HTTP code.
    * `from`, `to` -- the from and to values of the interval you're requesting, the type of [TimeMeasure](#time-measure). If malformed you'll see `400 Bad Request` HTTP code.
    * `sampleRate` -- is optional parameter which defines which sample rate to use to form the stream. By default 44100. In this case only affects the offset time values.

Both endpoints return stream as new-line separated JSON objects like this:

```json
{"offset":368162653061,"value":0.6921708580045118}
{"offset":368162675736,"value":0.6455957967580076}
{"offset":368162698411,"value":0.5964844019471909}
{"offset":368162721086,"value":0.5450296159930959}
{"offset":368162743761,"value":0.4914335880185192}
{"offset":368162766436,"value":0.43590687909747267}
{"offset":368162789111,"value":0.3786676351923965}
{"offset":368162811786,"value":0.3199407308930018}
{"offset":368162834461,"value":0.2599568846828487}
```

The `offset` is the time marker of the sample in nanoseconds, the value is serialized as JSON the sample value. For the type `Sample` it is simply `double` value. If you store the custom value in the table before returning it as a part of HTTP API you need to make it serializable. 

### Custom types

In order to be used custom types needs to be made serializable. If you're using primitive types like String or Integer, you don't need to define its serialization routine. 

If you use classes which uses primitive values as its fields it is enough to mark such class with `kotlinx.serialization.Serializable` annotation:

```kotlin
import kotlinx.serialization.* // import package however it is not required if you'll specify it explicitly with the annotation

@Serializable // specify the annotation over your class
data class MyType(val field1: Int, val field2: String)
```

For more serialization techniques follow official documentation of [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization)

## Helper Types

### Time Measure

Time measure is the type that allows you specify the time markers. It looks like this: `100ms` -- 100 milliseconds, `1.0d` -- 1 day, etc. It consists of two parts number and time unit which is not split up by anything. The number defines the amount of time within defined unit, it is always integer, so if it is defined as double only the integer part will be used.

Number formats, examples:
* `100`, `100L` -- any integer and long values.
* double `100.2` or as float `100.2f`, however will be rounded down to `100`
* `1e2` -- the value is double, but has only integer part so will be interpreted as `100`
* `1.2e2` -- the value is double as well, but keeping in mind mantias the value will be interpreted as `1.2 * 100 = 120.0 = 120`
* `-100`, `-1.2` -- all negatives are also supported.

The second part is time unit which is 1 or 2 latin symbols, case doesn't matter:
* `ns` -- nanoseconds
* `us` -- microseconds
* `ms` -- milliseconds
* `s` -- seconds
* `m` -- minutes
* `h` -- hours
* `d` -- days
