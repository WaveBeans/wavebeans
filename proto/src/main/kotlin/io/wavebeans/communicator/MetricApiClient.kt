package io.wavebeans.communicator

class MetricApiClient(
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var blockingClient: MetricApiGrpc.MetricApiBlockingStub

    override fun createClient(channel: io.grpc.Channel) {
        blockingClient = MetricApiGrpc.newBlockingStub(channel)
    }

    fun collectValues(
            collectUpToTimestamp: Long,
            collectorId: Long
    ): Sequence<MetricApiOuterClass.TimedValue> {
        return blockingClient.collectValues(MetricApiOuterClass.CollectValuesRequest.newBuilder()
                .setCollectUpToTimestamp(collectUpToTimestamp)
                .setCollectorId(collectorId)
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
    ): Long {
        return blockingClient.attachCollector(MetricApiOuterClass.AttachCollectorRequest.newBuilder()
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
                .collectorId
    }
}