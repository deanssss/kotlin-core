package xyz.dean.kotlin_core.functional

interface Foldable<F> {
    fun <A, B> Kind<F, A>.fold(init: B, f: (A, B) -> B): B
}

object FListFoldable: Foldable<FListHK> {
    private fun <A, B> Kind<FListHK, A>.fold0(v: B, f: (A, B) -> B): B {
        return when(this) {
            is Cons -> this.tail.fold0(f(this.head, v), f)
            else -> v
        }
    }

    override fun <A, B> Kind<FListHK, A>.fold(init: B, f: (A, B) -> B): B = fold0(init, f)
}

fun main() {
    FListFoldable.apply {
        val v = Cons(1, Cons(2, Cons(3, Nil))).fold(0, {a, b -> a + b})
        println(v)
    }
}