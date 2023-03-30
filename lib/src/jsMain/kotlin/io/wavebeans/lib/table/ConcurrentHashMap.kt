package io.wavebeans.lib.table

actual class ConcurrentHashMap<K, V> : MutableMap<K, V> {
    private val map = hashMapOf<K, V>()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries

    override val keys: MutableSet<K>
        get() = map.keys

    override val size: Int
        get() = map.size

    override val values: MutableCollection<V>
        get() = map.values

    override fun clear() {
        map.clear()
    }

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun remove(key: K): V? = map.remove(key)

    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
    }

    override fun put(key: K, value: V): V? = map.put(key, value)

    override fun get(key: K): V? = map[key]

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    actual fun putIfAbsent(k: K, v: V): V? {
        TODO("Not yet implemented")
    }
}