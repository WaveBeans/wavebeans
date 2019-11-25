package io.wavebeans.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.io.CsvSampleStreamOutputParams
import io.wavebeans.lib.io.SineGeneratedInputParams
import io.wavebeans.lib.stream.ChangeAmplitudeSampleStreamParams
import io.wavebeans.lib.stream.MergedSampleStreamParams
import io.wavebeans.lib.stream.TrimmedFiniteSampleStreamParams

object TopologySerializer {

    private val paramsModule = SerializersModule {
        polymorphic(BeanParams::class) {
            ChangeAmplitudeSampleStreamParams::class with ChangeAmplitudeSampleStreamParams.serializer()
            SineGeneratedInputParams::class with SineGeneratedInputParams.serializer()
            NoParams::class with NoParams.serializer()
            TrimmedFiniteSampleStreamParams::class with TrimmedFiniteSampleStreamParams.serializer()
            MergedSampleStreamParams::class with MergedSampleStreamParams.serializer()
            CsvSampleStreamOutputParams::class with CsvSampleStreamOutputParams.serializer()
            BeanGroupParams::class with BeanGroupParams.serializer()
        }
    }

    val jsonCompact = Json(context = paramsModule)

    val jsonPretty = Json(context = paramsModule, configuration = JsonConfiguration.Stable.copy(prettyPrint = true))

    fun deserialize(topology: String): Topology = jsonCompact.parse(Topology.serializer(), topology)

    fun serialize(topology: Topology, json: Json = jsonCompact): String = json.stringify(Topology.serializer(), topology)
}