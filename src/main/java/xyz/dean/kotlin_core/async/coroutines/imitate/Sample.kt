package xyz.dean.kotlin_core.async.coroutines.imitate

import xyz.dean.kotlin_core.async.coroutines.dispatcher.Dispatchers
import xyz.dean.kotlin_core.async.coroutines.log
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun main() = runBlocking {
    log("Job Start")
    val job = launch(Dispatchers.Single) {
        log("Job-1")
        val result = hello()
        log("Hello-result", result)
        delay(1000L)
        log("Job-2")
    }
    log("JobState ${job.isActive}")
    job.join()
    log("Job End")
    log("=======================")
    log("Async Start")
    val def = async {
        log("Async-1")
        delay(1000L)
        log("Async-2")
        "Hello Async"
//        error("Exception")
    }
    log("waiting...")
    val result = def.await()
    log("Async Result:", result)
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) { // 模拟一个耗时操作
        log("say hello")
        Thread.sleep(1000)
        it.resume(10086)
    }
}