package xyz.dean.kotlin_core.functional

object OptionHK
sealed class Option<out A> : Kind<OptionHK, A>

data class Some<V>(val value: V) : Option<V>()
object None : Option<Nothing>()

fun <A> Kind<OptionHK, A>.fix(): Option<A> =
    this as Option<A>

object OptionMonad : Monad<OptionHK> {
    override fun <A, B> Kind<OptionHK, A>.flatMap(f: (A) -> Kind<OptionHK, B>): Kind<OptionHK, B> {
        return when(this) {
            is Some -> f(value)
            else -> None
        }
    }

    override fun <A> pure(a: A): Kind<OptionHK, A> =  Some(a)
}

fun main() {
    fun readInt(): StdIO<Option<Int>> {
        return StdIOMonad.run {
            StdIO.readLine().map {
                when {
                    it.matches(Regex("[0-9]+")) -> Some(it.toInt())
                    else -> None
                }
            }.fix()
        }
    }

    fun addOption(oa: Option<Int>, ob: Option<Int>) =
        OptionMonad.run {
            oa.flatMap { a ->
                ob.map { b -> a + b }
            }
        }

    fun errorHandleWithOption(): Kind<StdIOHK, Unit> {
        return StdIOMonad.run {
            readInt().flatMap { oi ->
                readInt().flatMap { oj ->
                    val r = addOption(oi, oj)
                    val display = when(r) {
                        is Some<*> -> r.value.toString()
                        else -> ""
                    }
                    StdIO.writeLine(display)
                }
            }
        }
    }

    perform(errorHandleWithOption().fix())
}