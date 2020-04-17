package io.wavebeans.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.FftStreamParams
import io.wavebeans.lib.stream.window.WindowStreamParams
import io.wavebeans.lib.stream.window.WindowStreamParamsSerializer
import io.wavebeans.lib.table.*

object TopologySerializer {

    val paramsModule = SerializersModule {
        polymorphic(BeanParams::class) {
            ChangeAmplitudeSampleStreamParams::class with ChangeAmplitudeSampleStreamParams.serializer()
            SineGeneratedInputParams::class with SineGeneratedInputParams.serializer()
            NoParams::class with NoParams.serializer()
            TrimmedFiniteSampleStreamParams::class with TrimmedFiniteSampleStreamParams.serializer()
            CsvStreamOutputParams::class with CsvWindowStreamOutputParamsSerializer
            BeanGroupParams::class with BeanGroupParams.serializer()
            CsvFftStreamOutputParams::class with CsvFftStreamOutputParams.serializer()
            FftStreamParams::class with FftStreamParams.serializer()
            WindowStreamParams::class with WindowStreamParamsSerializer
            ProjectionBeanStreamParams::class with ProjectionBeanStreamParams.serializer()
            MapStreamParams::class with MapStreamParamsSerializer
            InputParams::class with InputParamsSerializer
            FunctionMergedStreamParams::class with FunctionMergedStreamParamsSerializer
            ListAsInputParams::class with ListAsInputParamsSerializer
            TableOutputParams::class with TableOutputParams.serializer()
            TableDriverStreamParams::class with TableDriverStreamParams.serializer()
        }
        polymorphic(TableQuery::class) {
            TimeRangeTableQuery::class with TimeRangeTableQuery.serializer()
            LastIntervalTableQuery::class with LastIntervalTableQuery.serializer()
        }
    }

    val jsonCompact = Json(context = paramsModule)

    val jsonPretty = Json(context = paramsModule, configuration = JsonConfiguration.Stable.copy(prettyPrint = true))

    fun deserialize(topology: String): Topology = jsonCompact.parse(Topology.serializer(), topology)

    fun serialize(topology: Topology, json: Json = jsonCompact): String = json.stringify(Topology.serializer(), topology)
}