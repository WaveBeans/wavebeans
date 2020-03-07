package io.wavebeans.cli.script

import io.wavebeans.lib.WaveBeansClassLoader
import mu.KotlinLogging.logger
import java.io.Closeable
import java.io.File
import java.lang.reflect.InvocationTargetException
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

    private val imports: List<String> = listOf( // TODO replace with compile time generation, reflection will slow everything down here, not reasonable
            "io.wavebeans.lib.*",
            "io.wavebeans.lib.io.*",
            "io.wavebeans.lib.math.*",
            "io.wavebeans.lib.stream.*",
            "io.wavebeans.lib.stream.fft.*",
            "io.wavebeans.lib.stream.window.*",
            "io.wavebeans.lib.table.*",
            "io.wavebeans.cli.script.*",
            "java.util.concurrent.TimeUnit.*" // to use time units easier
    ).map { "import $it" }

    private fun Any.parameter() = if (this is String) "\"${this}\"" else "$this"

    private val evaluator = runMode.clazz.simpleName + "(" + runOptions.map { "${it.key} = ${it.value.parameter()}" }.joinToString(", ") + ")"

    private val startCountDown = CountDownLatch(1)

    fun start(): ScriptRunner {
        check(task == null) { "Task is already started" }
        // extract imports from content
        val importsRegex = Regex("import\\s+[\\w.* ]+;?")
        val customImports = importsRegex.findAll(content)
                .map { it.groupValues.first().removeSuffix(";") }
                .toList()
        val cleanedContent = content.replace(importsRegex, "")

        val scriptContent = """
            package io.wavebeans.script
            
            ${(imports + customImports).joinToString(separator = "\n")}
            import mu.KotlinLogging.logger
            
            val log = logger {}
            
            class WaveBeansScript {

                val evaluator = $evaluator
                
                fun StreamOutput<*>.out() { evaluator.addOutput(this) }

                fun content() {
                    ${cleanedContent}
                }

                fun run() {
                    content()
                    
                    try {
                        log.info { "Script evaluation started" }
                        evaluator.eval(${sampleRate}f).all { it.get() }
                    } catch (e : java.lang.InterruptedException) {
                        log.info { "Script evaluation interrupted" }
                        // nothing to do
                    } catch (e : Exception) {
                        log.error(e) { "Script evaluation failed" } 
                        e.printStackTrace(System.err)
                        evaluator.close()
                        System.exit(1)
                    } finally {
                        evaluator.close() 
                        log.info { "Evaluator closed" }
                    }
                }
            }
        """.trimIndent()

        log.debug { "Evaluating the following script: \n$scriptContent" }
        val compileTask = executor.submit(Callable<File> {
            compile(scriptContent)
        })
        val jarFile = try {
            compileTask.get(30000, TimeUnit.MILLISECONDS)!!
        } catch (e: ExecutionException) {
            log.error(e) { "Can't compile the script:\n$scriptContent" }
            throw IllegalStateException("Can't compile the script: ${e.message}", e)
        } catch (e: TimeoutException) {
            log.error(e) { "Compilation took more than 30 sec:\n$scriptContent" }
            throw IllegalStateException("Compilation took more than 30 sec", e)
        }

        task = executor.submit(Callable {
            return@Callable try {
                startCountDown.countDown()
                run(jarFile)
                null
            } catch (e: Throwable) {
                log.error(e) { "Error while evaluating the following script: \n$scriptContent" }
                e
            }
        })

        check(startCountDown.await(10000, TimeUnit.MILLISECONDS)) { "The task is not started" }

        return this
    }

    private fun compile(script: String): File {
        val scriptFile = File(createTempDir("wavebeans-script"), "WaveBeansScript.kt")
        val jarFile = File(createTempDir("wavebeans-script"), "wavebeans.jar")

        scriptFile.writeText(script)

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
                "-d", jarFile.absolutePath, scriptFile.absolutePath,
                "-cp", System.getProperty("java.class.path"),
                "-jvm-target", "1.8"
        ).call()

        if (compileCall.exitCode != 0) {
            throw IllegalStateException("Can't compile the script. \n${String(compileCall.output)}")
        }

        return jarFile
    }

    private fun run(jarFile: File) {
        try {
            WaveBeansClassLoader.classLoader = ScriptClassLoader(jarFile)
            val scriptClazz = WaveBeansClassLoader.classForName("io.wavebeans.script.WaveBeansScript")
            val scriptInstance = scriptClazz.constructors.first().newInstance()
            val runMethod = scriptClazz.getMethod("run")
            runMethod.invoke(scriptInstance)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
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
        log.debug { "Attempting to interrupt the execution [waitToFinish=$waitToFinish]" }
        return (task?.cancel(true) ?: false)
                .also {
                    executor.shutdown()
                    if (waitToFinish)
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                    log.debug { "Interrupted the execution" }
                }
    }

    override fun close() {
        log.debug { "Shutting down script runner:\n $content" }
        interrupt()
        if (!executor.awaitTermination(closeTimeout, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }
}