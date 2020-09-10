package io.wavebeans.tests

import io.wavebeans.communicator.FacilitatorApiClient
import java.io.File

fun startFacilitator(port: Int, threadsNumber: Int = 1, customLoggingConfig: String? = null) {
    val confFile = File.createTempFile("facilitator-config", ".conf").also { it.deleteOnExit() }
    confFile.writeText("""
        facilitatorConfig {
            communicatorPort: $port
            threadsNumber: $threadsNumber
        }
    """.trimIndent())

    val loggingFile = customLoggingConfig?.let {
        val logFile = File.createTempFile("log-config", ".xml").also { it.deleteOnExit() }
        logFile.writeText(customLoggingConfig)
        logFile.absolutePath
    }

    val runner = CommandRunner(
            javaCmd(),
            *((
                    loggingFile?.let { listOf("-Dlogback.configurationFile=$it") } ?: emptyList()) + listOf(
                    "-cp", System.getProperty("java.class.path"),
                    "io.wavebeans.execution.distributed.FacilitatorCliKt", confFile.absolutePath
            )).toTypedArray()
    )
    val runCall = runner.run()

    if (runCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the script. \n${String(runCall.output)}")
    }
}

fun waitForFacilitatorToStart(location: String) {
    FacilitatorApiClient(location).use { facilitatorApiClient ->
        while (true) {
            if (facilitatorApiClient.status())
                break
            // continue trying
            Thread.sleep(1)
        }
    }
}

fun terminateFacilitator(location: String) {
    FacilitatorApiClient(location).use { facilitatorApiClient ->
        facilitatorApiClient.terminate()
    }
}