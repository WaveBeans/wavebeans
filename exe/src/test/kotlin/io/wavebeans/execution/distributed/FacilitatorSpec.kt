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
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.content.PartData
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.asInput
import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.trim
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.random.Random
import kotlin.reflect.jvm.jvmName

class FacilitatorSpec : Spek({
    val testEngine = TestApplicationEngine(createTestEnvironment())
    testEngine.start(wait = false)

    val gardener: Gardener = mock()
    val podDiscovery = object : PodDiscovery() {}
    val facilitator = Facilitator(
            advertisingHostAddress = "127.0.0.1",
            listeningPortRange = 40000..50000,
            startingUpAttemptsCount = 10,
            threadsNumber = 1,
            applicationEngine = testEngine,
            gardener = gardener,
            podDiscovery = podDiscovery
    )

    Thread { facilitator.start().waitAndClose() }.start()

    afterGroup {
        facilitator.terminate()
        facilitator.close()
        testEngine.stop(1000, 1000)
    }

    val json = Json(JsonConfiguration.Stable, TopologySerializer.paramsModule)

    describe("Planting") {
        with(testEngine) {
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
    }

    describe("Describe job") {
        with(testEngine) {
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
    }

    describe("List jobs") {
        with(testEngine) {
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
    }

    describe("Cancel job") {
        with(testEngine) {
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
                verify(gardener).stop(eq(jobKey))
            }
        }
    }

    describe("Start job") {
        with(testEngine) {
            val jobKey = newJobKey()

            it("should cancel the job") {
                assertThat(handleRequest(Put, "/job?jobKey=$jobKey") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
                verify(gardener).start(eq(jobKey))
            }
        }
    }

    describe("Startup classes") {
        with(testEngine) {
            it("should enlist startup classes") {
                assertThat(handleRequest(Get, "/code/classes") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().all {
                            prop("json") { json.parse(ListSerializer(ClassDesc.serializer()), it) }.all {
                                isNotEmpty()
                                matchesPredicate { it.any { it.classPath.contains("wavebeans") } }
                            }
                        }
                    }
                }
            }
        }
    }

    describe("Upload") {
        with(testEngine) {
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
    }

    describe("Register bush endpoints") {
        with(testEngine) {
            val jobKey = newJobKey()
            val bushKey1 = newBushKey()
            val bushKey2 = newBushKey()
            val podKey1 = PodKey(Random.nextLong(0, 10000000), 0)
            val podKey2 = PodKey(Random.nextLong(0, 10000000), 0)
            val podKey3 = PodKey(Random.nextLong(0, 10000000), 0)
            val podKey4 = PodKey(Random.nextLong(0, 10000000), 0)
            it("should register") {
                assertThat(handleRequest(Post, "/bush/endpoints") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val registerBushEndpointsRequest = RegisterBushEndpointsRequest(
                            jobKey,
                            listOf(
                                    BushEndpoint(bushKey1, "http://127.0.0.1:40000", listOf(podKey1, podKey2)),
                                    BushEndpoint(bushKey2, "http://127.0.0.1:40001", listOf(podKey3, podKey4))
                            )
                    )
                    val body = json.stringify(RegisterBushEndpointsRequest.serializer(), registerBushEndpointsRequest)
                    setBody(body)
                }).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("OK")
                    }
                }
            }
            it("should discover remote bush 1") {
                assertThat(podDiscovery.bush(bushKey1))
                        .isNotNull()
                        .isInstanceOf(RemoteBush::class)
                        .prop("endpoint") { it.endpoint }.isEqualTo("http://127.0.0.1:40000")
            }
            it("should discover remote bush 2") {
                assertThat(podDiscovery.bush(bushKey2))
                        .isNotNull()
                        .isInstanceOf(RemoteBush::class)
                        .prop("endpoint") { it.endpoint }.isEqualTo("http://127.0.0.1:40001")
            }
            it("should discover pods on remote bush 1") {
                assertThat(podDiscovery.bushFor(podKey1)).isEqualTo(bushKey1)
                assertThat(podDiscovery.bushFor(podKey2)).isEqualTo(bushKey1)
            }
            it("should discover pods on remote bush 2") {
                assertThat(podDiscovery.bushFor(podKey3)).isEqualTo(bushKey2)
                assertThat(podDiscovery.bushFor(podKey4)).isEqualTo(bushKey2)
            }
        }
    }

    describe("Job status") {
        with(testEngine) {
            val jobKey = newJobKey()
            val cancelledFuture = mock<Future<ExecutionResult>>()
            whenever(cancelledFuture.isCancelled()).thenReturn(true)
            whenever(gardener.getAllFutures(eq(jobKey))).thenReturn(
                    listOf(
                            // execution in progress
                            CompletableFuture<ExecutionResult>(),
                            // failed to execute
                            CompletableFuture<ExecutionResult>()
                                    .also { it.completeExceptionally(IllegalStateException("failed to execute")) },
                            // finished with error
                            CompletableFuture<ExecutionResult>()
                                    .also { it.complete(ExecutionResult.error(IllegalArgumentException("finished with error", NotImplementedError("wat?")))) },
                            // finished with success
                            CompletableFuture<ExecutionResult>()
                                    .also { it.complete(ExecutionResult.success()) },
                            // execution cancelled
                            cancelledFuture
                    )
            )

            it("should return valid status") {
                assertThat(handleRequest(Get, "/job/status?jobKey=$jobKey")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull()
                                .prop("asJson") { json.parse(ListSerializer(JobStatus.serializer()), it) }.all {
                                    eachIndexed(5) { status, i ->
                                        when (i) {
                                            0 -> status.all {
                                                prop("status") { it.status }.isEqualTo(FutureStatus.IN_PROGRESS)
                                                prop("exception") { it.exception }.isNull()
                                            }
                                            1 -> status.all {
                                                prop("status") { it.status }.isEqualTo(FutureStatus.FAILED)
                                                prop("exception") { it.exception }.isNotNull().all {
                                                    prop("clazz") { it.descriptor.clazz }.isEqualTo(IllegalStateException::class.jvmName)
                                                    prop("message") { it.descriptor.message }.isEqualTo("failed to execute")
                                                    prop("causes") { it.causes.fromProtoValue() }.isNotNull().isEmpty()
                                                }
                                            }
                                            2 -> status.all {
                                                prop("status") { it.status }.isEqualTo(FutureStatus.FAILED)
                                                prop("exception") { it.exception }.isNotNull().all {
                                                    prop("clazz") { it.descriptor.clazz }.isEqualTo(IllegalArgumentException::class.jvmName)
                                                    prop("message") { it.descriptor.message }.isEqualTo("finished with error")
                                                    prop("cause") { it.causes.fromProtoValue() }.isNotNull().eachIndexed(1) { v, _ ->
                                                        v.prop("clazz") { it.clazz }.isEqualTo(NotImplementedError::class.jvmName)
                                                        v.prop("message") { it.message }.isEqualTo("wat?")
                                                    }
                                                }
                                            }
                                            3 -> status.all {
                                                prop("status") { it.status }.isEqualTo(FutureStatus.DONE)
                                                prop("exception") { it.exception }.isNull()
                                            }
                                            4 -> status.all {
                                                prop("status") { it.status }.isEqualTo(FutureStatus.CANCELLED)
                                                prop("exception") { it.exception }.isNull()
                                            }
                                        }

                                    }
                                }
                    }
                }

            }
        }
    }

    describe("Making calls to bushes") {
        with(testEngine) {
            val bushKey = newBushKey()
            val bush = mock<LocalBush>()
            podDiscovery.registerBush(bushKey, bush)

            val podId = 0L
            val podPartition = 0
            val request = "someMethod?param1=value&param2=3"

            val podCallResult = mock<PodCallResult>()
            val result = ByteArray(4) { it.toByte() }
            whenever(podCallResult.writeTo(any()))
                    .then { (it.arguments[0] as OutputStream).write(result) }
            whenever(bush.call(PodKey(podId, podPartition), request))
                    .thenReturn(CompletableFuture<PodCallResult>().also { it.complete(podCallResult) })

            it("should call") {
                assertThat(handleRequest(Get, "/bush/call?bushKey=$bushKey&podId=$podId&podPartition=$podPartition&request=${request.encodeURLParameter()}")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        byteContent().isNotNull().isEqualTo(result)
                    }
                }
            }
        }
    }

    describe("Terminating") {
        with(testEngine) {
            /* Though don't actually terminate as in real app. */
            it("should terminate") {
                assertThat(handleRequest(Get, "/terminate")).all {
                    requestHandled().isTrue()
                    response().all {
                        status().isNotNull().isEqualTo(HttpStatusCode.OK)
                        content().isNotNull().isEqualTo("Terminating")
                        verify(gardener).stopAll()
                    }
                }
            }
        }
    }
})

private fun Assert<TestApplicationResponse>.content() = prop("content") { it.content }
private fun Assert<TestApplicationResponse>.byteContent() = prop("content") { it.byteContent }

private fun Assert<TestApplicationResponse>.status() = prop("status") { it.status() }

private fun Assert<TestApplicationCall>.requestHandled() = prop("requestHandled") { it.requestHandled }

private fun Assert<TestApplicationCall>.response() = prop("response") { it.response }


