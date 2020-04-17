@file:Suppress("UNCHECKED_CAST")

package io.wavebeans.lib

import kotlinx.serialization.Serializable

typealias AnyBean = Bean<*>

interface Bean<T : Any> {

    fun inputs(): List<AnyBean>

    val parameters: BeanParams

    val type: String
        get() = this::class.simpleName!!

}

interface SourceBean<T : Any> : Bean<T> {

    override fun inputs(): List<AnyBean> = emptyList()
}

interface SingleBean<T : Any> : Bean<T> {

    val input: Bean<T>

    override fun inputs(): List<AnyBean> = listOf(input)
}

interface AlterBean<IT : Any, OT : Any> : Bean<OT> {

    val input: Bean<IT>

    override fun inputs(): List<AnyBean> = listOf(input)
}

interface MultiAlterBean<OT : Any> : Bean<OT> {

    val inputs: List<AnyBean>

    override fun inputs(): List<AnyBean> = inputs
}

interface MultiBean<T : Any> : Bean<T> {

    val inputs: List<Bean<T>>

    override fun inputs(): List<AnyBean> = inputs
}

interface SinkBean<T : Any> : SingleBean<T>

interface SinglePartitionBean

@Serializable
open class BeanParams

@Serializable
class NoParams : BeanParams() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}