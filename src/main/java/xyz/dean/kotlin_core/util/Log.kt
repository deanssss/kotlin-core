package xyz.dean.kotlin_core.util

fun log(msg: String, vararg args: Any) {
    println("[${Thread.currentThread().name}] $msg ${args.joinToString(separator = " ")}")
}