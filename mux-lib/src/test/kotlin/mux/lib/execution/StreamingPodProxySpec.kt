package mux.lib.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import com.nhaarman.mockitokotlin2.mock
import mux.lib.Sample
import mux.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@ExperimentalStdlibApi
class PodProxyTester(
        val pointedTo: AnyPod,
        val timeToReadAtOnce: Int = 1
) {
    val podDiscovery: PodDiscovery = mock()

    var iteratorStartCounter: Int = 0
        private set

    var iteratorNextCounter: Int = 0
        private set

    val bushCaller = object : BushCaller {

        override fun call(request: String): PodCallResult {
            val call = Call.parseRequest(request)
            when (call.method) {
                "iteratorStart" -> {
                    iteratorStartCounter++
                }
                "iteratorNext" -> {
                    iteratorNextCounter++
                }
                else -> throw UnsupportedOperationException()
            }
            return pointedTo.call(call)
        }
    }

    val bushCallerRepository = object : BushCallerRepository(podDiscovery) {
        override fun create(bushKey: BushKey, podKey: PodKey): BushCaller = bushCaller
    }

    val podProxy = object : StreamingPodProxy<Sample>(
            pointedTo = pointedTo.podKey,
            bushCallerRepository = bushCallerRepository,
            podDiscovery = podDiscovery,
            timeToReadAtOnce = timeToReadAtOnce
    ) {}
}


@UseExperimental(ExperimentalStdlibApi::class)
object StreamingPodProxySpec : Spek({

    describe("Pod Proxy count amount of calls to Pod") {
        val podProxyTester = PodProxyTester(
                pointedTo = newTestPod((1..10).toList()),
                timeToReadAtOnce = 5
        )

        val seq = podProxyTester.podProxy.asSequence(20.0f).flatMap { it.asSequence() }
        it("should create a sequence") { assertThat(seq).isNotNull() }
        it("should call iteratorStart once") { assertThat(podProxyTester.iteratorStartCounter).isEqualTo(1) }
        val res = seq.take(10).map { it.asInt() }.toList()
        it("should read all samples") { assertThat(res).isEqualTo((1..10).toList()) }
        it("should call iteratorNext 5 times 500ms by 100ms step") {
            assertThat(podProxyTester.iteratorNextCounter).isEqualTo(5)
        }
    }

    describe("Iterator testing") {
        describe("data fits in one buffer") {
            val seq = PodProxyTester(
                    pointedTo = newTestPod((1..2).toList()),
                    timeToReadAtOnce = 2
            ).podProxy.asSequence(2.0f).flatMap { it.asSequence() }
            val iterator = seq.iterator()

            it("should have value on 1st iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { it.next().asInt() }.isEqualTo(1)
                }
            }
            it("should have value on 2nd iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { it.next().asInt() }.isEqualTo(2)
                }
            }
            it("should not have value on 3rd iteration") {
                assertThat(iterator)
                        .prop("hasNext") { it.hasNext() }.isEqualTo(false)
                assertThat(catch { iterator.next() })
                        .isNotNull()
                        .isInstanceOf(NoSuchElementException::class)
                        .message()
                        .isNotNull()
                        .isNotEmpty()
            }
        }

        describe("data doesn't fit in one buffer") {
            val seq = PodProxyTester(
                    pointedTo = newTestPod((1..2).toList()),
                    timeToReadAtOnce = 2
            ).podProxy.asSequence(1.0f).flatMap { it.asSequence() }
            val iterator = seq.iterator()

            it("should have value on 1st iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { it.next().asInt() }.isEqualTo(1)
                }
            }
            it("should have value on 2nd iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { it.next().asInt() }.isEqualTo(2)
                }
            }
            it("should not have value on 3rd iteration") {
                assertThat(iterator)
                        .prop("hasNext") { it.hasNext() }.isEqualTo(false)
                assertThat(catch { iterator.next() })
                        .isNotNull()
                        .isInstanceOf(NoSuchElementException::class)
                        .message()
                        .isNotNull()
                        .isNotEmpty()
            }
        }


    }
})