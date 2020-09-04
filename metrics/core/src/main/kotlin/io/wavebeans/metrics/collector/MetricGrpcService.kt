package io.wavebeans.metrics.collector

import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.MetricApiGrpc
import io.wavebeans.communicator.MetricApiOuterClass
import io.wavebeans.communicator.sequence
import io.wavebeans.communicator.single
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.MetricService
import java.util.concurrent.ConcurrentHashMap

class MetricGrpcService(
        internal val collectors: MutableMap<Pair<String, String>, MetricCollector<*, Any>>
) : MetricApiGrpc.MetricApiImplBase() {

    companion object {
        fun instance(): MetricGrpcService = MetricGrpcService(ConcurrentHashMap())
    }

    override fun collectValues(
            request: MetricApiOuterClass.CollectValuesRequest,
            responseObserver: StreamObserver<MetricApiOuterClass.TimedValue>
    ) {
        responseObserver.sequence("MetricGrpcService.collectValues", request) {

            collectors[request.metricObject.component to request.metricObject.name]
                    ?.let { collector ->
                        collector.collectValues(request.collectUpToTimestamp)
                                .asSequence()
                                .map {
                                    MetricApiOuterClass.TimedValue.newBuilder()
                                            .setTimestamp(it.timestamp)
                                            .setSerializedValue(collector.serialize(it.value))
                                            .build()
                                }
                    }
                    ?: emptySequence()


        }
    }

    override fun attachCollector(
            request: MetricApiOuterClass.AttachCollectorRequest,
            responseObserver: StreamObserver<MetricApiOuterClass.AttachCollectorResponse>
    ) {
        responseObserver.single("MetricGrpcService.attachCollector", request) {
            val collectorClass = request.collectorClass
            val downstreamCollectors = request.downstreamCollectorsList.map { it }
            val refreshIntervalMs = request.refreshIntervalMs
            val granularValueInMs = request.granularValueInMs
            val metricObject = MetricObject.of(request.metricObject)

            val collector = createCollector(
                    collectorClass,
                    metricObject,
                    downstreamCollectors,
                    refreshIntervalMs,
                    granularValueInMs
            )

            attachCollector(collector)
            MetricService.registerConnector(collector)

            MetricApiOuterClass.AttachCollectorResponse.getDefaultInstance()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun attachCollector(collector: MetricCollector<*, *>): MetricGrpcService {
        collectors.putIfAbsent(
                collector.metricObject.component to collector.metricObject.name,
                collector as MetricCollector<*, Any>
        )
        return this
    }
}