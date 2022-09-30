package io.wavebeans.tests

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import assertk.assertions.prop
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EnvironmentSpec : Spek({

    describe("Kotlin compiler installed") {
        it("should be 1.7.x") {
            val cmd = CommandRunner(
                kotlincCmd(),
                "-version"
            ).run(inheritIO = false)

            assertThat(cmd).all {
                prop(CommandResult::exitCode).isEqualTo(0)
                prop(CommandResult::output)
                    .transform { String(it) }
                    .matches(".*kotlinc-jvm 1\\.7\\.\\d+.*".toRegex(RegexOption.DOT_MATCHES_ALL))
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