package xyz.dean.kotlin_core.async.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() {
    val executeTime = measureTimeMillis { runBlocking {
        launch { // A
            println(",1")
            delay(1000L)
            println(",2")
        }
        launch { // B
            println("world1")
            delay(1000L)
            println("world2")
        }
        println("hello1")
        delay(1000L)
        println("hello2")

        // 协程的第一个作用，以阻塞调用的样子写出非阻塞的代码（callback）
        // 协程中阻塞仍然会导致相关的线程阻塞

        ///////////////
        // block-1
        ///////////////
        // launch { } // A
        // launch { } // B
        // println("hello1")
        // delay(1000L)
        // 至少1s后再执行block-6

        ///////////////
        // block-2
        ///////////////
        // println(",1")
        // delay(1000L)
        // 至少1s后再执行block-4

        ///////////////
        // block-3
        ///////////////
        // println("world1")
        // delay(1000L)
        // 至少1s后再执行block-5

        ///////////////
        // block-4
        ///////////////
        // println(",2")

        ///////////////
        // block-5
        ///////////////
        // println("world2")

        ///////////////
        // block-6
        ///////////////
        // println("hello2")
    } }
    println(executeTime)
}