package xyz.dean.kotlin_core.async.coroutines.imitate.sample

import xyz.dean.kotlin_core.async.coroutines.imitate.GlobalScope
import xyz.dean.kotlin_core.async.coroutines.imitate.delay
import xyz.dean.kotlin_core.async.coroutines.imitate.launch
import xyz.dean.kotlin_core.async.coroutines.log
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    val job2 = GlobalScope.launch {
        log("delay---1")
        val res = hello()
        log("delay---2", res)
        delay(1000L)
        log("delay---3")
    }
    job2.cancel()
    job2.join()
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = false) { // 模拟一个耗时操作
        log("say hello")
        Thread.sleep(1000)
        it.resume(10086)
    }
}