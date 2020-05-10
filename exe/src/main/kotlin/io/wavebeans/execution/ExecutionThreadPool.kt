package io.wavebeans.execution

import java.util.concurrent.*

interface ExecutionThreadPool : ScheduledExecutorService {
}

abstract class AbstractExecutionThreadPool : ExecutionThreadPool {

    protected abstract val workingPool: ScheduledExecutorService

    override fun shutdown() {
        workingPool.shutdown()
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
        return workingPool.submit(task)
    }

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
        return workingPool.submit(task, result)
    }

    override fun submit(task: Runnable): Future<*> {
        return workingPool.submit(task)
    }

    override fun shutdownNow(): MutableList<Runnable> {
        return workingPool.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return workingPool.isShutdown
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return workingPool.awaitTermination(timeout, unit)
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return workingPool.invokeAny(tasks)
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        return workingPool.invokeAny(tasks, timeout, unit)
    }

    override fun isTerminated(): Boolean {
        return workingPool.isTerminated
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return workingPool.invokeAll(tasks)
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): MutableList<Future<T>> {
        return workingPool.invokeAll(tasks, timeout, unit)
    }

    override fun execute(command: Runnable) {
        workingPool.execute(command)
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return workingPool.schedule(command, delay, unit)
    }

    override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> {
        return workingPool.schedule(callable, delay, unit)
    }

    override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduleAtFixedRate(command, initialDelay, period, unit)
    }

    override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduleWithFixedDelay(command, initialDelay, delay, unit)
    }
}

class NamedThreadFactory(val name: String) : ThreadFactory {
    private var c = 0
    override fun newThread(r: Runnable): Thread {
        return Thread(ThreadGroup(name), r, "$name-${c++}")
    }
}

class MultiThreadedExecutionThreadPool(threadsNum: Int) : AbstractExecutionThreadPool() {

    override val workingPool = Executors.newScheduledThreadPool(threadsNum, NamedThreadFactory("work"))

}