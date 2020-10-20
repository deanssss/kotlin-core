/*
 *借助Kotlin协程仿写一个非线程安全的python Generator(懒序列)。
 */

package xyz.dean.kotlin_core.async.coroutines

import kotlin.coroutines.*

interface Generator<T> {
    operator fun iterator(): Iterator<T>
}

class GeneratorImpl<T>(
    private val block: suspend GeneratorScope<T>.() -> Unit
) : Generator<T> {
    override fun iterator(): Iterator<T> {
        return GeneratorIterator(block)
    }
}

abstract class GeneratorScope<in T> internal constructor() {
    abstract suspend fun yield(value: T)
}

sealed class State {
    class NotReady(val continuation: Continuation<Unit>) : State()
    class Ready<T>(val continuation: Continuation<Unit>, val value: T) : State()
    object Done : State()
}

class GeneratorIterator<T>(
    block: suspend GeneratorScope<T>.() -> Unit
) : GeneratorScope<T>(), Iterator<T>, Continuation<Any?> {
    override val context: CoroutineContext = EmptyCoroutineContext
    private var state: State

    init {
        val start = block.createCoroutine(this, this)
        state = State.NotReady(start)
    }

    override suspend fun yield(value: T) = suspendCoroutine<Unit> { continuation ->
        state = when (state) {
            is State.NotReady -> State.Ready(continuation, value)
            is State.Ready<*> -> throw IllegalStateException("Cannot yield a value while ready.")
            State.Done -> throw IllegalArgumentException("Cannot yield a value while done.")
        }
    }

    override fun hasNext(): Boolean {
        resume()
        return state != State.Done
    }

    override fun next(): T {
        return when (val currentState = state) {
            is State.NotReady -> {
                resume()
                return next()
            }
            is State.Ready<*> -> {
                state = State.NotReady(currentState.continuation)
                @Suppress("UNCHECKED_CAST")
                (currentState as State.Ready<T>).value
            }
            State.Done -> throw IndexOutOfBoundsException("No value left")
        }
    }

    private fun resume() {
        when (val currentState = state) {
            is State.NotReady -> currentState.continuation.resume(Unit)
        }
    }

    override fun resumeWith(result: Result<Any?>) {
        state = State.Done
        result.getOrThrow()
    }
}

fun <T> generator(block: suspend GeneratorScope<T>.() -> Unit): Generator<T> {
    return GeneratorImpl(block)
}

fun main() {
    val generator = generator<Int> {
        yield(1)
        yield(2)
        yield(3)
    }

    for (i in generator) {
        println(i)
    }

    val sequence = sequence<Int> {
        yield(3)
        yield(2)
        yield(1)
    }

    for (i in sequence) {
        println(i)
    }
}