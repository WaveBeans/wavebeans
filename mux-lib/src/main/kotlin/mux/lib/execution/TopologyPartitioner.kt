package mux.lib.execution

import mux.lib.Bean
import mux.lib.SinkBean
import mux.lib.SinglePartitionBean
import kotlin.reflect.full.isSubclassOf

interface PartitioningIdResolver {
    fun id(node: BeanRef, partition: Int): Int
}

class DefaultPartitioningIdResolver(topology: Topology) : PartitioningIdResolver {

    private var idSeq = topology.refs.maxBy { it.id }?.id ?: 0

    override fun id(node: BeanRef, partition: Int): Int = ++idSeq

}

fun Topology.partition(partitionsCount: Int, idResolver: PartitioningIdResolver = DefaultPartitioningIdResolver(this)): Topology {
    check(partitionsCount > 0) { "Partitions count should be > 0" }
    val beanRefs = mutableListOf<BeanRef>()
    val links = mutableListOf<BeanLink>()
    val replacedBeans = mutableMapOf<BeanRef, List<BeanRef>>()


    fun handleBean(beanRef: BeanRef, level: Int = 1) {
        println(">>>> Level: $level")
        val beanClazz = Class.forName(beanRef.type).kotlin
        println("Bean=$beanRef")
        val linkedBeans = this.links
                .filter { it.from == beanRef.id }
                .map { l -> this.refs.first { it.id == l.to } }
        println("linkedBeans=$linkedBeans")
        if (linkedBeans.isEmpty()) return

        if (linkedBeans.size == 1) {
            val linkedBeanRef = linkedBeans.first()
            val linkedBeanClazz = Class.forName(linkedBeanRef.type).kotlin
            when {
                linkedBeanClazz.isSubclassOf(SinglePartitionBean::class) && beanClazz.isSubclassOf(SinglePartitionBean::class) -> {
                    TODO("Not supported: beanClazz=$beanClazz, linkedBeanClazz=$linkedBeanClazz")
                }
                linkedBeanClazz.isSubclassOf(SinglePartitionBean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val partitionedBeans = replacedBeans.getValue(beanRef)
                    val newBean = linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, 0))
                    replacedBeans[linkedBeanRef] = listOf(newBean)
                    val newLinks = partitionedBeans.map { BeanLink(it.id, newBean.id) }
                    beanRefs += newBean
                    links += newLinks
                    handleBean(newBean, level + 1)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(SinglePartitionBean::class) -> {
                    val newBeans = (0 until partitionsCount)
                            .map { linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, it), partition = it) }
                    val beanRefReplacementId = replacedBeans.getValue(beanRef).first().id
                    val newLinks = newBeans.map { BeanLink(beanRefReplacementId, it.id) }
                    replacedBeans[linkedBeanRef] = newBeans
                    beanRefs += newBeans
                    links += newLinks
                    handleBean(linkedBeanRef, level + 1)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val newBeans = (0 until partitionsCount)
                            .map { linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, it), partition = it) }
                    val partitionedBeans = replacedBeans.getValue(beanRef)
                    val newLinks = newBeans
                            .map { newBean ->
                                BeanLink(
                                        partitionedBeans.first { it.partition == newBean.partition }.id,
                                        newBean.id
                                )
                            }
                    replacedBeans[linkedBeanRef] = newBeans
                    beanRefs += newBeans
                    links += newLinks
                    handleBean(linkedBeanRef, level + 1)
                }
            }
        } else {
            TODO("linkedBeans.size=${linkedBeans.size}")
        }
    }

    this.refs.filter { Class.forName(it.type).kotlin.isSubclassOf(SinkBean::class) }
            .forEach {
                val newSinkBean = it.copy(id = idResolver.id(it, 0))
                beanRefs += newSinkBean
                replacedBeans[it] = listOf(newSinkBean)
                handleBean(it)
            }


    return Topology(beanRefs, links)
}
