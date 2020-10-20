package io.wavebeans.execution.distributed

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import com.nhaarman.mockitokotlin2.*
import io.wavebeans.communicator.FacilitatorApiClient
import io.wavebeans.communicator.JobStatusResponse.JobStatus.FutureStatus.*
import io.wavebeans.communicator.RegisterBushEndpointsRequest
import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.trim
import io.wavebeans.tests.compileCode
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.modules.SerializersModule
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.random.Random
import kotlin.reflect.jvm.jvmName

class FacilitatorGrpcServiceSpec : Spek({
    val gardener: Gardener = mock()
    val podDiscovery = object : PodDiscovery() {}
    val port1 = 40300
    val port2 = 40301
    val facilitator = Facilitator(
            communicatorPort = port1,
            threadsNumber = 1,
            gardener = gardener,
            podDiscovery = podDiscovery
    )
    val facilitatorApiClient by memoized(SCOPE) { FacilitatorApiClient("127.0.0.1:$port1") }

    beforeGroup {
        facilitator.start()
    }

    afterGroup {
        facilitatorApiClient.close()
        facilitator.terminate()
        facilitator.close()
    }

    describe("Planting") {
        val pods1 = listOf(440.sine().trim(1000).toDevNull()).buildTopology().buildPods()
        val pods2 = listOf(880.sine().trim(500).toDevNull()).buildTopology().buildPods()

        val jobKey = newJobKey()
        val bushKey1 = newBushKey()
        val bushKey2 = newBushKey()

        fun plant(bushKey: BushKey, pods: List<PodRef>) {
            facilitatorApiClient.plantBush(
                    jobKey,
                    bushKey,
                    44100.0f,
                    jsonCompact(SerializersModule { beanParams(); tableQuery() }).encodeToString(ListSerializer(PodRef.serializer()), pods)
            )
        }

        it("should plant one bush") {
            assertThat(catch { plant(bushKey1, pods1) }).isNull()
            verify(gardener).plantBush(eq(jobKey), eq(bushKey1), any(), eq(44100.0f))
        }

        it("should plant second bush") {
            assertThat(catch { plant(bushKey2, pods2) }).isNull()
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

            fun parsePods(s: String): List<PodRef> =
                    jsonCompact(SerializersModule { beanParams();tableQuery() })
                            .decodeFromString(ListSerializer(PodRef.serializer()), s)

            assertThat(facilitatorApiClient.describeJob(jobKey))
                    .isNotNull()
                    .eachIndexed(2) { v, i ->
                        when (i) {
                            0 -> v.all {
                                prop("bushKey") { UUID.fromString(it.bushKey) }.isEqualTo(bushKey1)
                                prop("pods.key") { parsePods(it.podsAsJson).map { it.key } }.isEqualTo(pods1.map { it.key })
                            }
                            1 -> v.all {
                                prop("bushKey") { UUID.fromString(it.bushKey) }.isEqualTo(bushKey2)
                                prop("pods.key") { parsePods(it.podsAsJson).map { it.key } }.isEqualTo(pods2.map { it.key })
                            }
                        }
                    }
        }
        it("shouldn't describe job if it is absent") {
            val jobKey = newJobKey()
            whenever(gardener.job(eq(jobKey))).thenReturn(listOf())

            assertThat(facilitatorApiClient.describeJob(jobKey)).isEmpty()
        }
    }

    describe("List jobs") {
        it("should return list of jobs") {
            val jobKey1 = newJobKey()
            val jobKey2 = newJobKey()
            whenever(gardener.jobs()).thenReturn(listOf(jobKey1, jobKey2))

            assertThat(facilitatorApiClient.listJobs())
                    .isListOf(jobKey1, jobKey2)
        }
    }

    describe("Cancel job") {
        val jobKey = newJobKey()

        it("should cancel the job") {
            assertThat(catch { facilitatorApiClient.stopJob(jobKey) }).isNull()
            verify(gardener).stop(eq(jobKey))
        }
    }

    describe("Start job") {
        val jobKey = newJobKey()

        it("should cancel the job") {
            assertThat(catch { facilitatorApiClient.startJob(jobKey) }).isNull()
            verify(gardener).start(eq(jobKey))
        }
    }

    describe("Startup classes") {
        it("should enlist startup classes") {
            assertThat(facilitatorApiClient.codeClasses()).all {
                prop("classesList") { it.flatMap { it.classesList.asSequence() }.toList() }.all {
                    isNotEmpty()
                    matchesPredicate { it.any { it.classPath.contains("wavebeans") } }
                }
            }
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

        fun upload(file: File) {
            facilitatorApiClient.uploadCode(jobKey, file.inputStream())
        }

        it("should upload the file") {
            assertThat(catch { upload(jarFile) }).isNull()
        }

        lateinit var myClass: Class<*>
        it("should be able to create class instance and invoke the method") {
            myClass = WaveBeansClassLoader.classForName("io.wavebeans.execution.test.MyClass")
            assertThat(myClass)
                    .prop("[answer(): Int]") { it.getMethod("answer").invoke(it.newInstance()) }
                    .isEqualTo(42)
        }

        it("should upload the second file") {
            assertThat(catch { upload(jarFile2) }).isNull()
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

    describe("Register bush endpoints") {
        val jobKey = newJobKey()
        val bushKey1 = newBushKey()
        val bushKey2 = newBushKey()
        val podKey1 = PodKey(Random.nextLong(0, 10000000), 0)
        val podKey2 = PodKey(Random.nextLong(0, 10000000), 0)
        val podKey3 = PodKey(Random.nextLong(0, 10000000), 0)
        val podKey4 = PodKey(Random.nextLong(0, 10000000), 0)

        it("should register") {
            val req = RegisterBushEndpointsRequest.newBuilder()
                    .setJobKey(jobKey.toString())
                    .addBushEndpoints(
                            RegisterBushEndpointsRequest.BushEndpoint.newBuilder()
                                    .setBushKey(bushKey1.toString())
                                    .setLocation("127.0.0.1:$port1")
                                    .addPods(
                                            RegisterBushEndpointsRequest.BushEndpoint.PodKey.newBuilder()
                                                    .setId(podKey1.id).setPartition(podKey1.partition)
                                                    .build()
                                    )
                                    .addPods(
                                            RegisterBushEndpointsRequest.BushEndpoint.PodKey.newBuilder()
                                                    .setId(podKey2.id).setPartition(podKey2.partition)
                                                    .build()
                                    )
                                    .build()
                    )
                    .addBushEndpoints(
                            RegisterBushEndpointsRequest.BushEndpoint.newBuilder()
                                    .setBushKey(bushKey2.toString())
                                    .setLocation("127.0.0.1:$port2")
                                    .addPods(
                                            RegisterBushEndpointsRequest.BushEndpoint.PodKey.newBuilder()
                                                    .setId(podKey3.id).setPartition(podKey3.partition)
                                                    .build()
                                    )
                                    .addPods(
                                            RegisterBushEndpointsRequest.BushEndpoint.PodKey.newBuilder()
                                                    .setId(podKey4.id).setPartition(podKey4.partition)
                                                    .build()
                                    )
                                    .build()
                    )
                    .build()
            assertThat(catch { facilitatorApiClient.registerBushEndpoints(req) }).isNull()
        }
        it("should discover remote bush 1") {
            assertThat(podDiscovery.bush(bushKey1))
                    .isNotNull()
                    .isInstanceOf(RemoteBush::class)
                    .prop("endpoint") { it.endpoint }.isEqualTo("127.0.0.1:$port1")
        }
        it("should discover remote bush 2") {
            assertThat(podDiscovery.bush(bushKey2))
                    .isNotNull()
                    .isInstanceOf(RemoteBush::class)
                    .prop("endpoint") { it.endpoint }.isEqualTo("127.0.0.1:$port2")
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

    describe("Job status") {
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
            assertThat(facilitatorApiClient.jobStatus(jobKey))
                    .prop("jobStatuses") { it.statusesList }.eachIndexed(5) { status, i ->
                        when (i) {
                            0 -> status.all {
                                prop("status") { it.status }.isEqualTo(IN_PROGRESS)
                                prop("hasExceptionDescriptor") { it.exception.hasExceptionDescriptor() }.isFalse()
                            }
                            1 -> status.all {
                                prop("status") { it.status }.isEqualTo(FAILED)
                                prop("exception") { it.exception }.isNotNull().all {
                                    prop("hasExceptionDescriptor") { it.hasExceptionDescriptor() }.isTrue()
                                    prop("clazz") { it.exceptionDescriptor.clazz }.isEqualTo(IllegalStateException::class.jvmName)
                                    prop("message") { it.exceptionDescriptor.message }.isEqualTo("failed to execute")
                                    prop("causes") { it.causesList }.isNotNull().isEmpty()
                                }
                            }
                            2 -> status.all {
                                prop("status") { it.status }.isEqualTo(FAILED)
                                prop("exception") { it.exception }.isNotNull().all {
                                    prop("hasExceptionDescriptor") { it.hasExceptionDescriptor() }.isTrue()
                                    prop("clazz") { it.exceptionDescriptor.clazz }.isEqualTo(IllegalArgumentException::class.jvmName)
                                    prop("message") { it.exceptionDescriptor.message }.isEqualTo("finished with error")
                                    prop("cause") { it.causesList }.isNotNull().eachIndexed(1) { v, _ ->
                                        v.prop("clazz") { it.clazz }.isEqualTo(NotImplementedError::class.jvmName)
                                        v.prop("message") { it.message }.isEqualTo("wat?")
                                    }
                                }
                            }
                            3 -> status.all {
                                prop("status") { it.status }.isEqualTo(DONE)
                                prop("hasExceptionDescriptor") { it.exception.hasExceptionDescriptor() }.isFalse()
                            }
                            4 -> status.all {
                                prop("status") { it.status }.isEqualTo(CANCELLED)
                                prop("hasExceptionDescriptor") { it.exception.hasExceptionDescriptor() }.isFalse()
                            }
                        }
                    }
        }
    }

    describe("Making calls to bushes") {
        val bushKey = newBushKey()
        val bush = mock<LocalBush>()
        podDiscovery.registerBush(bushKey, bush)

        val podId = 0L
        val podPartition = 0
        val request = "someMethod?param1=value&param2=3"

        val podCallResult = mock<PodCallResult>()
        val result = ByteArray(4) { it.toByte() }
        whenever(podCallResult.stream())
                .thenReturn(ByteArrayInputStream(result))
        whenever(bush.call(PodKey(podId, podPartition), request))
                .thenReturn(CompletableFuture<PodCallResult>().also { it.complete(podCallResult) })

        it("should call") {
            assertThat(facilitatorApiClient.call(bushKey, podId, podPartition, request))
                    .prop("byteContent") { it.readBytes() }.isEqualTo(result)
        }
    }

    describe("Terminating") {
        /* Though don't actually terminate as in real app. */
        it("should terminate") {
            assertThat(catch { facilitatorApiClient.terminate() }).isNull()
            verify(gardener).stopAll()
        }
    }
})
