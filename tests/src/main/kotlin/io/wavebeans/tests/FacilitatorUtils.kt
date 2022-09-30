package io.wavebeans.tests

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.wavebeans.communicator.FacilitatorApiClient
import mu.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger { }

fun startFacilitator(
        port: Int,
        threadsNumber: Int = 1,
        facilitatorLogLevel: String = "INFO"
) {
    log.info { "Starting facilitator on port=$port, threadsNumber=$threadsNumber, facilitatorLogLevel=$facilitatorLogLevel" }
    val customLoggingConfig = """
                    <configuration debug="false">

                    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                        <encoder>
                            <pattern>[[[$port]]] %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                        </encoder>
                    </appender>

                    <logger name="io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler" level="INFO" />
                    <logger name="io.grpc.netty.shaded.io.grpc.netty.NettyServerHandler" level="INFO" />

                    <root level="$facilitatorLogLevel">
                        <appender-ref ref="STDOUT" />
                    </root>
                </configuration>
            """.trimIndent()
    val confFile = File.createTempFile("facilitator-config", ".conf").also { it.deleteOnExit() }
    confFile.writeText("""
        facilitatorConfig {
            communicatorPort: $port
            threadsNumber: $threadsNumber
        }
    """.trimIndent())

    val loggingFile = customLoggingConfig.let {
        val logFile = File.createTempFile("log-config", ".xml").also { it.deleteOnExit() }
        logFile.writeText(customLoggingConfig)
        logFile.absolutePath
    }

    val runner = CommandRunner(
            javaCmd(),
            *(listOf(
                    "-Dlogback.configurationFile=$loggingFile",
                    "-cp", System.getProperty("java.class.path"),
                    "io.wavebeans.execution.distributed.FacilitatorCliKt", confFile.absolutePath
            )).toTypedArray()
    )
    val runCall = runner.run()

    if (runCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the script. \n${String(runCall.output)}")
    }
}

fun waitForFacilitatorToStart(location: String, timeoutMs: Int = 30000) {
    log.info { "Waiting for facilitator to start on location $location." }
    FacilitatorApiClient(location).use { facilitatorApiClient ->
        val startedAt = System.currentTimeMillis()
        while (true) {
            if (facilitatorApiClient.status())
                break
            if (System.currentTimeMillis() - startedAt > timeoutMs)
                throw IllegalStateException("Facilitator at $location start timed out")
            // continue trying
            Thread.sleep(1)
        }
    }
    log.info { "Facilitator on location $location started." }
}

fun terminateFacilitator(location: String, timeoutMs: Int = 30000) {
    log.info { "Terminating facilitator on location $location" }
    FacilitatorApiClient(location).use { facilitatorApiClient ->
        try {
            facilitatorApiClient.terminate()
            val startedAt = System.currentTimeMillis()
            while (true) {
                if (!facilitatorApiClient.status())
                    break
                if (System.currentTimeMillis() - startedAt > timeoutMs)
                    throw IllegalStateException("Facilitator at $location can stop within timeout")
                // continue trying
                Thread.sleep(1)
            }
        } catch (e: Exception) {
            fun isUnavailable(e: Throwable?) =
                e != null &&
                        e is StatusRuntimeException &&
                        e.status.code == Status.UNAVAILABLE.code
            if (!isUnavailable(e)
                    && !isUnavailable(e.cause)
                    && !isUnavailable(e.cause?.cause)
                    && !isUnavailable(e.cause?.cause?.cause)
            ) {
                throw e
            }
        }
    }
    log.info { "Terminated facilitator on location $location" }
}