package io.wavebeans.lib.table

actual class ConcurrentHashMap<K, V> : MutableMap<K, V> {

    private val map = java.util.concurrent.ConcurrentHashMap<K, V>()

    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries

    actual override val keys: MutableSet<K>
        get() = map.keys

    actual override val size: Int
        get() = map.size

    actual override val values: MutableCollection<V>
        get() = map.values

    actual override fun clear() {
        map.clear()
    }

    actual override fun isEmpty(): Boolean = map.isEmpty()

    actual override fun remove(key: K): V? = map.remove(key)

    actual override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
    }

    actual override fun put(key: K, value: V): V? = map.put(key, value)

    actual override fun get(key: K): V? = map[key]

    actual override fun containsValue(value: V): Boolean = map.containsValue(value)

    actual override fun containsKey(key: K): Boolean = map.containsKey(key)

    actual fun putIfAbsent(k: K, v: V): V? = map.putIfAbsent(k, v)

    override fun toString(): String {
        return map.toString();
    }
}