package xyz.dean.kotlin_core.async.coroutines.dispatcher

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

interface Dispatcher {
    fun dispatch(block: () -> Unit)
}

object DefaultDispatcher : Dispatcher {
    private val threadGroup = ThreadGroup("DefaultDispatcher")
    private val threadIndex = AtomicInteger(0)

    private val executor = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors()) { runnable ->
        Thread(threadGroup, runnable, "${threadGroup.name}-worker-${threadIndex.getAndIncrement()}")
    }

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }
}