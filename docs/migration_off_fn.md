### Migration off `Fn` within `lib`

This document tracks the progress of migrating away from the `Fn` class and its related infrastructure within the `lib` module. The goal is to replace `Fn` with more standard or efficient functional representations where applicable.

#### Migration Instructions

The goal of this migration is to replace the use of the `Fn` class with standard Kotlin functional interfaces (lambdas) in the `lib` module while maintaining serialization compatibility in the `exe` module.

##### Step 1: Update the `lib` module

Modify the classes in the `lib` module to use standard Kotlin functional interfaces instead of `Fn`.

- Change constructor parameters and properties from `Fn<T, R>` to `(T) -> R` (or appropriate functional type).
- Update the implementation to call the lambda directly instead of using `.apply()`.
- Keep the `BeanParams` classes and other structures, but update their properties to use lambdas.
- If the component needs to access parameters from the environment (e.g., multiplier in `changeAmplitude`), it should use `ExecutionScope`.
- `ExecutionScope` should be added to the `BeanParams` class and passed to the lambda as a receiver: `ExecutionScope.(T) -> R`.
- If the `BeanParams` had a custom serializer within the `lib` module, it should be moved or replaced by a more general approach, as lambdas cannot be directly serialized by `kotlinx.serialization` without extra help.

Example with `ExecutionScope` (`MapStreamParams` in `io.wavebeans.lib.stream.MapStream`):
```kotlin
class MapStreamParams<T : Any, R : Any>(
    val scope: ExecutionScope,
    val transform: ExecutionScope.(T) -> R
) : BeanParams
```

Example (`InputParams` in `io.wavebeans.lib.io.FunctionInput`):
```kotlin
// Before
class InputParams<T : Any>(
    val generator: Fn<Pair<Long, Float>, T?>,
    val sampleRate: Float? = null
) : BeanParams

// After
class InputParams<T : Any>(
    val generator: (Long, Float) -> T?,
    val sampleRate: Float? = null
) : BeanParams
```

##### Step 2: Create a custom serializer in the `exe` module

Since lambdas are not serializable, create a custom `KSerializer` in the `exe` module (typically under `io.wavebeans.execution.serializer`) that wraps the lambda into an `Fn` during serialization and unwraps it during deserialization.

- The `serialize` method should use `io.wavebeans.lib.wrap()` to convert the lambda to an `Fn`.
- If using `ExecutionScope`, ensure it is also serialized (using `ExecutionScope.serializer()`) and passed to `wrap()` if necessary, or handled in the lambda returned by `deserialize`.
- The `deserialize` method should decode the `Fn` and then return a lambda that calls `fn.apply()`.
- Use `FnSerializer` to handle the actual serialization/deserialization of the wrapped `Fn`.

Example with `ExecutionScope` (`MapStreamParamsSerializer` in `io.wavebeans.execution.serializer`):
```kotlin
object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MapStreamParams::class.className()) {
        element("scope", ExecutionScope.serializer().descriptor)
        element("transformFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var fn: Fn<Any, Any>
            lateinit var scope: ExecutionScope
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> fn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            MapStreamParams<Any, Any>(scope) { fn.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: MapStreamParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeSerializableElement(descriptor, 1, FnSerializer, wrap(value.transform))
        }
    }
}
```

Example (`InputParamsSerializer` in `io.wavebeans.execution.serializer`):
```kotlin
object InputParamsSerializer : KSerializer<InputParams<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(InputParams::class.className()) {
        element("generateFn", FnSerializer.descriptor)
        element("sampleRate", Float.serializer().nullable.descriptor)
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        return decoder.decodeStructure(descriptor) {
            var sampleRate: Float? = null
            lateinit var func: Fn<Pair<Long, Float>, Any?>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> func = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Long, Float>, Any?>
                    1 -> sampleRate = decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            InputParams({ a, b -> func.apply(a to b) }, sampleRate)
        }
    }

    override fun serialize(encoder: Encoder, value: InputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, FnSerializer, wrap(value.generator))
            encodeNullableSerializableElement(descriptor, 1, Float.serializer().nullable, value.sampleRate)
        }
    }
}
```

##### Step 3: Register the serializer in `SerializationUtils.kt`

Update `io.wavebeans.execution.SerializationUtils.kt` to register the new serializer in the `beanParams()` method. This ensures that when a `BeanParams` is encountered during topology serialization, it uses your custom serializer.

```kotlin
fun SerializersModuleBuilder.beanParams() {
    polymorphic(BeanParams::class) {
        // ...
        subclass(InputParams::class, InputParamsSerializer)
        // ...
    }
}
```

#### Technical Debt

The following items are temporary measures introduced during the migration and should be resolved once the migration is complete:

- [ ] Remove deprecated `BeanStream<T>.map(transform: Fn<T, R>)` in `MapStream.kt`. It is currently kept for compatibility with components not yet migrated (e.g., `ChangeAmplitudeSampleStream`).

#### Classes to Migrate

- [ ] `io.wavebeans.lib.stream.SincResampleFn`
- [x] `io.wavebeans.lib.io.CsvStreamOutput`
- [x] `io.wavebeans.lib.io.CsvStreamOutputParams`
- [x] `io.wavebeans.lib.io.CsvPartialStreamOutput`
- [x] `io.wavebeans.lib.stream.window.MapWindowFn`
- [ ] `io.wavebeans.lib.stream.ResampleStreamParams`
- [ ] `io.wavebeans.lib.stream.ResampleBeanStream`
- [ ] `io.wavebeans.lib.stream.ResampleFiniteStream`
- [ ] `io.wavebeans.lib.stream.AbstractResampleStream`
- [x] `io.wavebeans.lib.io.InputParams` (in `io.wavebeans.lib.io.FunctionInput`)
- [x] `io.wavebeans.lib.io.Input` (in `io.wavebeans.lib.io.FunctionInput`)
- [ ] `io.wavebeans.lib.io.FunctionStreamOutput`
- [ ] `io.wavebeans.lib.io.FunctionStreamOutputParams`
- [ ] `io.wavebeans.lib.stream.FlattenStreamsParams` (in `io.wavebeans.lib.stream.FlattenStream`)
- [ ] `io.wavebeans.lib.stream.FlattenStream`
- [ ] `io.wavebeans.lib.stream.FlattenWindowStreamsParams` (in `io.wavebeans.lib.stream.FlattenWindowStream`)
- [ ] `io.wavebeans.lib.stream.FlattenWindowStream`
- [x] `io.wavebeans.lib.stream.FunctionMergedStreamParams`
- [x] `io.wavebeans.lib.stream.FunctionMergedStream`
- [x] Support `ExecutionScope` in `map`, `merge`, `FunctionMergedStream` and `MapStream`.
- [x] `io.wavebeans.lib.stream.MapStreamParams`
- [x] `io.wavebeans.lib.stream.MapStream`
- [ ] `io.wavebeans.lib.io.WavFileOutputParams`
- [ ] `io.wavebeans.lib.io.WavFileOutput`
- [ ] `io.wavebeans.lib.io.WavPartialFileOutput`
- [ ] `io.wavebeans.lib.stream.SimpleResampleFn`
- [ ] `io.wavebeans.lib.stream.window.WindowStreamParams`
- [ ] `io.wavebeans.lib.stream.window.WindowStream`
- [ ] `io.wavebeans.lib.table.TableOutputParams`
- [ ] `io.wavebeans.lib.table.TableOutput`
- [x] `io.wavebeans.lib.io.SampleCsvFn` (in `io.wavebeans.lib.io.CsvSampleStreamOutput`)
- [ ] `io.wavebeans.lib.io.WavInputParams`
- [ ] `io.wavebeans.lib.io.WavInput`
- [x] `io.wavebeans.lib.stream.ChangeAmplitudeFn` (in `io.wavebeans.lib.stream.ChangeAmplitudeSampleStream`)
- [x] `io.wavebeans.lib.stream.window.ScalarSampleWindowOpFn` (in `io.wavebeans.lib.stream.window.SampleScalarWindowStream`)
