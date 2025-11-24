package io.wavebeans.tests

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import assertk.assertions.prop
import io.kotest.core.spec.style.DescribeSpec

class EnvironmentSpec : DescribeSpec({

    describe("Kotlin compiler installed") {
        it("should be 2.x.x") {
            val cmd = CommandRunner(
                kotlincCmd(),
                "-version"
            ).run(inheritIO = false)

            assertThat(cmd).all {
                prop(CommandResult::exitCode).isEqualTo(0)
                prop(CommandResult::output)
                    .transform { String(it) }
                    .matches(".*kotlinc-jvm 2\\.\\d+\\.\\d+.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            }
        }
    }

    describe("JDK installed") {
        it("should be 11 version") {
            val cmd = CommandRunner(
                javaCmd(),
                "-version"
            ).run(inheritIO = false)

            assertThat(cmd).all {
                prop(CommandResult::exitCode).isEqualTo(0)
                prop(CommandResult::output)
                    .transform { String(it) }
                    .matches(".*jdk.*11\\.\\d+\\.\\d+.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            }
        }
    }
})