@file:Suppress("UNCHECKED_CAST")

package mux.lib

import kotlinx.serialization.Serializable

interface Bean<T : Any, S : Any> {

    fun inputs(): List<Bean<*, *>>

    val parameters: BeanParams

    val type: String
        get() = this::class.simpleName!!

}

interface SourceBean<T : Any, S : Any> : Bean<T, S> {

    override fun inputs(): List<Bean<*, *>> = emptyList()
}

interface SingleBean<T : Any, S : Any> : Bean<T, S> {

    val input: Bean<T, S>

    override fun inputs(): List<Bean<*, *>> = listOf(input)
}

interface AlterBean<IT : Any, IS : Any, OT : Any, OS : Any> : Bean<OT, OS> {

    val input: Bean<IT, IS>

    override fun inputs(): List<Bean<*, *>> = listOf(input)
}

interface MultiBean<T : Any, S : Any> : Bean<T, S> {

    val inputs: List<Bean<T, S>>

    override fun inputs(): List<Bean<*, *>> = inputs
}

interface SinkBean<T : Any, S : Any> : SingleBean<T, S>

interface SinglePartitionBean

@Serializable
open class BeanParams

@Serializable
class NoParams : BeanParams()