package xyz.dean.kotlin_core.functional

interface Monoid<T> {
    fun zero(): T
    fun T.append(n: T): T
}

object StringConcat : Monoid<String> {
    override fun zero(): String = ""
    override fun String.append(n: String): String = this + n
}

fun <T> FList<T>.sum(monoid: Monoid<T>): T {
    return FListFoldable.run {
        fold(monoid.zero()) { i, c ->
            monoid.run { c.append(i) }
        }
    }
}

fun main() {
    val list = Cons("Hello", Cons(" ", Cons("World", Cons("!", Nil))))
    println(list.sum(StringConcat))
}