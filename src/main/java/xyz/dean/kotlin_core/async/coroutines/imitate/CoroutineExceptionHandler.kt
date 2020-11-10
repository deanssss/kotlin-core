package xyz.dean.kotlin_core.async.coroutines.imitate

import xyz.dean.kotlin_core.async.coroutines.log
import java.lang.RuntimeException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface CoroutineExceptionHandler : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, exception: Throwable)
}

@Suppress("FunctionName")
inline fun CoroutineExceptionHandler(
    crossinline handler: (CoroutineContext, Throwable) -> Unit
): CoroutineExceptionHandler =
    object : AbstractCoroutineContextElement(CoroutineExceptionHandler),
        CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            handler.invoke(context, exception)
        }
    }

@Suppress("UNREACHABLE_CODE")
suspend fun main() {
    val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        log("${context[Job]}", throwable)
    }
    val job = launch(exceptionHandler) {
        log("log-1")
        delay(1000L)
        log("log-2")
        throw RuntimeException("Error!!!")
        log("log-3")
    }
    job.join()
}