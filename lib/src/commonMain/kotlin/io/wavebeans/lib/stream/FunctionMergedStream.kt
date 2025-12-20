package io.wavebeans.lib.stream

import io.wavebeans.lib.*

fun <T1 : Any, T2 : Any, R : Any> BeanStream<T1>.merge(
    with: BeanStream<T2>,
    merge: ExecutionScope.(T1?, T2?) -> R?
): BeanStream<R> =
    FunctionMergedStream(this, with, FunctionMergedStreamParams(EmptyScope) { (a, b) -> merge(a, b) })

fun <T1 : Any, T2 : Any, R : Any> BeanStream<T1>.merge(
    with: BeanStream<T2>,
    scope: ExecutionScope,
    merge: ExecutionScope.(T1?, T2?) -> R?
): BeanStream<R> =
    FunctionMergedStream(this, with, FunctionMergedStreamParams(scope) { (a, b) -> merge(a, b) })

class FunctionMergedStreamParams<T1 : Any, T2 : Any, R : Any>(
    val scope: ExecutionScope,
    val merge: ExecutionScope.(Pair<T1?, T2?>) -> R?
) : BeanParams

@Suppress("UNCHECKED_CAST")
class FunctionMergedStream<T1 : Any, T2 : Any, R : Any>(
    sourceStream: BeanStream<T1>,
    mergeStream: BeanStream<T2>,
    override val parameters: FunctionMergedStreamParams<T1, T2, R>
) : AbstractMultiOperationBeanStream<R>(listOf(sourceStream, mergeStream) as List<BeanStream<Any>>), MultiAlterBean<R>,
    SinglePartitionBean {

    override val inputs: List<AnyBean> by lazy { listOf(sourceStream, mergeStream) }

    override fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<R> {
        val sourceIterator = inputs.first().iterator()
        val mergeIterator = inputs.drop(1).first().iterator()
        return object : Iterator<R> {

            var nextEl: R? = null

            override fun hasNext(): Boolean {
                return if (isNextElAvailable()) {
                    advance()
                    nextEl != null
                } else {
                    false
                }
            }

            override fun next(): R {
                if (!isNextElAvailable()) throw NoSuchElementException("No more elements to read")
                advance()
                val el = nextEl ?: throw NoSuchElementException("No more elements to read")
                nextEl = null
                return el
            }

            private fun isNextElAvailable() = nextEl != null || sourceIterator.hasNext() || mergeIterator.hasNext()

            private fun advance() {
                if (nextEl == null) {
                    val s = if (sourceIterator.hasNext()) sourceIterator.next() else null
                    val m = if (mergeIterator.hasNext()) mergeIterator.next() else null
                    nextEl = parameters.merge.invoke(parameters.scope, Pair(s as T1?, m as T2?))
                }
            }
        }.asSequence()
    }
}

