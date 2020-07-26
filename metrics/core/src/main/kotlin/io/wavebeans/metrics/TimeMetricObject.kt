package io.wavebeans.metrics

data class TimeMetricObject(
        override val component: String,
        override val name: String,
        override val description: String,
        override val possibleTags: List<String>,
        override val tags: Map<String, String> = emptyMap()
) : MetricObject<TimeMetricObject> {

    override fun withTags(vararg newTags: Pair<String, String>): TimeMetricObject = TimeMetricObject(component, name, description, possibleTags, tags + newTags)

    override fun withoutTags(): TimeMetricObject = if (tags.isEmpty()) this else TimeMetricObject(component, name, description, possibleTags)

    /**
     * Calls [MetricConnector.time] on [MetricService].
     */
    fun time(valueInMs: Long) = MetricService.time(this, valueInMs)

    /**
     * Calls body automatically measuring and recording with [time] the execution duration.
     *
     * @param body the function to run.
     *
     * @return the result of [body] function call.
     */
    fun <T> measure(body: () -> T): T {
        val startedAt = System.currentTimeMillis()
        val ret = body()
        time(System.currentTimeMillis() - startedAt)
        return ret
    }
}