package io.wavebeans.execution

import io.wavebeans.lib.io.StreamOutput
import mu.KotlinLogging
import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sequentially launches all specified outputs.
 */
class LocalOverseer(
        override val outputs: List<StreamOutput<out Any>>
) : Overseer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val pool = Executors.newSingleThreadExecutor()

    private val earlyFinish = AtomicBoolean(false)

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        return outputs
                .map { out ->
                    pool.submit(Callable<ExecutionResult> {
                        log.info { "[$out] Evaluating" }
                        var i = 0L
                        try {
                            out.writer(sampleRate).use { writer ->
                                @Suppress("ControlFlowWithEmptyBody")
                                while (!earlyFinish.get() && writer.write()) {
                                    i++
                                    sleep(0)
                                }
                            }
                            log.info { "[$out] Finished evaluating. Done $i iterations" }
                            ExecutionResult.success()
                        } catch (e: InterruptedException) {
                            log.info { "[$out] Finished evaluating early. Done $i iterations" }
                            ExecutionResult.success()
                        } catch (e: Exception) {
                            log.error(e) { "[$out] Error running. Done $i iterations" }
                            ExecutionResult.error(e)
                        }
                    })
                }
    }

    override fun close() {
        log.info { "Overseer asked to close" }
        earlyFinish.set(true)
        pool.shutdown()
        if (!pool.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
            pool.shutdownNow()
        }
    }
}