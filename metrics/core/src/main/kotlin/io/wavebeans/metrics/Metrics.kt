package io.wavebeans.metrics

const val tableTag = "table"
const val bitDepthTag = "bitDepth"
const val formatTag = "format"
const val limitTag = "limit"
const val offsetTag = "offset"
const val clazzTag = "class"
const val typeTag = "type"

val audioStreamRequestMetric = MetricObject.counter(
        "io.wavebeans.http.audioService.stream",
        "requestCount",
        "Counts the number of requests for streaming on Audio Service.",
        tableTag, bitDepthTag, formatTag, limitTag, offsetTag
)
val audioStreamRequestTimeMetric = MetricObject.time(
        "io.wavebeans.http.audioService.stream",
        "requestTime",
        "Measures the time taken to handle the incoming request and start streaming on Audio Service.",
        tableTag, bitDepthTag, formatTag, limitTag, offsetTag
)
val audioStreamBytesSentMetric = MetricObject.counter(
        "io.wavebeans.http.audioService.stream",
        "bytesSent",
        "Counts the number of bytes sent during stream on Audio Service",
        tableTag, bitDepthTag, formatTag, limitTag, offsetTag
)

val samplesProcessedOnOutputMetric = MetricObject.counter(
        "io.wavebeans.lib.output",
        "samplesProcessed",
        "Counts the number of samples processed by the output",
        clazzTag
)

val samplesProcessedOnInputMetric = MetricObject.counter(
        "io.wavebeans.lib.input",
        "samplesProcessed",
        "Counts the number of samples processed by the input",
        clazzTag, typeTag
)

val flushedOnOutputMetric = MetricObject.counter(
        "io.wavebeans.lib.output",
        "flushed",
        "Counts the number of times the output was flushed if applicable",
        clazzTag
)

val gateStateOnOutputMetric = MetricObject.gauge(
        "io.wavebeans.lib.output",
        "gate.state",
        "Reflects the state of the gate of the specific output, 0 is closed, 1 is opened",
        clazzTag
)

val bytesProcessedOnOutputMetric = MetricObject.counter(
        "io.wavebeans.lib.output",
        "bytesProcessed",
        "Counts the number of bytes processed by the output",
        clazzTag
)

