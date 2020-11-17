package xyz.dean.kotlin_core.async.coroutines.imitate

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private val executor = Executors.newScheduledThreadPool(1) { runnable ->
    Thread(runnable, "Delay-Scheduler").apply {
        isDaemon = true
    }
}

suspend fun delay(
    time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Unit> { continuation ->
    val feature = executor.schedule({
        continuation.resume(Unit)
    }, time, unit)
    continuation.invokeOnCancel { feature.cancel(true) }
}