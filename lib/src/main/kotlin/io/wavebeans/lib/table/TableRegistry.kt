package io.wavebeans.lib.table

import java.util.concurrent.ConcurrentHashMap

interface TableRegistry {
    companion object {
        val default = TableRegistryImpl()
    }

    fun register(tableName: String, timeseriesTableDriver: TimeseriesTableDriver<*>)

    fun unregister(tableName: String): TimeseriesTableDriver<*>?

    fun reset(tableName: String)

    fun exists(tableName: String): Boolean

    fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T>

    fun <T : Any> query(tableName: String, query: TableQuery): Sequence<T>

}

class TableRegistryImpl : TableRegistry {

    private val registry = ConcurrentHashMap<String, TimeseriesTableDriver<*>>()

    override fun register(tableName: String, timeseriesTableDriver: TimeseriesTableDriver<*>) {
        registry.put(tableName, timeseriesTableDriver)
                ?.let { throw IllegalStateException("Can't register table `$tableName`, already registered $it") }
    }

    override fun unregister(tableName: String): TimeseriesTableDriver<*>? = registry.remove(tableName)

    override fun reset(tableName: String) {
        registry[tableName]?.reset()
    }

    override fun exists(tableName: String): Boolean = registry.containsKey(tableName)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> byName(tableName: String): TimeseriesTableDriver<T> =
            registry[tableName] as TimeseriesTableDriver<T>?
                    ?: throw IllegalArgumentException("$tableName is not registered")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> query(tableName: String, query: TableQuery): Sequence<T> = byName<T>(tableName).query(query)


}