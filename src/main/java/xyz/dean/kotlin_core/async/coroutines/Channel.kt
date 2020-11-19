/*
 * 利用Kotlin的协程实现Go中的Channel。
 */

package xyz.dean.kotlin_core.async.coroutines

import xyz.dean.kotlin_core.async.coroutines.dispatcher.DispatcherContext
import xyz.dean.kotlin_core.util.log
import java.lang.Exception
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

interface Channel<T> {
    suspend fun send(value: T)
    suspend fun receive(): T
    fun close()
}

class ClosedException(message: String) : Exception(message)

class SimpleChannel<T> : Channel<T> {
    override suspend fun send(value: T) = suspendCoroutine<Unit> { continuation ->
        val prev = state.getAndUpdate {
            when (it) {
                State.None -> State.Producer(value, continuation)
                is State.Producer<*> -> error("The data has not yet been consumed.")
                is State.Consumer<*> -> State.None
                State.Closed -> error("Cannot send because channel is closed.")
            }
        }

        @Suppress("UNCHECKED_CAST")
        (prev as? State.Consumer<T>)?.continuation
            ?.resume(value)
            ?.let {
                continuation.resume(Unit)
            }
    }

    override suspend fun receive(): T = suspendCoroutine { continuation ->
        val prev = state.getAndUpdate {
            when (it) {
                State.None -> State.Consumer(continuation)
                is State.Producer<*> -> State.None
                is State.Consumer<*> -> error("No data been consumed.")
                State.Closed -> error("Cannot receive because channel is closed.")
            }
        }

        @Suppress("UNCHECKED_CAST")
        (prev as? State.Producer<T>)
            ?.let {
                it.continuation.resume(Unit)
                continuation.resume(it.value)
            }
    }

    override fun close() {
        when (val prev = state.getAndUpdate { State.Closed }) {
            is State.Consumer<*> -> prev.continuation.resumeWithException(ClosedException("Channel is closed."))
            is State.Producer<*> -> prev.continuation.resumeWithException(ClosedException("Channel is closed."))
        }
    }

    private val state = AtomicReference<State>(State.None)

    sealed class State {
        object None : State()
        class Producer<T>(val value: T, val continuation: Continuation<Unit>) : State()
        class Consumer<T>(val continuation: Continuation<T>) : State()
        object Closed : State()

        override fun toString(): String = this.javaClass.simpleName
    }
}

fun go(
    name: String = "",
    completion: () -> Unit = {},
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
) {
    block.startCoroutine(object : Continuation<Any> {
        override val context = context

        override fun resumeWith(result: Result<Any>) {
            log("Channel: $name end.", result)
            completion()
        }
    })
}

suspend fun main() {
    val channel = SimpleChannel<Int>()

    go("producer", context = DispatcherContext()) {
        for (i in 0 .. 6) {
            log("send", i)
            channel.send(i)
        }
    }

    go("consumer", channel::close, DispatcherContext()) {
        for (i in 0 .. 5) {
            log("receive...")
            val got = channel.receive()
            log("got", got)
        }
    }
}