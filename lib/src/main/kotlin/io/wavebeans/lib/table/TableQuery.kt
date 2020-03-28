package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import kotlinx.serialization.Serializable

interface TableQuery

@Serializable
data class TimeRangeTableQuery(
        val from: TimeMeasure,
        val to: TimeMeasure
) : TableQuery

@Serializable
data class LastIntervalTableQuery(
        val interval: TimeMeasure
) : TableQuery