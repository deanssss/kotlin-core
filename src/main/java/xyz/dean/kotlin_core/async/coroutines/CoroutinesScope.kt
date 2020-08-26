package xyz.dean.kotlin_core.async.coroutines

import kotlinx.coroutines.*

fun main() {
    blockingScope()
    globalScope()
    customScope()
    Thread.sleep(3000L)
}

// runBlocking会导致当前线程被阻塞，直到协程执行完毕
fun blockingScope() {
    runBlocking {
        println("1")
        delay(1000L)
    }
    println("2")
}

// GlobalScope不会阻塞线程，可以在整个应用的声明周期中操作，且无法取消，容易造成内存泄漏
fun globalScope() {
    val job = GlobalScope.launch {
        println("11")
        delay(1000L)
    }
    println("22")
    job.cancel()
}

// 自定义Scope不会阻塞线程，且可以取消，不会造成内存泄露
fun customScope() {
    val scope = MainScope()
    val job = scope.launch(Dispatchers.IO) {
        println("111")
        delay(1000L)
    }
    job.cancel()
    println("222")
}