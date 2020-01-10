package io.wavebeans.lib

import java.util.concurrent.TimeUnit

fun TimeUnit.abbreviation(): String {
    return when (this) {
        TimeUnit.NANOSECONDS -> "ns"
        TimeUnit.MICROSECONDS -> "us"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.SECONDS -> "s"
        TimeUnit.MINUTES -> "m"
        TimeUnit.HOURS -> "h"
        TimeUnit.DAYS -> "d"
    }
}