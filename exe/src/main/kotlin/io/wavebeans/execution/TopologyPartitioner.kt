package io.wavebeans.execution

import io.wavebeans.lib.Bean
import io.wavebeans.lib.SinkBean
import io.wavebeans.lib.SinglePartitionBean
import kotlin.reflect.full.isSubclassOf

fun Topology.partition(partitionsCount: Int): Topology {
    check(partitionsCount > 0) { "Partitions count should be > 0" }
    val beanRefs = mutableListOf<BeanRef>()
    val links = mutableListOf<BeanLink>()
    val replacedBeans = mutableMapOf<BeanRef, List<BeanRef>>()
    val handledBeans = mutableSetOf<BeanRef>()


    fun handleBean(beanRef: BeanRef, level: Int = 1) {
        val beanClazz = Class.forName(beanRef.type).kotlin
        val linkedBeans = this.links
                .filter { it.from == beanRef.id }
                .map { l -> this.refs.first { it.id == l.to } }
        if (linkedBeans.isEmpty() || handledBeans.contains(beanRef)) {
            return
        }
        handledBeans += beanRef

        for (linkedBeanRef in linkedBeans) {
            val linkedBeanClazz = Class.forName(linkedBeanRef.type).kotlin
            val (newLinks, newBeans) = when {
                linkedBeanClazz.isSubclassOf(SinglePartitionBean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val partitionedBeans = replacedBeans.getValue(beanRef)
                    val newBeans = replacedBeans[linkedBeanRef] ?: listOf(linkedBeanRef)
                    val newLinks = partitionedBeans
                            .map {
                                val bean = newBeans.first()
                                val currentLink = this.links.first { l -> it.id == l.from && bean.id == l.to }
                                BeanLink(
                                        from = it.id,
                                        fromPartition = it.partition,
                                        to = bean.id,
                                        toPartition = 0,
                                        order = currentLink.order
                                )
                            }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(SinglePartitionBean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount).map { linkedBeanRef.copy(partition = it) }
                    val newLinks = newBeans
                            .map {
                                val currentLink = this.links.first { l -> beanRef.id == l.from && it.id == l.to }
                                BeanLink(
                                        from = beanRef.id,
                                        fromPartition = 0,
                                        to = it.id,
                                        toPartition = it.partition,
                                        order = currentLink.order
                                )
                            }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount).map { linkedBeanRef.copy(partition = it) }
                    val newLinks = newBeans
                            .map { newBean ->
                                val currentLink = this.links.first { l -> beanRef.id == l.from && newBean.id == l.to }
                                BeanLink(
                                        from = beanRef.id,
                                        fromPartition = newBean.partition,
                                        to = newBean.id,
                                        toPartition = newBean.partition,
                                        order = currentLink.order
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
                beanRefs += it
                replacedBeans[it] = listOf(it)
                handleBean(it)
            }


    return Topology(beanRefs, links, partitionsCount)
}
