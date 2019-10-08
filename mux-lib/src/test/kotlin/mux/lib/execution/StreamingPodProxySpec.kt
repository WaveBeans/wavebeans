package mux.lib.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isZero
import assertk.catch
import com.nhaarman.mockitokotlin2.mock
import mux.lib.Sample
import mux.lib.asInt
import mux.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.random.Random

val rnd = Random(1234)

class PodProxyTester(
        val sequence: List<Sample>
) {
    val pointedTo = rnd.nextInt()

    val podDiscovery: PodDiscovery = mock()

    var iteratorStartCounter: Int = 0
        private set

    var iteratorNextCounter: Int = 0
        private set

    val bushCaller = object : BushCaller {

        var iterator: Iterator<Sample>? = null

        override fun call(request: String): PodCallResult {
            val call = Call.parseRequest(request)
            return when (call.method) {
                "iteratorStart" -> {
                    iteratorStartCounter++
                    iterator = sequence.asSequence().iterator()
                    PodCallResult.wrap(call, 1L)
                }
                "iteratorNext" -> {
                    iteratorNextCounter++
                    PodCallResult.wrap(call, if (iterator!!.hasNext()) iterator!!.next() else null)
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    val bushCallerRepository = object : BushCallerRepository(podDiscovery) {
        override fun create(bushKey: BushKey, podKey: PodKey): BushCaller = bushCaller
    }

    val podProxy = object : StreamingPodProxy<Sample>(
            pointedTo = pointedTo,
            bushCallerRepository = bushCallerRepository,
            podDiscovery = podDiscovery
    ) {}
}


object StreamingPodProxySpec : Spek({

    describe("Pod Proxy count amount of calls to Pod") {
        val podProxyTester = PodProxyTester((1..10).map { sampleOf(it) })

        val seq = podProxyTester.podProxy.asSequence(0.0f)
        it("should create a sequence") { assertThat(seq).isNotNull() }
        it("should call iteratorStart once") { assertThat(podProxyTester.iteratorStartCounter).isEqualTo(1) }
        val res = seq.take(10).map { it.asInt() }.toList()
        it("should read all samples") { assertThat(res).isEqualTo((1..10).toList()) }
        it("should call iteratorNext [10 (read samples) + 1 (pre-read sample)] times") {
            assertThat(podProxyTester.iteratorNextCounter).isEqualTo(11)
        }
    }
})