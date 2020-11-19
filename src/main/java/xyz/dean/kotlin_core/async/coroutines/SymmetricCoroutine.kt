/*
 * 在非对称协程的基础上，通过一个中间调度器协程实现对称协程。
 */

package xyz.dean.kotlin_core.async.coroutines

import xyz.dean.kotlin_core.async.coroutines.dispatcher.DispatcherContext
import xyz.dean.kotlin_core.util.log
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class SymCoroutine<T> private constructor(
    override val context: CoroutineContext = EmptyCoroutineContext,
    private val block: suspend SymCoroutine<T>.SymCoroutineScope.() -> Unit
) : Continuation<T> {
    val isMain: Boolean
        get() = this == main

    private val scope = SymCoroutineScope()

    override fun resumeWith(result: Result<T>) {
        throw IllegalArgumentException("SymCoroutine cannot be dead!")
    }

    suspend fun start(value: T) {
        coroutine.resume(value)
    }

    private val coroutine = Coroutine<T, Param<*>>(context) {
        @Suppress("UNCHECKED_CAST")
        Param(this@SymCoroutine, suspend {
            block(scope)
            if (this@SymCoroutine.isMain) Unit
            else throw IllegalArgumentException("SymCoroutine cannot be dead!")
        }() as T)
    }

    class Param<T>(val coroutine: SymCoroutine<T>, val value: T)

    inner class SymCoroutineScope {
        private tailrec suspend fun <P> innerTransfer(symCoroutine: SymCoroutine<P>, value: Any?): T {
            @Suppress("UNCHECKED_CAST")
            return if (this@SymCoroutine.isMain) {
                if (symCoroutine.isMain) {
                    value as T
                } else {
                    val param = symCoroutine.coroutine.resume(value as P)
                    innerTransfer(param.coroutine, param.value)
                }
            } else {
                this@SymCoroutine.coroutine.run {
                    yield(Param(symCoroutine, value as P))
                }
            }
        }

        suspend fun <P> transfer(symCoroutine: SymCoroutine<P>, value: P): T {
            return innerTransfer(symCoroutine, value)
        }
    }

    companion object {
        lateinit var main: SymCoroutine<Any?>

        suspend fun main(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend SymCoroutine<Any?>.SymCoroutineScope.() -> Unit
        ) {
            SymCoroutine<Any?>(context) { block() }.also { main = it }.start(Unit)
        }

        fun <T> create(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend SymCoroutine<T>.SymCoroutineScope.() -> Unit
        ): SymCoroutine<T> {
            return SymCoroutine(context, block)
        }
    }
}

object SymCoroutines {
    val coroutine0: SymCoroutine<Int> = SymCoroutine.create(DispatcherContext()) {
        log("coroutine-0", "first")
        var result = transfer(coroutine2, 0)
        log("coroutine-0", "second", "received:$result")
        result = transfer(SymCoroutine.main, Unit)
        log("coroutine-0", "third", "receive:$result")
    }

    val coroutine1: SymCoroutine<Int> = SymCoroutine.create(DispatcherContext()) {
        log("coroutine-1", "first")
        val result = transfer(coroutine0, 1)
        log("coroutine-1", "second", "received:$result")
    }

    val coroutine2: SymCoroutine<Int> = SymCoroutine.create(DispatcherContext()) {
        log("coroutine-2", "first")
        var result = transfer(coroutine1, 2)
        log("coroutine-2", "second", "received:$result")
        result = transfer(coroutine0, 3)
        log("coroutine-2", "third", "received:$result")
    }
}

suspend fun main() {
    SymCoroutine.main(DispatcherContext()) {
        log("main", "start")
        val result = transfer(SymCoroutines.coroutine2, -1)
        log("main", "end", "received:$result")
    }
}