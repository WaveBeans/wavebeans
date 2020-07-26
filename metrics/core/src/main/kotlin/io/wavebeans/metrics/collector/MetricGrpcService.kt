package io.wavebeans.metrics.collector

import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.MetricApiGrpc
import io.wavebeans.communicator.MetricApiOuterClass
import io.wavebeans.communicator.sequence
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
fun MetricGrpcService.attachCollector(
        collector: MetricCollector<*, *>
): MetricGrpcService {
    collectors.putIfAbsent(collector.metricObject.component to collector.metricObject.name, collector as MetricCollector<*, Any>)
    return this
}

class MetricGrpcService(
        internal val collectors: MutableMap<Pair<String, String>, MetricCollector<*, Any>>
) : MetricApiGrpc.MetricApiImplBase() {

    companion object {
        fun instance(): MetricGrpcService = MetricGrpcService(ConcurrentHashMap())
    }

    override fun collectValues(request: MetricApiOuterClass.CollectValuesRequest, responseObserver: StreamObserver<MetricApiOuterClass.TimedValue>) {
        responseObserver.sequence("MetricGrpcService.collectValues", request) {

            collectors[request.metricObject.component to request.metricObject.name]?.let { collector ->
                collector.collectValues(request.lastCollectionTimestamp)
                        .asSequence()
                        .map {
                            MetricApiOuterClass.TimedValue.newBuilder()
                                    .setTimestamp(it.timestamp)
                                    .setSerializedValue(collector.serialize(it.value))
                                    .build()
                        }

            } ?: emptySequence()


        }
    }
}