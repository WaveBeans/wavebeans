package mux.lib.execution

interface TickPod: Pod {

    fun tick(): Boolean
}