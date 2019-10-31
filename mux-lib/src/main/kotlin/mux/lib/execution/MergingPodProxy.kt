package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class MergingPodProxy<T : Any, S : Any> : BeanStream<T, S>, PodProxy<T, S> {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val pointedTo: PodKey
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val partition: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun inputs(): List<Bean<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}