package io.wavebeans.execution

import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.*
import java.lang.Exception
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

@Serializer(forClass = KType::class)
object KTypeSerializer : KSerializer<KType> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): KType {
        val primitiveTypes = listOf(
                typeOf<Int>(),
                typeOf<Long>(),
                typeOf<String>(),
                typeOf<Float>(),
                typeOf<Double>()
        ).map { it.toString() to it }.toMap()

        val containerClasses = listOf(
                List::class,
                Map::class,
                Set::class
        ).map { it.qualifiedName to it }.toMap()

        fun decodeByName(name: String): KType {
            if (name.contains("?")) throw NotImplementedError("Nullable types are not supported at the moment")
            /*
            * TypeParameter workaround.
            * Need to detect it on serialization, thought can't create KTypeParameter instance properly also.
            * Not needed for now.
            */
            if (name.length == 1) return typeOf<Any>()

            try {
                val primitiveType = primitiveTypes[name]
                if (primitiveType != null) return primitiveType
                if (!name.contains('<')) {
                    return WaveBeansClassLoader.classForName(name).kotlin.createType()
                } else {
                    val className = name.takeWhile { it != '<' }
                    val projectedNamesAsString = name
                            .dropWhile { it != '<' }
                            .drop(1) // drop the symbol itself
                            .dropLastWhile { it != '>' }
                            .dropLast(1) // drop the symbol itself

                    val genericType = containerClasses[className] ?: WaveBeansClassLoader.classForName(className).kotlin

                    // parse project names, they may nest each other, e.g. `Q<R<S, T>, U>`
                    val projectedNames = mutableListOf<String>()
                    var i = 0
                    var insideOtherType = 0
                    var currentType = ""
                    val charArray = projectedNamesAsString.toCharArray()
                    while (i < charArray.size) {
                        val c = charArray[i]
                        when (c) {
                            '<' -> insideOtherType++
                            '>' -> insideOtherType--
                            ',' -> if (insideOtherType == 0) {
                                projectedNames += currentType.trim(' ', ',')
                                currentType = ""
                            }
                        }
                        currentType += c
                        i++
                    }
                    projectedNames += currentType.trim(' ', ',')

                    // convert names to projections
                    val arguments = projectedNames.map {
                        when {
                            it == "*" -> KTypeProjection.STAR
                            it.startsWith("out") -> KTypeProjection.covariant(decodeByName(it.removePrefix("out ")))
                            it.startsWith("in") -> KTypeProjection.contravariant(decodeByName(it.removePrefix("in ")))
                            else -> KTypeProjection.invariant(decodeByName(it))
                        }
                    }
                    return genericType.createType(arguments)
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Can't decode name $name", e)
            }

        }
        return decodeByName(decoder.decodeString())
    }
}