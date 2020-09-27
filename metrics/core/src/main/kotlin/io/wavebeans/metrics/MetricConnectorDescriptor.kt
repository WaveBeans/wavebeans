package io.wavebeans.metrics

import mu.KotlinLogging
import kotlin.reflect.KFunction
import kotlin.reflect.typeOf

/**
 * Descriptor to create a [MetricConnector] from the class available only during runtime. The [clazz] should be
 * discoverable via current thread class loader.
 */
data class MetricConnectorDescriptor(
        /**
         * The class to create a connector from, must be subtype of [MetricConnector].
         */
        val clazz: String,
        /**
         * The properties to by-pass to the constructor of the [clazz].
         */
        val properties: Map<String, String>
) {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    /**
     * Creates with [create] and registers the metric connector using [MetricService.registerConnector].
     *
     * @return the instance of the [MetricConnector] that was created.
     */
    fun createAndRegister(): MetricConnector {
        val connector = create()
        MetricService.registerConnector(connector)
        return connector
    }

    /**
     * Creates a [MetricConnector] based on the specified parameters [clazz] and [properties].
     *
     * @return newly created [MetricConnector]
     *
     * @throws IllegalStateException if there was problems during class location.
     * @throws IllegalArgumentException if the constructor with desired parameters wasn't found:
     *                                  it might be either absent or not public. Ex
     */
    fun create(): MetricConnector {
        val clazz = try {
            Class.forName(clazz).kotlin
        } catch (e: Exception) {
            throw IllegalStateException("Can't locate ${clazz} class", e)
        }
        val constructor = clazz.constructors
                .filterIsInstance<KFunction<MetricConnector>>()
                .filter { c ->
                    c.parameters.size == properties.size &&
                            c.parameters
                                    .map { it.name }
                                    .zip(properties.keys)
                                    .all { it.first == it.second }
                }
                .also {
                    require(it.size == 1) {
                        "Can't find appropriate constructor with parameters: " +
                                "${properties}. Following constructors are presented:\n" +
                                clazz.constructors.joinToString("\n") { it.parameters.joinToString() }
                    }
                }
                .first()
        val parametersValues = constructor.parameters.map { kParameter ->
            val parameterValueProto = checkNotNull(properties[kParameter.name]) {
                "$kParameter cannot be located in the properties: ${properties}"
            }
            kParameter to when (kParameter.type) {
                typeOf<Int>() -> parameterValueProto.toInt()
                typeOf<Int?>() -> parameterValueProto.toIntOrNull()
                typeOf<Long>() -> parameterValueProto.toLong()
                typeOf<Long?>() -> parameterValueProto.toLongOrNull()
                typeOf<String>() -> parameterValueProto
                typeOf<String?>() -> if (parameterValueProto.toLowerCase() == "null") null else parameterValueProto
                typeOf<Boolean>() -> parameterValueProto.toBoolean()
                typeOf<Boolean?>() -> if (parameterValueProto.toLowerCase() == "null") null else parameterValueProto.toBoolean()
                typeOf<Float>() -> parameterValueProto.toFloat()
                typeOf<Float?>() -> parameterValueProto.toFloatOrNull()
                typeOf<Double>() -> parameterValueProto.toDouble()
                typeOf<Double?>() -> parameterValueProto.toDoubleOrNull()
                else -> throw UnsupportedOperationException("${kParameter.type} is not suppported")
            }
        }.toMap()

        log.info { "Instantiating metric connector $this using constructor $constructor and parameters $parametersValues" }

        val metricConnector = constructor.callBy(parametersValues)
        return metricConnector
    }
}