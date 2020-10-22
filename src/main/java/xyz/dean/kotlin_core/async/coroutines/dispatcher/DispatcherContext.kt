package xyz.dean.kotlin_core.async.coroutines.dispatcher

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

open class DispatcherContext(
    private val dispatcher: Dispatcher = DefaultDispatcher
) : AbstractCoroutineContextElement(ContinuationInterceptor),
    ContinuationInterceptor
{
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
            = DispatchedContinuation(continuation, dispatcher)
}

private class DispatchedContinuation<T>(
    val delegate: Continuation<T>,
    val dispatcher: Dispatcher
) : Continuation<T> {
    override val context= delegate.context

    override fun resumeWith(result: Result<T>) {
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }
}