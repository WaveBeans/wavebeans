package mux.lib.file

interface Informable {
    fun info(namespace: String? = null): Map<String, String>
}