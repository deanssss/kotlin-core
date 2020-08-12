package xyz.dean.kotlin_core.functional

sealed class FList<out T> : Kind<FListHK, T>
object Nil : FList<Nothing>()
data class Cons<T>(val head: T, val tail: FList<T>): FList<T>()

object FListHK
@Suppress("UNCHECKED_CAST")
fun <T> Kind<FListHK, T>.fix(): FList<T> = this as FList<T>

fun main() {
    Cons(1, Cons(2, Cons(3, Nil)))
}