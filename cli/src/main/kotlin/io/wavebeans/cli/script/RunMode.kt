package io.wavebeans.cli.script

enum class RunMode(val id: String) {
    LOCAL("local"),
    LOCAL_DISTRIBUTED("local-distributed")
    ;

    companion object {
        fun byId(id: String): RunMode? = values().firstOrNull { it.id == id }
    }
}