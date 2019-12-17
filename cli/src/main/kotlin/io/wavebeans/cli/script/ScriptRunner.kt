package io.wavebeans.cli.script

import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import de.swirtz.ktsrunner.objectloader.LoadException
import mu.KotlinLogging.logger
import java.io.Closeable
import java.util.concurrent.*

class ScriptRunner(
        private val content: String,
        private val sampleRate: Float = 44100.0f,
        private val closeTimeout: Long = 10000L,
        runMode: RunMode = RunMode.LOCAL,
        runOptions: Map<String, Any> = emptyMap()
) : Closeable {

    companion object {
        private val log = logger {}
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var task: Future<Throwable?>? = null
    private val ktsObjectLoader = KtsObjectLoader()

    private val imports: List<String> = listOf( // TODO replace with compile time generation, reflection will slow everything down here, not reasonable
            "io.wavebeans.lib.*",
            "io.wavebeans.lib.io.*",
            "io.wavebeans.lib.math.*",
            "io.wavebeans.lib.stream.*",
            "io.wavebeans.lib.stream.fft.*",
            "io.wavebeans.lib.stream.window.*",
            "io.wavebeans.cli.script.*",
            "java.util.concurrent.TimeUnit.*" // to use time units easier
    ).map { "import $it" }

    private fun Any.parameter() = if (this is String) "\"${this}\"" else "$this"

    private val evaluator = when (runMode) {
        RunMode.LOCAL -> LocalScriptEvaluator::class
        RunMode.LOCAL_DISTRIBUTED -> LocalDistributedScriptEvaluator::class
    }.simpleName + "(" + runOptions.map { "${it.key} = ${it.value.parameter()}" }.joinToString(", ") + ")"

    private val functions = """
        val evaluator = $evaluator
        fun StreamOutput<*>.out() { evaluator.addOutput(this) }
    """.trimIndent()

    private val evaluate = """
        evaluator.eval(${sampleRate}f)
    """.trimIndent()


    private val startCountDown = CountDownLatch(1)

    fun start(): ScriptRunner {
        check(task == null) { "Task is already started" }
        // extract imports from content
        val importsRegex = Regex("import\\s+[\\w.* ]+;?")
        val customImports = importsRegex.findAll(content)
                .map { it.groupValues.first().removeSuffix(";") }
                .toList()
        val cleanedContent = content.replace(importsRegex, "")

        val scriptContent = listOf(
                (imports + customImports).joinToString(separator = "\n"),
                functions,
                cleanedContent,
                evaluate,
                "true // need to return something :/"
        ).joinToString(separator = "\n\n")
        log.debug { "Evaluating the following script: \n$scriptContent" }
        task = executor.submit(Callable {
            return@Callable try {
                startCountDown.countDown()
                ktsObjectLoader.load<Any?>(scriptContent)
                null
            } catch (e: Exception) {
                log.error(e) { "Error while evaluating the following script: \n$scriptContent" }
                if (e is LoadException) e.cause else e
            }
        })

        check(startCountDown.await(10000, TimeUnit.MILLISECONDS)) { "The task is not started" }

        return this
    }

    fun result(): Pair<Boolean, Throwable?> {
        val t = task
        check(t != null) { "Task is not started. Please call start() first." }
        return if (t.isDone) Pair(true, t.get()) else Pair(false, null)
    }

    fun awaitForResult(timeout: Long = Long.MAX_VALUE, unit: TimeUnit = TimeUnit.MILLISECONDS): Throwable? {
        val t = task
        check(t != null) { "Task is not started. Please call start() first." }
        return t.get(timeout, unit)
    }

    fun interrupt(waitToFinish: Boolean = false): Boolean {
        executor.shutdown()
        return task?.cancel(true) ?: false
                .also { if (waitToFinish) executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS) }
    }

    override fun close() {
        log.debug { "Shutting down script runner:\n $content" }
        interrupt()
        if (!executor.awaitTermination(closeTimeout, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }
}