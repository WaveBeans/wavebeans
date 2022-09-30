package io.wavebeans.cli.script

import mu.KotlinLogging.logger
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmCompiledModuleInMemoryImpl
import java.io.Closeable
import java.io.File
import java.util.concurrent.*
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.reflect.jvm.jvmName
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.PropertiesCollection

class ScriptRunner(
    private val content: String,
    private val sampleRate: Float = 44100.0f,
    private val closeTimeout: Long = 10000L,
    private val runMode: RunMode = RunMode.LOCAL,
    private val runOptions: Map<String, Any> = emptyMap()
) : Closeable {

    companion object {
        private val log = logger {}
        private var scriptGeneration = 0
    }

    private val executor =
        Executors.newSingleThreadExecutor { Thread(it, "script-runner-${it.hashCode().absoluteValue.toString(16)}") }
    private var task: Future<Throwable?>? = null

    private val imports: List<String> =
        listOf( // TODO replace with compile time generation, reflection will slow everything down here, not reasonable
            "io.wavebeans.lib.*",
            "io.wavebeans.lib.io.*",
            "io.wavebeans.lib.math.*",
            "io.wavebeans.lib.stream.*",
            "io.wavebeans.lib.stream.fft.*",
            "io.wavebeans.lib.stream.window.*",
            "io.wavebeans.lib.table.*",
            "io.wavebeans.cli.script.*",
            "java.util.concurrent.TimeUnit.*", // to use time units easier
            "java.io.File",
            "mu.KLogger",
            "mu.KotlinLogging"
        ).map { "import $it" }

    private fun Any.parameter(): String = when (this) {
        is String -> "\"${this}\""
        is List<*> -> when (this.firstOrNull()) {
            is String -> this.joinToString(",", prefix = "listOf(", postfix = ")") { "\"$it\"" }
            null -> "emptyList()"
            else -> this.joinToString(",", prefix = "listOf(", postfix = ")")
        }

        else -> "$this"
    }

    private val evaluator = runMode.clazz.simpleName + "(" + runOptions.map { "${it.key} = ${it.value.parameter()}" }
        .joinToString(", ") + ")"

    private val startCountDown = CountDownLatch(1)

    fun start(): ScriptRunner {
        log.debug { "Starting Script Runner runMode=$runMode, runOptions=$runOptions, sampleRate=$sampleRate, content:\n$content" }
        check(task == null) { "Task is already started" }
        // extract imports from content
        val importsRegex = Regex("import\\s+[\\w.* ]+;?")
        val customImports = importsRegex.findAll(content)
            .map { it.groupValues.first().removeSuffix(";") }
            .toList()
        val cleanedContent = content.replace(importsRegex, "")

        val additionalClassesDir = createTempDir("wavebeans-cli", "").also { it.deleteOnExit() }

        val scriptContent = """package io.wavebeans.script

${(imports + customImports).joinToString(separator = "\n")}

val log: KLogger = KotlinLogging.logger("ScriptRunner")

val evaluator: ScriptEvaluator = $evaluator

val additionalClassesDir: File = File("${additionalClassesDir.absolutePath}")

fun StreamOutput<*>.out() { evaluator.addOutput(this) }

$cleanedContent
    
try {
    log.info { "Script evaluation started" }
    if (evaluator is ${SupportClassUploading::class.jvmName})
        evaluator.setAdditionalClassesDir(additionalClassesDir)
    evaluator.eval(${sampleRate}f)
        .map { it.get() }
        .also {
            it.mapNotNull { it.exception }
                    .map { log.error(it) { "Error during evaluation" }; it }
                    .firstOrNull()?.let { throw it }
        }
        .all { it.finished }
} catch (e : java.lang.InterruptedException) {
    log.info { "Script evaluation interrupted" }
} catch (e : Exception) {
    log.error(e) { "Script evaluation failed" } 
    e.printStackTrace(System.err)
    evaluator.close()
    System.exit(1)
} finally {
    evaluator.close() 
    log.info { "Evaluator closed" }
}

Unit
"""

        log.debug { "Evaluating the following script: \n$scriptContent" }
        val compileTask = executor.submit(Callable {
            compile(scriptContent)
        })
        val compileResult = try {
            compileTask.get(30000, TimeUnit.MILLISECONDS)!!
        } catch (e: ExecutionException) {
            log.error(e) {
                val split = scriptContent.split("\n")
                val maxNumberLength = log10(split.size.toDouble()).toInt() + 1
                "Can't compile the script:\n" +
                        split.mapIndexed { i, row ->
                            (i + 1).toString().padStart(maxNumberLength, '0') + " " + row
                        }.joinToString("\n")
            }
            throw IllegalStateException("Can't compile the script: ${e.message}", e)
        } catch (e: TimeoutException) {
            log.error(e) { "Compilation took more than 30 sec:\n$scriptContent" }
            throw IllegalStateException("Compilation took more than 30 sec", e)
        }

        (((compileResult.data as LinkedSnippet<*>).get() as KJvmCompiledScript).getCompiledModule() as KJvmCompiledModuleInMemoryImpl)
            .compilerOutputFiles.forEach { (className, byteArray) ->
                val outputFile = File(additionalClassesDir, className)
                outputFile.parentFile.mkdirs()
                outputFile.writeBytes(byteArray)
            }


        task = executor.submit(Callable {
            return@Callable try {
                startCountDown.countDown()
                run(compileResult)
                null
            } catch (e: Throwable) {
                if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
                log.error(e) { "Error while evaluating the following script: \n$scriptContent" }
                e
            }
        })

        check(startCountDown.await(10000, TimeUnit.MILLISECONDS)) { "The task is not started" }

        return this
    }

    private fun compile(script: String): ReplCompileResult.CompiledClasses {
        val scriptCompilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                compilerOptions.append(
                    "-jvm-target", "11"
                )
            }
            dependencies.append(JvmDependency(
                System.getProperty("java.class.path").split(":").map { File(it) }
            ))
        }
        val compiler = JvmReplCompiler(scriptCompilationConfiguration)
        val compilerState = compiler.createState()

        return when (val compileResult = compiler.compile(compilerState, ReplCodeLine(0, scriptGeneration++, script))) {
            is ReplCompileResult.CompiledClasses -> compileResult
            is ReplCompileResult.Incomplete -> throw IllegalStateException("[Compilation Incomplete] $compileResult")
            is ReplCompileResult.Error -> throw IllegalStateException("[Compilation Error] ${compileResult.location ?: "UNKNOWN"}: ${compileResult.message}")
        }

    }

    private fun run(compileResult: ReplCompileResult.CompiledClasses) {
        val configuration = ScriptEvaluationConfiguration { }
        val evaluator = JvmReplEvaluator(configuration)
        val evaluatorState = evaluator.createState()
        when (val result = evaluator.eval(evaluatorState, compileResult)) {
            is ReplEvalResult.Error.Runtime -> {
                throw java.lang.IllegalStateException("message: ${result.message} cause:${result.cause}")
            }

            is ReplEvalResult.UnitResult -> {
                // nothing to do
            }

            else -> throw UnsupportedOperationException("EvalResult $result is not supported")

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