package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

class DeferredCoroutine<T>(
    context: CoroutineContext
) : AbstractCoroutine<T>(context), Deferred<T> {
    override suspend fun await(): T {
        @Suppress("UNCHECKED_CAST")
        return when(val currentState = state.get()) {
            is Cancelling,
            is InComplete -> awaitSuspend()
            is Complete<*> -> (currentState.value as T?) ?: throw currentState.exception!!
        }
    }

    private suspend fun awaitSuspend() = suspendCoroutine<T> { continuation ->
        doOnCompleted { result ->
            continuation.resumeWith(result)
        }
    }
}