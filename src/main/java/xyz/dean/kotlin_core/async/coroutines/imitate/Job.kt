package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlin.coroutines.CoroutineContext

//typealias OnComplete = () -> Unit

interface Job : CoroutineContext.Element {
    companion object : CoroutineContext.Key<Job>
    override val key: CoroutineContext.Key<*> get() = Job

    val isActive: Boolean

    val isCompleted: Boolean

//    fun invokeOnComplete(onComplete: OnComplete): Disposable

    fun remove(disposable: Disposable)

    suspend fun join()
}