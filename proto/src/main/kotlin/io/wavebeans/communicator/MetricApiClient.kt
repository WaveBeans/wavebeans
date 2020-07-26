package io.wavebeans.communicator

class MetricApiClient(
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var blockingClient: MetricApiGrpc.MetricApiBlockingStub

    override fun createClient(channel: io.grpc.Channel) {
        blockingClient = MetricApiGrpc.newBlockingStub(channel)
    }

    fun <M, T> collectValues(
            lastCollectionTimestamp: Long,
            name: String,
            component: String,
            tags: Map<String, String>
    ): Sequence<MetricApiOuterClass.TimedValue> {
        return blockingClient.collectValues(MetricApiOuterClass.CollectValuesRequest.newBuilder()
                .setLastCollectionTimestamp(lastCollectionTimestamp)
                .setMetricObject(
                        MetricApiOuterClass.MetricObject.newBuilder()
                                .setName(name)
                                .setComponent(component)
                                .putAllTags(tags)
                                .build()
                )
                .build()
        ).asSequence()
    }
}