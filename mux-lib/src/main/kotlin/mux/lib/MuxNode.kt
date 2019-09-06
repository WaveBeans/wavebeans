package mux.lib

data class Mux(
        val desc: String
)

abstract class MuxNode(
        val mux: Mux
) {

    override fun toString(): String {
        return "$mux"
    }
}

class MuxInputNode(mux: Mux) : MuxNode(mux) {

    override fun toString(): String {
        return "MuxInputNode ${super.toString()}"
    }
}

class MuxSingleInputNode(mux: Mux, val input: MuxNode) : MuxNode(mux) {

    override fun toString(): String {
        return "MuxSingleInputNode[${super.toString()}]\n(input=$input) "
    }
}

class MuxMultiInputNode(mux: Mux, val inputs: List<MuxNode>) : MuxNode(mux) {

    override fun toString(): String {
        return "MuxMultiInputNode[${super.toString()}]\n(inputs=${inputs.joinToString("\n")}) "
    }
}
