package xyz.dean.kotlin_core.async.coroutines.imitate

import xyz.dean.kotlin_core.async.coroutines.dispatcher.Dispatchers
import xyz.dean.kotlin_core.async.coroutines.log
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    val job = launch(Dispatchers.Single) {
        log("1")
        val result = hello()
        log("result", result)
        delay(1000L)
        log("3")
    }
    log("${job.isActive}")
    job.join()
    log("after")
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) { // 模拟一个耗时操作
        log("say hello")
        Thread.sleep(1000)
        it.resume(10086)
    }
}