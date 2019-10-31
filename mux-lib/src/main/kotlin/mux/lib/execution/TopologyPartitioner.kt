package mux.lib.execution

import mux.lib.Bean
import mux.lib.SinkBean
import mux.lib.SinglePartitionBean
import kotlin.reflect.full.isSubclassOf

fun Topology.partition(partitionsCount: Int): Topology {
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
                    val newBeans = replacedBeans[linkedBeanRef] ?: listOf(linkedBeanRef)
                    val newLinks = partitionedBeans
                            .map {
                                BeanLink(
                                        from = it.id,
                                        fromPartition = it.partition,
                                        to = newBeans.first().id,
                                        toPartition = 0
                                )
                            }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(SinglePartitionBean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount).map { linkedBeanRef.copy(partition = it) }
                    val newLinks = newBeans
                            .map {
                                BeanLink(
                                        from = beanRef.id,
                                        fromPartition = 0,
                                        to = it.id,
                                        toPartition = it.partition
                                )
                            }

                    Pair(newLinks, newBeans)
                }
                linkedBeanClazz.isSubclassOf(Bean::class) && beanClazz.isSubclassOf(Bean::class) -> {
                    val newBeans = replacedBeans[linkedBeanRef]
                            ?: (0 until partitionsCount).map { linkedBeanRef.copy(partition = it) }
                    val newLinks = newBeans
                            .map { newBean ->
                                BeanLink(
                                        from = beanRef.id,
                                        fromPartition = newBean.partition,
                                        to = newBean.id,
                                        toPartition = newBean.partition
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


    return Topology(beanRefs, links)
}
