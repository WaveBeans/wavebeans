package io.wavebeans.execution

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.FftStreamParams
import io.wavebeans.lib.stream.window.WindowStreamParams
import io.wavebeans.lib.stream.window.WindowStreamParamsSerializer
import io.wavebeans.lib.table.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*

fun jsonCompact(paramsModule: SerializersModule? = null) = Json {
    serializersModule = paramsModule ?: EmptySerializersModule
}

fun jsonPretty(paramsModule: SerializersModule? = null) = Json {
    serializersModule = paramsModule ?: EmptySerializersModule
    prettyPrint = true
}

fun SerializersModuleBuilder.tableQuery() {
    polymorphic(TableQuery::class) {
        subclass(TimeRangeTableQuery::class, TimeRangeTableQuery.serializer())
        subclass(LastIntervalTableQuery::class, LastIntervalTableQuery.serializer())
        subclass(ContinuousReadTableQuery::class, ContinuousReadTableQuery.serializer())
    }
}

fun SerializersModuleBuilder.beanParams() {
    polymorphic(BeanParams::class) {
        subclass(SineGeneratedInputParams::class, SineGeneratedInputParams.serializer())
        subclass(NoParams::class, NoParams.serializer())
        subclass(TrimmedFiniteSampleStreamParams::class, TrimmedFiniteSampleStreamParams.serializer())
        subclass(CsvStreamOutputParams::class, CsvStreamOutputParamsSerializer)
        subclass(BeanGroupParams::class, BeanGroupParams.serializer())
        subclass(CsvFftStreamOutputParams::class, CsvFftStreamOutputParams.serializer())
        subclass(FftStreamParams::class, FftStreamParams.serializer())
        subclass(WindowStreamParams::class, WindowStreamParamsSerializer)
        subclass(ProjectionBeanStreamParams::class, ProjectionBeanStreamParams.serializer())
        subclass(MapStreamParams::class, MapStreamParamsSerializer)
        subclass(InputParams::class, InputParamsSerializer)
        subclass(FunctionMergedStreamParams::class, FunctionMergedStreamParamsSerializer)
        subclass(ListAsInputParams::class, ListAsInputParamsSerializer)
        subclass(TableOutputParams::class, TableOutputParamsSerializer)
        subclass(TableDriverStreamParams::class, TableDriverStreamParams.serializer())
        subclass(WavFileOutputParams::class, WavFileOutputParamsSerializer)
        subclass(FlattenWindowStreamsParams::class, FlattenWindowStreamsParamsSerializer)
        subclass(FlattenStreamsParams::class, FlattenStreamsParamsSerializer)
        subclass(ResampleStreamParams::class, ResampleStreamParamsSerializer)
        subclass(WavInputParams::class, WavInputParams.serializer())
        subclass(ByteArrayLittleEndianInputParams::class, ByteArrayLittleEndianInputParams.serializer())
        subclass(FunctionStreamOutputParams::class, FunctionStreamOutputParamsSerializer)
    }
}
