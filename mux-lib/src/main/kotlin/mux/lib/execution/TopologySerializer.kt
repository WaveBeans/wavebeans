package mux.lib.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import mux.lib.BeanParams
import mux.lib.NoParams
import mux.lib.io.CsvSampleStreamOutputParams
import mux.lib.io.SineGeneratedInputParams
import mux.lib.stream.ChangeAmplitudeSampleStreamParams
import mux.lib.stream.MergedSampleStreamParams
import mux.lib.stream.TrimmedFiniteSampleStreamParams

object TopologySerializer {

    private val paramsModule = SerializersModule {
        polymorphic(BeanParams::class) {
            ChangeAmplitudeSampleStreamParams::class with ChangeAmplitudeSampleStreamParams.serializer()
            SineGeneratedInputParams::class with SineGeneratedInputParams.serializer()
            NoParams::class with NoParams.serializer()
            TrimmedFiniteSampleStreamParams::class with TrimmedFiniteSampleStreamParams.serializer()
            MergedSampleStreamParams::class with MergedSampleStreamParams.serializer()
            CsvSampleStreamOutputParams::class with CsvSampleStreamOutputParams.serializer()
        }
    }

    val jsonCompact = Json(context = paramsModule)

    val jsonPretty = Json(context = paramsModule, configuration = JsonConfiguration.Stable.copy(prettyPrint = true))

    fun deserialize(topology: String): Topology = jsonCompact.parse(Topology.serializer(), topology)

    fun serialize(topology: Topology, json: Json = jsonCompact): String = json.stringify(Topology.serializer(), topology)
}