package io.wavebeans.lib.io.table

import io.wavebeans.lib.TimeMeasure

interface TableRetentionPolicy {
    fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean
}

class TimeTableRetentionPolicy(
        val maximumDataLength: TimeMeasure
) : TableRetentionPolicy {

    override fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean =
            valueTimeMarker >= maximumTimeMarker - maximumDataLength

    override fun toString(): String {
        return "TimeTableRetentionPolicy(maximumDataLength=$maximumDataLength)"
    }

}