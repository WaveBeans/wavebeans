package io.wavebeans.lib.table

import java.util.concurrent.ConcurrentHashMap

interface TableRegistry {
    companion object {
        fun instance(): TableRegistry = LocalTableRegistry
    }

    fun register(tableName: String, timeseriesTableDriver: TimeseriesTableDriver<*>)

    fun unregister(tableName: String)

    fun reset(tableName: String)

    fun exists(tableName: String): Boolean

    fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T>

    fun <T : Any> query(tableName: String, query: TableQuery): Sequence<T>

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

    override fun reset(tableName: String) {
        registry[tableName]?.reset()
    }

    override fun exists(tableName: String): Boolean =
            registry.containsKey(tableName)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T> =
            registry[tableName] as TimeseriesTableDriver<T>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> query(tableName: String, query: TableQuery): Sequence<T> =
            (registry[tableName] as TimeseriesTableDriver<T>).query(query)


}