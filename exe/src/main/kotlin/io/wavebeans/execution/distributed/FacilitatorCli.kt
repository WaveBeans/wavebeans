package io.wavebeans.execution.distributed

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.hocon
import mu.KotlinLogging
import java.io.PrintStream
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val facilitatorCli = FacilitatorCli(System.out, args)

    Runtime.getRuntime().addShutdownHook(Thread {
        facilitatorCli.stop()
    })

    exitProcess(facilitatorCli.call())
}

class FacilitatorCli(
        private val printWriter: PrintStream,
        private val args: Array<String>
) : Callable<Int> {

    companion object {
        private val log = KotlinLogging.logger { }
        private const val configDescribeOption = "--help"
        private const val printVersionOption = "--version"
    }

    private var facilitator: Facilitator? = null

    private val startLatch = CountDownLatch(1)

    fun awaitStarted() {
        startLatch.await()
    }

    fun stop() {
        facilitator?.terminate()
    }

    override fun call(): Int {
        if (args.isEmpty()) {
            printWriter.println("Specify configuration file as a parameter or $configDescribeOption to see" +
                    " configuration options or $printVersionOption to check the version.")
            printWriter.flush()
            return 1
        }

        lateinit var configFilePath: String
        when (args[0].toLowerCase()) {
            configDescribeOption -> {
                printWriter.println("The following config attributes of facilitatorConfig are supported:\n" + FacilitatorConfig.items.joinToString("\n") {
                    "- ${it.name}: ${it.type} <${if (it.isRequired) "required" else "optional"}>. ${it.description}. " +
                            "Default value: ${if (it.isOptional) it.asOptionalItem.default?.toString() else "N/A"}"
                })
                printWriter.flush()
                return 0
            }
            printVersionOption -> {
                val version = Thread.currentThread().contextClassLoader.getResources(JarFile.MANIFEST_NAME)
                        .asSequence()
                        .mapNotNull {
                            it.openStream().use { stream ->
                                val attributes = Manifest(stream).mainAttributes
                                attributes.getValue("WaveBeans-Version")
                            }
                        }
                        .firstOrNull()
                        ?: "<NOT VERSIONED>"
                printWriter.println("Version $version")
                printWriter.flush()
                return 0

            }
            else -> {
                configFilePath = args[0]
            }
        }


        val config = try {
            Config { addSpec(FacilitatorConfig) }.withSource(Source.from.hocon.file(configFilePath))
        } catch (e: Exception) {
            printWriter.println("Can't parse the config file: " + e.message)
            printWriter.flush()
            return 1
        }

        log.info {
            "Staring Facilitator with following config:n\"" +
                    config.toMap().entries.joinToString("\n") { "${it.key} = ${it.value}" }
        }

        facilitator = Facilitator(
                communicatorPort = config[FacilitatorConfig.communicatorPort],
                threadsNumber = config[FacilitatorConfig.threadsNumber],
                callTimeoutMillis = config[FacilitatorConfig.callTimeoutMillis],
                onServerShutdownTimeoutMillis = config[FacilitatorConfig.onServerShutdownTimeoutMillis]
        )

        facilitator!!.start()

        startLatch.countDown()

        printWriter.println("Started server on ${config[FacilitatorConfig.communicatorPort]}")
        printWriter.flush()

        facilitator!!.waitAndClose()

        return 0
    }
}