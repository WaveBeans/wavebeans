package io.wavebeans.cli.script

import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import de.swirtz.ktsrunner.objectloader.LoadException
import mu.KotlinLogging.logger
import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ScriptRunner(
        private val content: String,
        private val sampleRate: Float = 44100.0f,
        private val closeTimeout: Long = 10000L
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
            Thread::class.qualifiedName + ".*",
            ArrayList::class.qualifiedName
    ).map { "import $it" }

    // TODO replace with proper evaluation environment
    private val functions = """
        val outputs = ArrayList<StreamOutput<*>>()
        
        fun StreamOutput<*>.out() { outputs += this }
        
        fun evalAllLocally() {
                outputs
                    .map { it.writer(${sampleRate}f) }
                    .forEach {
                        try {
                            while (it.write()) { sleep(0) }
                        } catch (e: InterruptedException) {
                            // if it's interrupted then we need to gracefully 
                            // close everything that we've already processed
                        } finally {
                            it.close()
                        }
                    }
        }
    """.trimIndent()

    fun start(): ScriptRunner {
        check(task == null) { "Task is already started" }
        // extract imports from content
        val importsRegex = Regex("import\\s+[\\w.*]+;?")
        val customImports = importsRegex.findAll(content)
                .map { it.groupValues.first().removeSuffix(";") }
                .toList()
        val cleanedContent = content.replace(importsRegex, "")

        val scriptContent = (imports + customImports).joinToString(separator = "\n") +
                "\n\n" + functions +
                "\n\n" + cleanedContent +
                "\n\n" + "evalAllLocally()" +
                "\n\n" + "true // need to return something :/"
        log.debug { "Evaluating the following script: \n$scriptContent" }
        task = executor.submit(Callable {
            return@Callable try {
                ktsObjectLoader.load<Any?>(scriptContent)
                null
            } catch (e: Exception) {
                log.error(e) { "Error while evaluating the following script: \n$scriptContent" }
                if (e is LoadException) e.cause else e
            }
        })
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