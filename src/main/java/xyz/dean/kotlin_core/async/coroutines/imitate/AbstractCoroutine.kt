package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlinx.coroutines.CoroutineName
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

abstract class AbstractCoroutine<T>(
    context: CoroutineContext
) : Job, Continuation<T>, CoroutineScope {
    protected val state = AtomicReference<CoroutineState>(InComplete())
    override val context: CoroutineContext = context + this

    protected val parentJob = context[Job]

    override val scopeContext: CoroutineContext
        get() = context

    override val isActive: Boolean
        get() = state.get() is InComplete

    override val isCompleted: Boolean
        get() = state.get() is Complete<*>

    private var parentCancelDisposable: Disposable? =
        parentJob?.invokeOnCancel {
            cancel()
        }

//    override fun invokeOnComplete(onComplete: OnComplete): Disposable {
//        return doOnCompleted { onComplete() }
//    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        val disposable = CancellationHandlerDisposable(this, onCancel)
        val newState = state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> {
                    InComplete().from(oldState).with(disposable)
                }
                is Complete<*>,
                is Cancelling ->{
                    oldState
                }
            }
        }
        (newState as? Cancelling)?.let { onCancel() }
        return disposable
    }

    override fun remove(disposable: Disposable) {
        state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> InComplete().from(oldState).withOut(disposable)
                is Complete<*> -> oldState
                is Cancelling -> {
                    Cancelling().from(oldState).withOut(disposable)
                }
            }
        }
    }

    override suspend fun join() {
        when (state.get()) {
            is Cancelling,
            is InComplete -> return joinSuspend()
            is Complete<*> -> {
                val currentCallingState = coroutineContext[Job]?.isActive ?: return
                if (!currentCallingState) {
                    throw CancellationException("Coroutine is cancelled.")
                }
                return
            }
        }
    }

    override fun cancel() {
        val newState = state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> Cancelling().from(oldState)
                is Complete<*>,
                is Cancelling -> {
                    oldState
                }
            }
        }
        if (newState is Cancelling) {
            newState.notifyCancellation()
        }

        parentCancelDisposable?.dispose()
    }

    private suspend fun joinSuspend() = suspendCoroutine<Unit> { continuation ->
        doOnCompleted {
            continuation.resume(Unit) // 恢复调用join的协程的执行。
        }
    }

    // 为当前协程注册了一个执行完成时的回调，disposable会被放到disposableList中，待当前协程执行完毕时（resumeWith调用时）回调。
    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val  disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> {
                    InComplete().from(oldState).with(disposable)
                }
                is Complete<*> -> {
                    oldState
                }
                is Cancelling -> {
                    Cancelling().from(oldState).with(disposable)
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        (newState as? Complete<T>)?.let {
            block(
                when {
                    it.value != null -> Result.success(it.value)
                    it.exception != null -> Result.failure(it.exception)
                    else -> error("Won't happen!")
                }
            )
        }
        return disposable
    }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { oldState ->
            when (oldState) {
                is Cancelling,
                is InComplete -> {
                    Complete(result.getOrNull(), result.exceptionOrNull()).from(oldState)
                }
                is Complete<*> -> error("Already completed!")
            }
        }
        @Suppress("UNCHECKED_CAST")
        (newState as Complete<T>).exception?.let(::tryHandleException)

        newState.notifyCompletion(result)
        newState.clear()
        parentCancelDisposable?.dispose()
    }

    private fun tryHandleException(e: Throwable): Boolean {
        return when (e) {
            is CancellationException -> false
            else -> {
                (parentJob as? AbstractCoroutine<*>)
                    ?.handleChildException(e)
                    ?.takeIf { it }
                    ?: handleJobException(e)
            }
        }
    }

    protected open fun handleJobException(e: Throwable): Boolean = false

    protected open fun handleChildException(e: Throwable): Boolean {
        cancel()
        return tryHandleException(e)
    }

    override fun toString(): String {
        return "${context[CoroutineName]?.name}"
    }
}