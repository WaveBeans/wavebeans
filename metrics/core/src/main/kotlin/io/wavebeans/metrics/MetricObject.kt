package io.wavebeans.metrics

import io.wavebeans.communicator.MetricApiOuterClass
import kotlin.reflect.jvm.jvmName

interface MetricObject<M : Any> {

    companion object {
        fun of(protoMetricObject: MetricApiOuterClass.MetricObject): MetricObject<*> {
            val tags = protoMetricObject.tagsMap.entries.map { it.key to it.value }.toTypedArray()
            // TODO replace with smarter way via reflection
            return when (protoMetricObject.type) {
                CounterMetricObject::class.jvmName -> counter(protoMetricObject.component, protoMetricObject.name, "not needed here").withTags(*tags)
                GaugeMetricObject::class.jvmName -> gauge(protoMetricObject.component, protoMetricObject.name, "not needed here").withTags(*tags)
                TimeMetricObject::class.jvmName -> time(protoMetricObject.component, protoMetricObject.name, "not needed here").withTags(*tags)
                else -> throw UnsupportedOperationException("${protoMetricObject.type} is not supported")
            }
        }


        fun counter(component: String, name: String, description: String, vararg possibleTags: String): CounterMetricObject {
            return CounterMetricObject(component, name, description, possibleTags.toList())
        }

        fun gauge(component: String, name: String, description: String, vararg possibleTags: String): GaugeMetricObject {
            return GaugeMetricObject(component, name, description, possibleTags.toList())
        }

        fun time(component: String, name: String, description: String, vararg possibleTags: String): TimeMetricObject {
            return TimeMetricObject(component, name, description, possibleTags.toList())
        }
    }

    /**
     * The component of the metric, i.e. `wavebeans.execution`. Some cnnecttor may reformat it, i.e. replace `.` with `_`.
     */
    val component: String

    /**
     * The specific name of the metric, i.e. `requestCount`.
     */
    val name: String

    /**
     * Description on how and when this metric is being collected.
     */
    val description: String

    /**
     * Defines the list of the possible tag names of the metric.
     */
    val possibleTags: List<String>

    /**
     * The tags of the metrics
     */
    val tags: Map<String, String>

    /**
     * Type of the metric object, to differentiate them from each other.
     */
    val type: String

    /**
     * Creates a copy of [MetricObject] with additional tags.
     */
    fun withTags(vararg newTags: Pair<String, String>): M

    /**
     * Creates a copy of [MetricObject] without any tags.
     */
    fun withoutTags(): M

    /**
     * Compares with another [MetricObject] ignoring tags.
     */
    fun isLike(another: MetricObject<*>): Boolean = component == another.component && name == another.name && this::class == another::class

    /**
     * Compares with another [MetricObject] respecting tags of [another].
     */
    fun isSuperOf(another: MetricObject<*>): Boolean =
            component == another.component
                    && name == another.name
                    && this::class == another::class
                    && tags == another.tags.filterKeys { it in tags.keys }
}



