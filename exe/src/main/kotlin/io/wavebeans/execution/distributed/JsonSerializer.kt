package io.wavebeans.execution.distributed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

internal fun <T> T.toByteArray(serializer: KSerializer<T>): ByteArray =
        Json.stringify(serializer, this)
                .also { log.trace { "Serializing `$this` with `$it`" } }
                .encodeToByteArray()

internal fun <T> ByteArray.toObj(serializer: KSerializer<T>): T =
        Json.parse(serializer, String(this)
                .also { log.trace { "Deserializing `$it` with `$serializer`" } })