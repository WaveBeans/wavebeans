package io.wavebeans.tests

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.trim
import io.wavebeans.metrics.collector.collector
import io.wavebeans.metrics.samplesProcessedOnOutputMetric
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.Executors

object DistributedMetricCollectionSpec : Spek({

    val facilitatorPorts = listOf(40001, 40002)

    val log = KotlinLogging.logger {}
    val facilitatorsLocations = facilitatorPorts.map { "127.0.0.1:$it" }

    val pool by memoized(CachingMode.SCOPE) { Executors.newCachedThreadPool() }

    beforeGroup {
        facilitatorPorts.forEach {
            pool.submit {
                startFacilitator(it, customLoggingConfig = """
                <configuration debug="false">

                    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                        <!-- encoders are  by default assigned the type
                             ch.qos.logback.classic.encoder.PatternLayoutEncoder -->
                        <encoder>
                            <pattern>[[[$it]]] %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                        </encoder>
                    </appender>

                    <logger name="io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler" level="INFO" />
                    <logger name="io.grpc.netty.shaded.io.grpc.netty.NettyServerHandler" level="INFO" />

                    <root level="INFO">
                        <appender-ref ref="STDOUT" />
                    </root>
                </configuration>
            """.trimIndent())
            }
        }

        facilitatorsLocations.forEach(::waitForFacilitatorToStart)
    }

    afterGroup {
        try {
            facilitatorsLocations.forEach(::terminateFacilitator)
        } finally {
            pool.shutdownNow()
        }
    }

    describe("Monitoring in distributed environment") {

        it("should collect processed samples count") {
            val collector = samplesProcessedOnOutputMetric.collector(
                    facilitatorsLocations,
                    refreshIntervalMs = 10,
                    granularValueInMs = 100
            )

            val outputs = listOf(440.sine().trim(1000).toDevNull())

            val overseer = DistributedOverseer(
                    outputs,
                    facilitatorsLocations,
                    emptyList(),
                    1
            )

            val exceptions = overseer.eval(44100.0f)
                    .mapNotNull { it.get().exception }

            overseer.close()
            assertThat(exceptions).isEmpty()

            assertThat(collector.collectValues(Long.MAX_VALUE).sumBy { it.value.toInt() }).isEqualTo(44100)
            collector.close()
        }
    }
})