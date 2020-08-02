package xyz.dean.kotlin_core.design_pattren

fun add(a: Int, b: Int): Int = a + b
fun minus(a: Int, b: Int): Int = a - b
fun times(a: Int, b: Int): Int = a * b
fun divide(a: Int, b: Int): Int = a / b

class Calculator(val algorithm: (Int, Int) -> Int) {
    fun calculate(a: Int, b: Int): Int = algorithm(a, b)
}