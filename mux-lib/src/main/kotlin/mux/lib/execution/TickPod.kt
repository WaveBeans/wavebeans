package mux.lib.execution

interface TickPod {

    fun tick(): Boolean

    fun terminate()
}