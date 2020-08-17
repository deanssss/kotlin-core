package xyz.dean.kotlin_core.functional

object StdIOHK
fun <A> Kind<StdIOHK, A>.fix(): StdIO<A> = this as StdIO<A>

sealed class StdIO<A> : Kind<StdIOHK, A> {
    fun <A> pure(a: A): StdIO<A> = Pure(a)

    companion object {
        fun readLine(): StdIO<String> = ReadLine
        fun writeLine(l: String): StdIO<Unit> = WriteLine(l)
    }
}

object ReadLine : StdIO<String>()
data class WriteLine(val l : String) : StdIO<Unit>()
data class Pure<A>(val a: A): StdIO<A>()
data class FlatMap<A, B>(val fa: StdIO<A>, val f: (A) -> StdIO<B>): StdIO<B>()

object StdIOMonad : Monad<StdIOHK> {
    override fun <A> pure(a: A): Kind<StdIOHK, A> = Pure(a)

    override fun <A, B> Kind<StdIOHK, A>.flatMap(f: (A) -> Kind<StdIOHK, B>): Kind<StdIOHK, B> =
        FlatMap(this.fix()) { a -> f(a).fix() }
}

@Suppress("UNCHECKED_CAST")
fun <A> perform(stdIO: StdIO<A>): A {
    fun <C, D> runFlatMap(fm: FlatMap<C, D>) {
        perform(fm.f(perform(fm.fa)))
    }

    return when(stdIO) {
        ReadLine -> readLine() as A
        is WriteLine -> println(stdIO.l) as A
        is Pure -> stdIO.a
        is FlatMap<*, A> -> runFlatMap(stdIO) as A
    }
}

fun main() {
    val io = StdIOMonad.run {
        StdIO.readLine().flatMap { a ->
            StdIO.readLine().flatMap { b->
                StdIO.writeLine("a=$a b=$b")
            }
        }
    }

    perform(io.fix())
}