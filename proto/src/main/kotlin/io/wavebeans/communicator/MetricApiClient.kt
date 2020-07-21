package io.wavebeans.communicator

class MetricApiClient(
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var blockingClient: MetricApiGrpc.MetricApiBlockingStub

    override fun createClient(channel: io.grpc.Channel) {
        blockingClient = MetricApiGrpc.newBlockingStub(channel)
    }

    fun <T> collectValues(lastCollectionTimestamp: Long, metricObjectConverter: (MetricApiOuterClass.MetricObject) -> T): Sequence<Pair<T, List<Pair<Long, Double>>>> {
        return blockingClient.collectValues(MetricApiOuterClass.CollectValuesRequest.newBuilder()
                .setLastCollectionTimestamp(lastCollectionTimestamp)
                .build())
                .asSequence()
                .map {
                    val k = metricObjectConverter(it.metricObject)
                    val values = it.valuesList.map { it.first to it.second }
                    k to values
                }
    }
}