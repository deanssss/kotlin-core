package xyz.dean.kotlin_core.async.coroutines.imitate

import xyz.dean.kotlin_core.async.coroutines.log
import kotlin.concurrent.thread
import kotlin.coroutines.*

fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
): Job {
    val completion = StandardCoroutine(context)
    block.startCoroutine(completion)
    return completion
}

suspend fun main() {
    val job = launch {
        log("1")
        val result = hello()
        log("result", result)
    }
    log("${job.isActive}")
    job.join()
    log("after")
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) { // 模拟一个耗时操作
        Thread.sleep(1000)
        it.resume(10086)
    }
}