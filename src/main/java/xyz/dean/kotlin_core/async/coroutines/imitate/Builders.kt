package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlinx.coroutines.CoroutineName
import xyz.dean.kotlin_core.async.coroutines.dispatcher.DispatcherContext
import xyz.dean.kotlin_core.async.coroutines.dispatcher.Dispatchers
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
): Job {
    val completion = StandardCoroutine(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}

fun <T> async(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T
): Deferred<T> {
    val completion = DeferredCoroutine<T>(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}

private val coroutineIndex = AtomicInteger(0)

fun newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val combined = context + CoroutineName("@coroutine#${coroutineIndex.getAndIncrement()}")
    return if (combined !== Dispatchers.Default
        && combined[ContinuationInterceptor] == null) {
        combined + Dispatchers.Default
    } else {
        combined
    }
}

fun <T> runBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T
): T {
    val eventQueue = BlockingQueueDispatcher()
    val newContext = newCoroutineContext(context + DispatcherContext(eventQueue))
    val completion = BlockingCoroutine<T>(newContext, eventQueue)
    block.startCoroutine(completion)
    return completion.joinBlocking()
}