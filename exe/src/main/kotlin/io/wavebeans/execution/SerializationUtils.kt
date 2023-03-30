package io.wavebeans.execution

import io.wavebeans.execution.distributed.AnySerializer
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.table.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmName

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
    contextualClass(
        AfterFillingFiniteStreamParams<*>::zeroFiller.field(AnySerializer(Any::class))
    )
    polymorphic(BeanParams::class) {
//        subclass(SineGeneratedInputParams::class, SineGeneratedInputParams.serializer())
//        subclass(NoParams::class, NoParams.serializer())
//        subclass(TrimmedFiniteSampleStreamParams::class, TrimmedFiniteSampleStreamParams.serializer())

//        subclass(CsvStreamOutputParams::class, CsvStreamOutputParamsSerializer)
//        subclass(BeanGroupParams::class, BeanGroupParams.serializer())
//        subclass(CsvFftStreamOutputParams::class, CsvFftStreamOutputParams.serializer())
//        subclass(FftStreamParams::class, FftStreamParams.serializer())
//        subclass(WindowStreamParams::class, WindowStreamParamsSerializer)
//        subclass(ProjectionBeanStreamParams::class, ProjectionBeanStreamParams.serializer())
//        subclass(MapStreamParams::class, MapStreamParamsSerializer)
//        subclass(InputParams::class, InputParamsSerializer)
//        subclass(FunctionMergedStreamParams::class, FunctionMergedStreamParamsSerializer)
//        subclass(ListAsInputParams::class, ListAsInputParamsSerializer)
//        subclass(TableOutputParams::class, TableOutputParamsSerializer)
//        subclass(TableDriverStreamParams::class, TableDriverStreamParams.serializer())
//        subclass(WavFileOutputParams::class, WavFileOutputParamsSerializer)
//        subclass(FlattenWindowStreamsParams::class, FlattenWindowStreamsParamsSerializer)
//        subclass(FlattenStreamsParams::class, FlattenStreamsParamsSerializer)
//        subclass(ResampleStreamParams::class, ResampleStreamParamsSerializer)
//        subclass(WavInputParams::class, WavInputParams.serializer())
        subClass(
            ByteArrayLittleEndianInputParams::bitDepth.field(serializer()),
            ByteArrayLittleEndianInputParams::sampleRate.field(Float.serializer()),
            ByteArrayLittleEndianInputParams::buffer.field(serializer())
        )
//        subclass(FunctionStreamOutputParams::class, FunctionStreamOutputParamsSerializer)
    }
}

fun <O : Any, P : Any> KProperty1<O, P?>.field(serializer: KSerializer<P>): FieldSerializer<O, P> {
    return FieldSerializer(this, serializer)
}

inline fun <reified T : Any> SerializersModuleBuilder.contextualClass(vararg fields: FieldSerializer<T, *>) {
    this.contextual(ClassSerializer(T::class, *fields))
}

inline fun <B: Any, reified T : B> PolymorphicModuleBuilder<B>.subClass(vararg fields: FieldSerializer<T, *>) {
    this.subclass(T::class, ClassSerializer(T::class, *fields))
}

data class FieldSerializer<O : Any, P : Any>(
    val prop: KProperty1<O, P?>,
    val serializer: KSerializer<P>,
)

class ClassSerializer<T : Any>(
    val clazz: KClass<T>,
    vararg fields: FieldSerializer<T, *>,
) : KSerializer<T> {

    private val classFields = fields

    private fun instantiate(arguments: Array<Any?>): T {
        return clazz.constructors
            .firstOrNull { it.parameters.size == classFields.size }
            ?.call(arguments)
            ?: throw SerializationException("Constructor for arguments: $arguments were not found")
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(clazz.jvmName) {
        classFields.forEach {
            element(it.prop.name, it.serializer.descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        val dec = decoder.beginStructure(descriptor)
        val arguments = arrayOfNulls<Any>(classFields.size)
        val decodedElements = hashSetOf<Int>()
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> {
                    if (i < classFields.size) {
                        arguments[i] = dec.decodeSerializableElement(descriptor, i, classFields[i].serializer)
                        decodedElements += i
                    } else {
                        throw SerializationException("Unknown index $i: $classFields")
                    }
                }
            }
        }
        if (decodedElements.size != arguments.size) {
            throw SerializationException("Not all elements were decoded. Decoded=$decodedElements, fields=$classFields")
        }
        return instantiate(arguments)
    }

    override fun serialize(encoder: Encoder, value: T) {
        val structure = encoder.beginStructure(descriptor)
        classFields.forEachIndexed { i, fieldSerializer ->
            @Suppress("UNCHECKED_CAST")
            structure.encodeNullableSerializableElement(
                descriptor,
                i,
                fieldSerializer.serializer as KSerializer<Any>,
                fieldSerializer.prop.get(value)
            )
        }
        structure.endStructure(descriptor)
    }
}