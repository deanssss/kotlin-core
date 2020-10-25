package xyz.dean.kotlin_core.async.coroutines.imitate

import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class AbstractCoroutine<T>(
    override val context: CoroutineContext
) : Job, Continuation<T> {
    private val state = AtomicReference<CoroutineState>(InComplete())

    override val isActive: Boolean
        get() = state.get() is InComplete

//    override fun invokeOnComplete(onComplete: OnComplete): Disposable {
//        return doOnCompleted { onComplete() }
//    }

    override fun remove(disposable: Disposable) {
        state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> InComplete().from(oldState).withOut(disposable)
                is Complete<*> -> oldState
            }
        }
    }

    override suspend fun join() {
        when (state.get()) {
            is InComplete -> return joinSuspend()
        }
    }

    private suspend fun joinSuspend() = suspendCoroutine<Unit> { continuation ->
        doOnCompleted {
            continuation.resume(Unit) // 恢复调用join的协程的执行。
        }
    }

    // 为当前协程注册了一个执行完成时的回调，disposable会被放到disposableList中，待当前协程执行完毕时（resumeWith调用时）回调。
    private fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val  disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet { oldState ->
            when (oldState) {
                is InComplete -> {
                    InComplete().from(oldState).with(disposable)
                }
                is Complete<*> -> {
                    oldState
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
                is InComplete -> {
                    Complete(result.getOrNull(), result.exceptionOrNull()).from(oldState)
                }
                is Complete<*> -> error("Already completed!")
            }
        }
//        (newState as Complete<T>).exception?.let {  }
        newState.notifyCompletion(result)
        newState.clear()
    }
}