package io.wavebeans.metrics

import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.MetricApiGrpc
import io.wavebeans.communicator.MetricApiOuterClass
import io.wavebeans.communicator.sequence

class MetricGrpcService(
        val collector: MetricCollector
) : MetricApiGrpc.MetricApiImplBase() {

    companion object {
        fun instance(collector: MetricCollector): MetricGrpcService = MetricGrpcService(collector)
    }

    override fun collectValues(request: MetricApiOuterClass.CollectValuesRequest, responseObserver: StreamObserver<MetricApiOuterClass.Values>) {
        responseObserver.sequence("MetricGrpcService.collectValues", request) {
            collector.collectValues(request.lastCollectionTimestamp)
                    .asSequence()
                    .map {
                        MetricApiOuterClass.Values.newBuilder()
                                .setMetricObject(MetricApiOuterClass.MetricObject.newBuilder()
                                        .setComponent(it.first.component)
                                        .setName(it.first.name)
                                        .build())
                                .addAllValues(
                                        it.second.map { e ->
                                            MetricApiOuterClass.LongToDoublePair.newBuilder()
                                                    .setFirst(e.first)
                                                    .setSecond(e.second)
                                                    .build()
                                        }
                                )
                                .build()
                    }
        }
    }
}