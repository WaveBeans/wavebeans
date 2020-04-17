package io.wavebeans.execution.distributed

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.nhaarman.mockitokotlin2.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.content.PartData
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.asInput
import io.wavebeans.execution.*
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.trim
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.util.concurrent.Callable
import kotlin.random.Random

class CrewGardenerSpec : Spek({
    val testEngine = TestApplicationEngine(createTestEnvironment())
    testEngine.start(wait = false)

    val gardener: Gardener = mock()
    val crewGardener = CrewGardener(
            advertisingHostAddress = "127.0.0.1",
            listeningPortRange = 40000..50000,
            startingUpAttemptsCount = 10,
            threadsNumber = 1,
            applicationEngine = testEngine,
            gardener = gardener
    )

    Thread { crewGardener.start(waitAndClose = true) }.start()
    afterGroup {
        crewGardener.terminate()
        crewGardener.close()
        testEngine.stop(1000, 1000)
    }

    val json = Json(JsonConfiguration.Stable, TopologySerializer.paramsModule)

    with(testEngine) {
        describe("Planting") {
            val pods1 = listOf(440.sine().trim(1000).toDevNull()).buildTopology().buildPods()
            val pods2 = listOf(880.sine().trim(500).toDevNull()).buildTopology().buildPods()

            val jobKey = newJobKey()
            val bushKey1 = newBushKey()
            val bushKey2 = newBushKey()

            fun plant(bushKey: BushKey, pods: List<PodRef>): TestApplicationCall {
                return handleRequest(Post, "/bush") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val bushContent = JobContent(bushKey, pods)
                    val plantBushRequest = PlantBushRequest(jobKey, bushContent, 44100.0f)
                    val body = json.stringify(PlantBushRequest.serializer(), plantBushRequest)
                    setBody(body)
                }
            }

            it("should plant one bush") {
                assertThat(plant(bushKey1, pods1)).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
                verify(gardener).plantBush(eq(jobKey), eq(bushKey1), any(), eq(44100.0f))
            }

            it("should plant second bush") {
                assertThat(plant(bushKey2, pods2)).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
                verify(gardener).plantBush(eq(jobKey), eq(bushKey2), any(), eq(44100.0f))
            }
        }

        describe("Describe job") {
            it("should describe job") {
                val jobKey = newJobKey()
                val bushKey1 = newBushKey()
                val bushKey2 = newBushKey()
                val pods1 = listOf(440.sine().trim(1000).toDevNull()).buildTopology().buildPods()
                val pods2 = listOf(880.sine().trim(500).toDevNull()).buildTopology().buildPods()
                whenever(gardener.job(eq(jobKey))).thenReturn(listOf(
                        JobDescriptor(bushKey1, pods1, 441000.0f, mock()),
                        JobDescriptor(bushKey2, pods2, 441000.0f, mock())
                ))
                assertThat(handleRequest(Get, "/job?jobKey=$jobKey")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull()
                                .prop("json") { json.parse(ListSerializer(JobContent.serializer()), it) }
                                .all {
                                    eachIndexed(2) { v, i ->
                                        when (i) {
                                            0 -> v.all {
                                                prop("bushKey") { it.bushKey }.isEqualTo(bushKey1)
                                                prop("pods.key") { it.pods.map { it.key } }.isEqualTo(pods1.map { it.key })
                                            }
                                            1 -> v.all {
                                                prop("bushKey") { it.bushKey }.isEqualTo(bushKey2)
                                                prop("pods.key") { it.pods.map { it.key } }.isEqualTo(pods2.map { it.key })
                                            }
                                        }
                                    }
                                }
                    }
                }
            }
            it("shouldn't describe job if it is absent") {
                val jobKey = newJobKey()
                whenever(gardener.job(eq(jobKey))).thenReturn(listOf())

                assertThat(handleRequest(Get, "/job?jobKey=$jobKey")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.NotFound)
                        content().isNotNull().isEqualTo("Not found")
                    }
                }
            }
        }

        describe("List jobs") {
            it("should return list of jobs") {
                val jobKey1 = newJobKey()
                val jobKey2 = newJobKey()
                whenever(gardener.jobs()).thenReturn(listOf(jobKey1, jobKey2))

                assertThat(handleRequest(Get, "/jobs")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull()
                                .prop("json") { json.parse(ListSerializer(UUIDSerializer), it) }
                                .isListOf(jobKey1, jobKey2)
                    }
                }
            }
        }

        describe("Cancel job") {
            val jobKey = newJobKey()

            it("should cancel the job") {
                assertThat(handleRequest(Delete, "/job?jobKey=$jobKey") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
                verify(gardener).cancel(eq(jobKey))
            }
        }

        describe("Upload") {
            val jobKey = newJobKey()
            val jarFile by memoized {
                compileCode(mapOf(
                        "MyClass.kt" to """
                            package io.wavebeans.execution.test
                            
                            class MyClass {
                                fun answer(): Int = 42
                            }
                        """.trimIndent()
                ))
            }
            val jarFile2 by memoized {
                compileCode(mapOf(
                        "MyClass2.kt" to """
                            package io.wavebeans.execution.test
                            
                            class MyClass2 {
                                fun answer(): Int = 42 * 2
                            }
                        """.trimIndent()
                ))
            }

            fun upload(file: File) = handleRequest(Post, "/code/upload") {
                val boundary = "***bbb***"

                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString())

                setBody(boundary, listOf(
                        PartData.FormItem(jobKey.toString(), {}, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline
                                        .withParameter(ContentDisposition.Parameters.Name, "jobKey")
                                        .toString()
                        )),
                        PartData.FileItem({ file.inputStream().asInput() }, {}, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline
                                        .withParameter(ContentDisposition.Parameters.Name, "file")
                                        .withParameter(ContentDisposition.Parameters.FileName, "code.jar")
                                        .toString()
                        ))
                ))
            }

            it("should upload the file") {
                assertThat(upload(jarFile)).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
            }

            lateinit var myClass: Class<*>
            it("should be able to create class instance and invoke the method") {
                myClass = WaveBeansClassLoader.classForName("io.wavebeans.execution.test.MyClass")
                assertThat(myClass)
                        .prop("[answer(): Int]") { it.getMethod("answer").invoke(it.newInstance()) }
                        .isEqualTo(42)
            }

            it("should upload the second file") {
                assertThat(upload(jarFile2)).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
            }

            it("should be able to create class instance and class loader should be the same") {
                val myClass2 = WaveBeansClassLoader.classForName("io.wavebeans.execution.test.MyClass2")
                assertThat(myClass2).all {
                    prop("[answer(): Int]") { it.getMethod("answer").invoke(it.newInstance()) }
                            .isEqualTo(42 * 2)
                    prop("classLoaded") { it.classLoader }.isEqualTo(myClass.classLoader)
                }
            }
        }

        describe("Terminating") {
            /* Though don't actually terminate as in real app. */
            it("should terminate") {
                assertThat(handleRequest(Get, "/terminate")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("Terminating")
                        verify(gardener).cancelAll()
                    }
                }
            }
        }
    }
})

private fun Assert<TestApplicationResponse>.content() = prop("content") { it.content }

private fun Assert<TestApplicationResponse>.status() = prop("status") { it.status() }

private fun Assert<TestApplicationCall>.requestHandled() = prop("requestHandled") { it.requestHandled }

private fun Assert<TestApplicationCall>.response() = prop("response") { it.response }

private fun compileCode(codeFiles: Map<String, String>): File {
    val jarFile = File(createTempDir("wavebeans-script"), "wavebeans.jar")
    val tempDir = createTempDir("wavebeans-test-code").also { it.deleteOnExit() }
    val scriptFiles = codeFiles.entries.map { (name, content) ->
        File(tempDir, name).also { it.writeText(content) }
    }
    val kotlinc = "kotlinc"
    val kotlinHome = System.getenv("KOTLIN_HOME")
            ?.takeIf { File("$it/$kotlinc").exists() }
            ?: System.getenv("PATH")
                    .split(":")
                    .firstOrNull { File("$it/$kotlinc").exists() }
            ?: throw IllegalStateException("$kotlinc is not located, make sure it is available via either" +
                    " PATH or KOTLIN_HOME environment variable")

    val compileCall = CommandRunner(
            "$kotlinHome/$kotlinc",
            "-d", jarFile.absolutePath, *scriptFiles.map { it.absolutePath }.toTypedArray(),
            "-cp", System.getProperty("java.class.path"),
            "-jvm-target", "1.8"
    ).call()

    if (compileCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the script. \n${String(compileCall.output)}")
    }

    return jarFile
}

data class CommandResult(
        val exitCode: Int,
        val output: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandResult

        if (exitCode != other.exitCode) return false
        if (!output.contentEquals(other.output)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exitCode
        result = 31 * result + output.contentHashCode()
        return result
    }
}

class CommandRunner(vararg commands: String) : Callable<CommandResult> {

    private val cmds = commands.toList()

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun call(): CommandResult {
        val id = Random.nextInt(0xFFFF, Int.MAX_VALUE).toString(16)
        log.debug { "[$id] Running command: ${cmds.joinToString(" ")}" }

        val tempDir = createTempDir().also { it.deleteOnExit() }
        val process = ProcessBuilder(*cmds.toTypedArray())
                .directory(tempDir)
                .start()

        return try {
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                CommandResult(
                        exitCode,
                        process.errorStream.readBytes()
                )
            } else {
                CommandResult(
                        exitCode,
                        process.inputStream.readBytes()
                )
            }
        } catch (e: InterruptedException) {
            log.debug { "[$id] Process $process destroying" }
            process.destroy()
            CommandResult(
                    -1,
                    process.inputStream.readBytes()
            )
        }.also { log.debug { "[$id] Exit=${it.exitCode}, Output:\n${String(it.output)}" } }
    }
}