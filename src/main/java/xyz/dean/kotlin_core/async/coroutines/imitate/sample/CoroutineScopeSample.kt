package xyz.dean.kotlin_core.async.coroutines.imitate.sample

import xyz.dean.kotlin_core.async.coroutines.imitate.*
import xyz.dean.kotlin_core.async.coroutines.log
import java.lang.RuntimeException

@Suppress("UNREACHABLE_CODE")
suspend fun main() {
    val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        log("${context[Job]}", throwable)
    }
    val job = GlobalScope.launch(exceptionHandler) {
        log("log-1")
        delay(1000L)
        log("log-2")
        val job2 = launch {
            throw RuntimeException("Error!!!")
        }
        log("log-3")
        job2.join()
        log("log-4")
        delay(1000L)
        log("log-5")
    }
    job.join()

    log("=========")

    val job3 = GlobalScope.launch(exceptionHandler) {
        log("log-1")
        delay(1000L)
        supervisorScope {
            log("log-2")
            val job2 = launch(exceptionHandler) {
                throw RuntimeException("Error!!!")
            }
            log("log-3")
            job2.join()
            log("log-4")
            delay(1000L)
            log("log-5")
        }
    }
    job3.join()
}