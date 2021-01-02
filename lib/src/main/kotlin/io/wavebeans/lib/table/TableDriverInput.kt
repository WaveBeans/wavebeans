package io.wavebeans.lib.table

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SinglePartitionBean
import io.wavebeans.lib.SourceBean
import kotlinx.serialization.Serializable

@Serializable
class TableDriverStreamParams(
        val tableName: String,
        val query: TableQuery
) : BeanParams()

class TableDriverInput<T : Any>(
        override val parameters: TableDriverStreamParams
) : BeanStream<T>, SourceBean<T>, SinglePartitionBean {

    override val desiredSampleRate: Float? = null

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return TableRegistry.default.query<T>(parameters.tableName, parameters.query)
    }
}