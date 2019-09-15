package mux.lib.execution

import mux.lib.MuxNode
import mux.lib.io.StreamOutput

data class MuxTuple(
        val base: StreamOutput<*,*>,
        val nodes: List<MuxNode<*,*>>
)