package io.wavebeans.lib.table

expect class ConcurrentHashMap<K, V>() : MutableMap<K, V> {
    fun putIfAbsent(k: K, v: V): V?
}