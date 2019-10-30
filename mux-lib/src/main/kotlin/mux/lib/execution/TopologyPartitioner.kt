package mux.lib.execution

import mux.lib.Bean
import mux.lib.SinkBean
import mux.lib.SinglePartitionBean
import kotlin.reflect.full.isSubclassOf

interface PartitioningIdResolver {
    fun id(beanRef: BeanRef, partition: Int): Int
}

class DefaultPartitioningIdResolver(topology: Topology) : PartitioningIdResolver {

    private var idSeq = topology.refs.maxBy { it.id }?.id ?: 0

    override fun id(beanRef: BeanRef, partition: Int): Int = ++idSeq

}

fun Topology.partition(partitionsCount: Int, idResolver: PartitioningIdResolver = DefaultPartitioningIdResolver(this)): Topology {
    check(partitionsCount > 0) { "Partitions count should be > 0" }
    val beanRefs = mutableListOf<BeanRef>()
    val links = mutableListOf<BeanLink>()
    val replacedBeans = mutableMapOf<BeanRef, List<BeanRef>>()
    val handledBeans = mutableSetOf<BeanRef>()


    fun handleBean(beanRef: BeanRef, level: Int = 1) {
        println(">>>> Level: $level")
        val beanClazz = Class.forName(beanRef.type).kotlin
        println("Bean=$beanRef")
        val linkedBeans = this.links
                .filter { it.from == beanRef.id }
                .map { l -> this.refs.first { it.id == l.to } }
        println("linkedBeans=$linkedBeans")
        println("Current: beanRefs=$beanRefs, links=$links")
        if (linkedBeans.isEmpty() || handledBeans.contains(beanRef)) {
            println("Skipping")
            return
        }
        handledBeans += beanRef

        for (linkedBeanRef in linkedBeans) {
            val linkedBeanClazz = Class.forName(linkedBeanRef.type).kotlin
            val (newLinks, newBeans) = when {
                linkedBeanClazz.isSubclassOf(SinglePartitionBean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val partitionedBeans = replacedBeans.getValue(beanRef)
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: listOf(linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, 0)))
                    val newLinks = partitionedBeans.map { BeanLink(it.id, newBeans.first().id) }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(SinglePartitionBean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount)
                                    .map { linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, it), partition = it) }
                    val beanRefReplacementId = replacedBeans.getValue(beanRef).first().id
                    val newLinks = newBeans.map { BeanLink(beanRefReplacementId, it.id) }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount)
                                    .map { linkedBeanRef.copy(id = idResolver.id(linkedBeanRef, it), partition = it) }
                    val partitionedBeans = replacedBeans.getValue(beanRef)
                    val newLinks = newBeans
                            .map { newBean ->
                                BeanLink(
                                        partitionedBeans.first { it.partition == newBean.partition }.id,
                                        newBean.id
                                )
                            }

                    Pair(newLinks, newBeans)
                }
                else -> throw UnsupportedOperationException("Not supported linkedBeanClazz=$linkedBeanClazz, beanClazz=$beanClazz")
            }

            links += newLinks
            if (linkedBeanRef !in replacedBeans.keys) {
                beanRefs += newBeans
                replacedBeans[linkedBeanRef] = newBeans
                handleBean(linkedBeanRef, level + 1)
            }

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
