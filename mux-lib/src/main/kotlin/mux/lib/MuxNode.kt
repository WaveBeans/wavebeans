@file:Suppress("UNCHECKED_CAST")

package mux.lib

import kotlinx.serialization.Serializable

interface MuxNode<T : Any, S : Any> {

    fun inputs(): List<MuxNode<*, *>>

    fun decorateInputs(inputs: List<MuxNode<*, *>>): MuxNode<*, *>

    val parameters: MuxParams

    val type: String
        get() = this::class.simpleName!!

}

interface SourceMuxNode<T : Any, S : Any> : MuxNode<T, S> {

    override fun inputs(): List<MuxNode<*, *>> = emptyList()

    override fun decorateInputs(inputs: List<MuxNode<*, *>>): MuxNode<*, *> = throw UnsupportedOperationException("It has not inputs to decorate")
}

interface SingleMuxNode<T : Any, S : Any> : MuxNode<T, S> {

    val input: MuxNode<T, S>

    fun decorate(input: MuxNode<T, S>): MuxNode<T, S> = TODO()

    override fun inputs(): List<MuxNode<*, *>> = listOf(input)

    override fun decorateInputs(inputs: List<MuxNode<*, *>>): MuxNode<*, *> = decorate(inputs.first() as MuxNode<T, S>)
}

interface AlterMuxNode<IT : Any, IS : Any, OT : Any, OS : Any> : MuxNode<OT, OS> {

    val input: MuxNode<IT, IS>

    fun decorate(input: MuxNode<IT, IS>): MuxNode<OT, OS> = TODO()

    override fun inputs(): List<MuxNode<*, *>> = listOf(input)

    override fun decorateInputs(inputs: List<MuxNode<*, *>>): MuxNode<*, *> = decorate(inputs.first() as MuxNode<IT, IS>)
}

interface MultiMuxNode<T : Any, S : Any> : MuxNode<T, S> {

    val inputs: List<MuxNode<T, S>>

    fun decorate(inputs: List<MuxNode<T, S>>): MuxNode<T, S> = TODO()

    override fun inputs(): List<MuxNode<*, *>> = inputs

    override fun decorateInputs(inputs: List<MuxNode<*, *>>): MuxNode<*, *> = decorate(inputs as List<MuxNode<T, S>>)
}

@Serializable
open class MuxParams

@Serializable
class NoParams : MuxParams()