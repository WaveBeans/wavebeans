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
            collectUpToTimestamp: Long,
            type: String,
            name: String,
            component: String,
            tags: Map<String, String>
    ): Sequence<MetricApiOuterClass.TimedValue> {
        return blockingClient.collectValues(MetricApiOuterClass.CollectValuesRequest.newBuilder()
                .setCollectUpToTimestamp(collectUpToTimestamp)
                .setMetricObject(
                        MetricApiOuterClass.MetricObject.newBuilder()
                                .setType(type)
                                .setName(name)
                                .setComponent(component)
                                .putAllTags(tags)
                                .build()
                )
                .build()
        ).asSequence()
    }

    fun attachCollector(
            collectorClass: String,
            downstreamCollectors: List<String>,
            type: String,
            name: String,
            component: String,
            tags: Map<String, String>,
            refreshIntervalMs: Long,
            granularValueInMs: Long
    ) {
        blockingClient.attachCollector(MetricApiOuterClass.AttachCollectorRequest.newBuilder()
                .setCollectorClass(collectorClass)
                .setMetricObject(
                        MetricApiOuterClass.MetricObject.newBuilder()
                                .setType(type)
                                .setName(name)
                                .setComponent(component)
                                .putAllTags(tags)
                                .build()
                )
                .addAllDownstreamCollectors(downstreamCollectors)
                .setRefreshIntervalMs(refreshIntervalMs)
                .setGranularValueInMs(granularValueInMs)
                .build())
    }
}