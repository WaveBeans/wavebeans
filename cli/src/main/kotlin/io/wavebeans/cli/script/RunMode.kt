package io.wavebeans.cli.script

enum class RunMode(name: String) {
    LOCAL("local"),
    LOCAL_DISTRIBUTED("local-distributed")
    ;

    companion object {
        fun byName(name: String): RunMode? = values().firstOrNull { it.name == name }
    }
}