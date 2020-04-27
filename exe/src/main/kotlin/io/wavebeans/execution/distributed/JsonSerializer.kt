package io.wavebeans.execution.distributed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

internal fun <T> T.toByteArray(serializer: KSerializer<T>): ByteArray =
        try {
            Json.stringify(serializer, this)
//                        .also { log.trace { "Serializing `$this` with `$it`" } }
                    .encodeToByteArray()
        } catch (e: Throwable) {
            throw IllegalStateException("Can't serialize with `$serializer` object: $this", e)
        }

internal fun <T> ByteArray.toObj(serializer: KSerializer<T>): T =
        try {
            Json.parse(
                    serializer,
                    String(this)
//                                .also { log.trace { "Deserializing `$it` with `$serializer`" } }
            )
        } catch (e: Throwable) {
            throw IllegalStateException("Can't deserialize with `$serializer` the buffer as string: ${String(this)}", e)
        }