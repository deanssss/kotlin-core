package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlin.coroutines.CoroutineContext

//typealias OnComplete = () -> Unit
typealias OnCancel = () -> Unit
typealias CancellationException = java.util.concurrent.CancellationException

interface Job : CoroutineContext.Element {
    companion object : CoroutineContext.Key<Job>
    override val key: CoroutineContext.Key<*> get() = Job

    val isActive: Boolean

    val isCompleted: Boolean

//    fun invokeOnComplete(onComplete: OnComplete): Disposable

    fun invokeOnCancel(onCancel: OnCancel): Disposable

    fun remove(disposable: Disposable)

    suspend fun join()

    fun cancel()
}