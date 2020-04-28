package io.wavebeans.execution.distributed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal fun <T> T.toByteArray(serializer: KSerializer<T>): ByteArray =
        try {
            Json.stringify(serializer, this)
                    .encodeToByteArray()
        } catch (e: Throwable) {
            throw IllegalStateException("Can't serialize with `$serializer` object: $this", e)
        }

internal fun <T> ByteArray.toObj(serializer: KSerializer<T>): T =
        try {
            Json.parse(
                    serializer,
                    String(this)
            )
        } catch (e: Throwable) {
            throw IllegalStateException("Can't deserialize with `$serializer` the buffer as string: ${String(this)}", e)
        }