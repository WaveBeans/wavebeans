package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.typeOf

private inline fun <reified T> Json.str(): String = this.encodeToString(KTypeSerializer, typeOf<T>())

private data class TestGenericClass<T>(val value: T)
private data class TestPlainClass(val value: Int)

object KTypeSerializerSpec : Spek({

    val json = jsonCompact(serializersModuleOf(KTypeSerializer))

    describe("Serialization") {
        it("should serialize Int") { assertThat(json.str<Int>()).isEqualTo("\"kotlin.Int\"") }
        it("should serialize Long") { assertThat(json.str<Long>()).isEqualTo("\"kotlin.Long\"") }
        it("should serialize String") { assertThat(json.str<String>()).isEqualTo("\"kotlin.String\"") }
        it("should serialize Float") { assertThat(json.str<Float>()).isEqualTo("\"kotlin.Float\"") }
        it("should serialize Double") { assertThat(json.str<Double>()).isEqualTo("\"kotlin.Double\"") }
        it("should serialize List<Int>") { assertThat(json.str<List<Int>>()).isEqualTo("\"kotlin.collections.List<kotlin.Int>\"") }
        it("should serialize List<Map<Int, String>>") {
            assertThat(json.str<List<Map<Int, String>>>()).isEqualTo(
                    "\"kotlin.collections.List<" +
                            "kotlin.collections.Map<kotlin.Int, kotlin.String>" +
                            ">\"")
        }
        it("should serialize List<Map<Int, Map<TestGenericClass<Int>, TestPlainClass>>>") {
            assertThat(json.str<List<Map<Int, Map<TestGenericClass<Int>, TestPlainClass>>>>()).isEqualTo(
                    "\"kotlin.collections.List<" +
                            "kotlin.collections.Map<" +
                            "kotlin.Int, " +
                            "kotlin.collections.Map<" +
                            "io.wavebeans.execution.TestGenericClass<kotlin.Int>, " +
                            "io.wavebeans.execution.TestPlainClass" +
                            ">" +
                            ">" +
                            ">\"")
        }
        it("should serialize TestPlainClass") {
            assertThat(json.str<TestPlainClass>()).isEqualTo("\"io.wavebeans.execution.TestPlainClass\"")
        }
        it("should serialize TestGenericClass<TestPlainClass>") {
            assertThat(json.str<TestGenericClass<TestPlainClass>>()).isEqualTo(
                    "\"io.wavebeans.execution.TestGenericClass<" +
                            "io.wavebeans.execution.TestPlainClass" +
                            ">\"")
        }

    }

    describe("Deserialization") {
        it("should deserialize Int") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.Int\"")).isEqualTo(typeOf<Int>()) }
        it("should deserialize Long") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.Long\"")).isEqualTo(typeOf<Long>()) }
        it("should deserialize String") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.String\"")).isEqualTo(typeOf<String>()) }
        it("should deserialize Float") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.Float\"")).isEqualTo(typeOf<Float>()) }
        it("should deserialize Double") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.Double\"")).isEqualTo(typeOf<Double>()) }
        it("should deserialize List<Int>") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.collections.List<kotlin.Int>\"")).isEqualTo(typeOf<List<Int>>()) }
        it("should deserialize TestGenericClass<out Int>") { assertThat(json.decodeFromString(KTypeSerializer, "\"io.wavebeans.execution.TestGenericClass<out kotlin.Int>\"")).isEqualTo(typeOf<TestGenericClass<out Int>>()) }
        it("should deserialize TestGenericClass<in Int>") { assertThat(json.decodeFromString(KTypeSerializer, "\"io.wavebeans.execution.TestGenericClass<in kotlin.Int>\"")).isEqualTo(typeOf<TestGenericClass<in Int>>()) }
        it("should deserialize TestGenericClass<T> as TestGenericClass<Any>") { assertThat(json.decodeFromString(KTypeSerializer, "\"io.wavebeans.execution.TestGenericClass<&T>\"")).isEqualTo(typeOf<TestGenericClass<Any>>()) }
        it("should deserialize List<ABC> as List<Any>") { assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.collections.List<&ABC>\"")).isEqualTo(typeOf<List<Any>>()) }
        it("should deserialize List<Map<Int, String>>") {
            assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.collections.List<" +
                    "kotlin.collections.Map<kotlin.Int, kotlin.String>" +
                    ">\"")
            ).isEqualTo(typeOf<List<Map<Int, String>>>())
        }
        it("should deserialize TestPlainClass") {
            assertThat(json.decodeFromString(KTypeSerializer, "\"io.wavebeans.execution.TestPlainClass\"")).isEqualTo(typeOf<TestPlainClass>())
        }
        it("should deserialize TestGenericClass<TestPlainClass>") {
            assertThat(json.decodeFromString(KTypeSerializer, "\"io.wavebeans.execution.TestGenericClass<" +
                    "io.wavebeans.execution.TestPlainClass" +
                    ">\"")
            ).isEqualTo(typeOf<TestGenericClass<TestPlainClass>>())
        }
        it("should deserialize List<Map<Int, Map<TestGenericClass<Int>, TestPlainClass>>>") {
            assertThat(json.decodeFromString(KTypeSerializer, "\"kotlin.collections.List<" +
                    "kotlin.collections.Map<" +
                    "kotlin.Int, " +
                    "kotlin.collections.Map<" +
                    "io.wavebeans.execution.TestGenericClass<kotlin.Int>, " +
                    "io.wavebeans.execution.TestPlainClass" +
                    ">" +
                    ">" +
                    ">\"")
            ).isEqualTo(typeOf<List<Map<Int, Map<TestGenericClass<Int>, TestPlainClass>>>>())
        }
    }
})