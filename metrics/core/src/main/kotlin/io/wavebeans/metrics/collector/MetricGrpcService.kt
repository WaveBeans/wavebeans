package io.wavebeans.metrics.collector

import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.MetricApiGrpc
import io.wavebeans.communicator.MetricApiOuterClass
import io.wavebeans.communicator.sequence
import io.wavebeans.communicator.single
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.MetricService
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class MetricGrpcService(
        private val collectors: MutableMap<Long, MetricCollector<Any>>
) : MetricApiGrpc.MetricApiImplBase() {

    companion object {
        fun instance(): MetricGrpcService = MetricGrpcService(ConcurrentHashMap())
        private const val maxAttachAttempts = 100
    }

    override fun collectValues(
            request: MetricApiOuterClass.CollectValuesRequest,
            responseObserver: StreamObserver<MetricApiOuterClass.TimedValue>
    ) {
        responseObserver.sequence("MetricGrpcService.collectValues", request) {

            collectors[request.collectorId]
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

            var collectorId = 0L
            for (attempt in 1..maxAttachAttempts) {
                collectorId = Random.nextLong()
                if (collectors.putIfAbsent(collectorId, collector) == null) break
                if (attempt == maxAttachAttempts) throw IllegalStateException("Can't register collector within $maxAttachAttempts attempts")
            }
            MetricService.registerConnector(collector)

            MetricApiOuterClass.AttachCollectorResponse.newBuilder()
                    .setCollectorId(collectorId)
                    .build()
        }
    }
}