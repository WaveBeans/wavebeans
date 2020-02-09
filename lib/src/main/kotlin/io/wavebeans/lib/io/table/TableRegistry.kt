package io.wavebeans.lib.io.table

import java.util.concurrent.ConcurrentHashMap

interface TableRegistry {
    companion object {
        fun instance(): TableRegistry = LocalTableRegistry
    }

    fun register(tableName: String, timeseriesTableDriver: TimeseriesTableDriver<*>)

    fun unregister(tableName: String)

    fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T>

}

object LocalTableRegistry : TableRegistry {

    private val registry = ConcurrentHashMap<String, TimeseriesTableDriver<*>>()

    override fun register(tableName: String, timeseriesTableDriver: TimeseriesTableDriver<*>) {
        registry.put(tableName, timeseriesTableDriver)
                ?.let { throw IllegalStateException("Can't register table `$tableName`, already registered $it") }
    }

    override fun unregister(tableName: String) {
        registry.remove(tableName)
    }

    override fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T> {
        @Suppress("UNCHECKED_CAST")
        return registry[tableName] as TimeseriesTableDriver<T>
    }

}