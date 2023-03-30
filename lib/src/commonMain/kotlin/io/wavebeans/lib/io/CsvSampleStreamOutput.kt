package io.wavebeans.lib.io

import io.wavebeans.lib.*

/**
 * Streams the sample of type [Sample] into a CSV file by specified [uri]. The [timeUnit] allows you to specify
 * the time unit the 1st column will be in, the resulted output is always an integer.
 *
 * It looks like this:
 * ```csv
 * time ms,value
 * 0,0.000000001
 * 1,0.000000002
 * ```
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param timeUnit the [TimeUnit] to use for 1st column representation
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 *
 * @return [StreamOutput] to run the further processing on.
 */
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

/**
 * Streams the sample of type [Managed] [Sample] into a CSV file by specified [uri]. The [timeUnit] allows you to specify
 * the time unit the 1st column will be in, the resulted output is always an integer.
 *
 * It looks like this:
 * ```csv
 * time ms,value
 * 0,0.000000001
 * 1,0.000000002
 * ```
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param timeUnit the [TimeUnit] to use for 1st column representation
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 * @param suffix the function that is based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
 *               before the extension: `file:///home/user/my${suffix}.csv`
 *
 * @return [StreamOutput] to run the further processing on.
 */
fun <A : Any> BeanStream<Managed<OutputSignal, A, Sample>>.toCsv(
        uri: String,
        suffix: (A?) -> String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<Managed<OutputSignal, A, Sample>> {
    return toCsv(
            uri = uri,
            header = listOf("time ${timeUnit.abbreviation()}", "value"),
            elementSerializer = SampleCsvFn(timeUnit),
            suffix = wrap(suffix),
            encoding = encoding
    )
}

/**
 * Streams the sample of type [Managed][Sample] into a CSV file by specified [uri]. The [timeUnit] allows you to specify
 * the time unit the 1st column will be in, the resulted output is always an integer.
 *
 * It looks like this:
 * ```csv
 * time ms,value
 * 0,0.000000001
 * 1,0.000000002
 * ```
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param timeUnit the [TimeUnit] to use for 1st column representation
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 * @param suffix the function as an instance of [Fn] that is based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
 *               before the extension: `file:///home/user/my${suffix}.csv`
 *
 * @return [StreamOutput] to run the further processing on.
 */
fun <A : Any> BeanStream<Managed<OutputSignal, A, Sample>>.toCsv(
        uri: String,
        suffix: Fn<A?, String>,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<Managed<OutputSignal, A, Sample>> {
    return toCsv(
            uri = uri,
            header = listOf("time ${timeUnit.abbreviation()}", "value"),
            elementSerializer = SampleCsvFn(timeUnit),
            suffix = suffix,
            encoding = encoding
    )
}

/**
 * The [Fn] the converts [Sample] stream to its CSV presentation:
 *
 * The output looks like this:
 * ```csv
 * 1,0.000000002
 * ```
 */
class SampleCsvFn(parameters: FnInitParameters) : Fn<Triple<Long, Float, Sample>, List<String>>(parameters) {

    constructor(timeUnit: TimeUnit) : this(FnInitParameters().addObj("timeUnit", timeUnit) { it.name })

    override fun apply(argument: Triple<Long, Float, Sample>): List<String> {
        val (idx, sampleRate, sample) = argument
        val tu = initParams.obj("timeUnit") { TimeUnit.valueOf(it) }
        val time = samplesCountToLength(idx, sampleRate, tu)
        return listOf(time.toString(), sample.toString())
    }
}

