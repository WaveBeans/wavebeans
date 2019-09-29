package mux.lib.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import mux.lib.MuxParams
import mux.lib.NoParams
import mux.lib.io.CsvSampleStreamOutputParams
import mux.lib.io.SineGeneratedInputParams
import mux.lib.stream.ChangeAmplitudeSampleStreamParams
import mux.lib.stream.MergedSampleStreamParams
import mux.lib.stream.TrimmedFiniteSampleStreamParams

object TopologySerializer {

    private val paramsModule = SerializersModule {
        polymorphic(MuxParams::class) {
            ChangeAmplitudeSampleStreamParams::class with ChangeAmplitudeSampleStreamParams.serializer()
            SineGeneratedInputParams::class with SineGeneratedInputParams.serializer()
            NoParams::class with NoParams.serializer()
            TrimmedFiniteSampleStreamParams::class with TrimmedFiniteSampleStreamParams.serializer()
            MergedSampleStreamParams::class with MergedSampleStreamParams.serializer()
            CsvSampleStreamOutputParams::class with CsvSampleStreamOutputParams.serializer()
        }
    }

    private val json = Json(context = paramsModule)

    fun deserialize(topology: String): Topology = json.parse(Topology.serializer(), topology)

    fun serialize(topology: Topology): String = json.stringify(Topology.serializer(), topology)
}