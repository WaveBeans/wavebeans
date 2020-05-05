package io.wavebeans.execution.distributed

import assertk.assertThat
import assertk.assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

object FacilitatorCliSpec : Spek({
    describe("Getting help") {
        val ostream = ByteArrayOutputStream()
        val cli = FacilitatorCli(PrintStream(ostream), arrayOf("--help"))

        it("should return 0 exit code") { assertThat(cli.call()).isEqualTo(0) }
        it("should have specific output") {
            assertThat(String(ostream.toByteArray())).isEqualTo(
                    "The following config attributes of facilitatorConfig are supported- advertisingHostAddress: [simple type, class java.lang.String] <optional>. The name the Facilitator is advertised under. Default value: 127.0.0.1\n" +
                            "- listeningPortRange: [simple type, class kotlin.ranges.IntRange] <required>. The range facilitator will choose the port randomly from. Whichever won't be occupied. Default value: N/A\n" +
                            "- startingUpAttemptsCount: [simple type, class java.lang.Integer] <optional>. The number of attempts the facilitator will try to make to bind to port. Default value: 10\n" +
                            "- threadsNumber: [simple type, class java.lang.Integer] <required>. The capacity of working pool for this facilitator. It is going to be shared across all jobs. Default value: N/A\n" +
                            "- callTimeoutMillis: [simple type, class java.lang.Long] <optional>. The maximum time the facilitator will wait for the answer from the pod, in milliseconds. Default value: 5000\n" +
                            "- onServerShutdownGracePeriodMillis: [simple type, class java.lang.Long] <optional>. The time to allow for the HTTP server to gracefully shutdown all resources. Default value: 5000\n" +
                            "- onServerShutdownTimeoutMillis: [simple type, class java.lang.Long] <optional>. The time to wait before killing the HTTP server even if it doesn't confirm that. Default value: 5000\n"
            )
        }
    }

    describe("Getting version") {
        val ostream = ByteArrayOutputStream()
        val cli = FacilitatorCli(PrintStream(ostream), arrayOf("--version"))

        it("should return 0 exit code") { assertThat(cli.call()).isEqualTo(0) }
        it("should have specific output") {
            assertThat(String(ostream.toByteArray())).isEqualTo("Version TEST-VERSION-123\n")
        }
    }

    describe("Starting up") {
        describe("No config file provided") {
            val ostream = ByteArrayOutputStream()
            val cli = FacilitatorCli(PrintStream(ostream), emptyArray())

            it("should return 1 exit code") {
                assertThat(cli.call()).isEqualTo(1)
            }
            it("should have specific output") {
                assertThat(String(ostream.toByteArray())).isEqualTo("Specify configuration file as a parameter or --help " +
                        "to see configuration options or --version to check the version.\n")
            }
        }

        describe("Invalid config file provided") {
            val ostream = ByteArrayOutputStream()
            val confFile = File.createTempFile("facilitator", "conf").also {
                it.writeText("""
                invalidConfig {
                    no values
                }
            """.trimIndent())
            }
            val cli = FacilitatorCli(PrintStream(ostream), arrayOf(confFile.absolutePath))

            it("should return 1 exit code") {
                assertThat(cli.call()).isEqualTo(1)
            }
            it("should have specific output") {
                assertThat(String(ostream.toByteArray())).startsWith("Can't parse the config file:")
            }
        }

        describe("Valid config file provided") {
            val ostream = ByteArrayOutputStream()
            val confFile = File.createTempFile("facilitator", "conf").also {
                it.writeText("""
                facilitatorConfig {
                    listeningPortRange: {start: 40000, end: 50000}
                    threadsNumber: 1
                    onServerShutdownGracePeriodMillis: 100
                    onServerShutdownTimeoutMillis: 100
                }
            """.trimIndent())
            }
            val cli = FacilitatorCli(PrintStream(ostream), arrayOf(confFile.absolutePath))
            val pool = Executors.newCachedThreadPool()

            lateinit var future: Future<Int>
            it("should start the cli") {
                future = pool.submit(cli)
                cli.awaitStarted()
                assertThat(future.isDone).isFalse()
            }
            it("should stop the cli") {
                cli.stop() // it is in shutdown hook
                assertThat(future.get()).isEqualTo(0)
            }
            it("should have specific output") {
                assertThat(String(ostream.toByteArray())).matches("Listening on port [\\d]+\n".toRegex())
            }
        }
    }
})