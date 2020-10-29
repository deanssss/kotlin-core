package xyz.dean.kotlin_core.async.coroutines.imitate

import xyz.dean.kotlin_core.async.coroutines.dispatcher.Dispatcher
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.CoroutineContext

typealias EventTask = () -> Unit

class BlockingQueueDispatcher : LinkedBlockingDeque<EventTask>() ,Dispatcher {
    // 此处传入的block包含着恢复协程执行的resumeWith的调用。
    override fun dispatch(block: () -> Unit) {
        offer(block)
    }
}

class BlockingCoroutine<T>(
    context: CoroutineContext,
    private val eventQueue: LinkedBlockingDeque<EventTask>
) : AbstractCoroutine<T>(context) {
    fun joinBlocking(): T {
        while (!isCompleted) {
            eventQueue.take().invoke()
        }
        @Suppress("UNCHECKED_CAST")
        return (state.get() as Complete<T>).let {
            it.value ?: throw it.exception!!
        }
    }
}
