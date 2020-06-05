package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.execution.distributed.RemoteTimeseriesTableDriver
import io.wavebeans.lib.table.TableRegistryImpl
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe

object HttpCommunicatorSpec : Spek({
    describe("Registering table") {
        val tableRegistry by memoized(SCOPE) { TableRegistryImpl() }

        val service by memoized(SCOPE) { HttpCommunicatorService(tableRegistry) }

        it("should register remote table driver") {
            val tableName = "myTable"
            service.registerTable(tableName, "127.0.0.1:4000")
            assertThat(tableRegistry).all {
                prop("exists($tableName)") { it.exists(tableName) }.isTrue()
                prop("byName<Any>($tableName)") { it.byName<Any>(tableName) }
                        .isInstanceOf(RemoteTimeseriesTableDriver::class)
                        .prop("facilitatorLocation") { it.facilitatorLocation }.isEqualTo("127.0.0.1:4000")
            }
        }
        it("should unregister remote table driver") {
            val tableName = "myTable"
            service.unregisterTable(tableName)
            assertThat(tableRegistry).all {
                prop("exists($tableName)") { it.exists(tableName) }.isFalse()
            }
        }
    }
})