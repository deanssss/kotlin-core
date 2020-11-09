package xyz.dean.kotlin_core.async.coroutines.imitate

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

sealed class CancelState {
    object InComplete : CancelState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CancelState()
    object Cancelled : CancelState()
}

class CancellationContinuation<T>(
    val continuation: Continuation<T>
) : Continuation<T> by continuation {
    private val state = AtomicReference<CancelState>(CancelState.InComplete)
    private val cancelHandlers = CopyOnWriteArrayList<OnCancel>()

    val isCompleted: Boolean
        get() = state.get() is CancelState.Complete<*>
    val isActive: Boolean
        get() = state.get() == CancelState.InComplete

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    continuation.resumeWith(result)
                    CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                is CancelState.Complete<*> -> error("Already completed.")
                CancelState.Cancelled -> {
                    CancellationException("Cancelled.").let {
                        continuation.resumeWith(Result.failure(it))
                        CancelState.Complete(null, it)
                    }
                }
            }
        }
    }

    fun getResult(): Any? {
        installCancelHandler()
        return when (val currentState = state.get()) {
            CancelState.InComplete -> COROUTINE_SUSPENDED
            is CancelState.Complete<*> -> {
                @Suppress("UNCHECKED_CAST")
                (currentState as CancelState.Complete<T>).let { state ->
                    state.exception?.let { throw it } ?: state.value
                }
            }
            CancelState.Cancelled -> throw CancellationException("Continuation is canceled.")
        }
    }

    fun invokeOnCancel(onCancel: OnCancel) {
        cancelHandlers += onCancel
    }

    fun cancel() {
        if (!isActive) return
        val parent = continuation.context[Job] ?: return
        parent.cancel()
    }

    private fun installCancelHandler() {
        if (!isActive) return
        val parent = continuation.context[Job] ?: return
        parent.invokeOnCancel { doCancel() }
    }

    private fun doCancel() {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    CancelState.Cancelled
                }
                is CancelState.Complete<*>,
                CancelState.Cancelled -> {
                    prev
                }
            }
        }
        cancelHandlers.forEach(OnCancel::invoke)
        cancelHandlers.clear()
    }
}

suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellationContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn { c: Continuation<T> ->
    val cancellationContinuation = CancellationContinuation(c.intercepted())
    block(cancellationContinuation)
    cancellationContinuation.getResult()
}