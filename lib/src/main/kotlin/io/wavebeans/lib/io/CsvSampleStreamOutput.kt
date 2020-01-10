package io.wavebeans.lib.io

import io.wavebeans.lib.*
import java.util.concurrent.TimeUnit

fun BeanStream<Sample>.toCsv(
        uri: String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<Sample> {
    return toCsv(
            uri = uri,
            header = listOf("time ${timeUnit.abbreviation()}", "value"),
            elementSerializer = SampleCsvFn(timeUnit),
            encoding = encoding
    )
}

class SampleCsvFn(parameters: FnInitParameters) : Fn<Triple<Long, Float, Sample>, List<String>>(parameters) {

    constructor(timeUnit: TimeUnit) : this(FnInitParameters().addObj("timeUnit", timeUnit) { it.name })

    override fun apply(argument: Triple<Long, Float, Sample>): List<String> {
        val (idx, sampleRate, sample) = argument
        val tu = initParams.obj("timeUnit") { TimeUnit.valueOf(it) }
        val time = samplesCountToLength(idx, sampleRate, tu)
        return listOf(time.toString(), String.format("%.10f", sample))
    }
}

