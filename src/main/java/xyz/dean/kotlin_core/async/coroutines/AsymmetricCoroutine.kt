/*
 * 借助Kotlin协程仿写Lua中的非对称协程。
 */

package xyz.dean.kotlin_core.async.coroutines

import xyz.dean.kotlin_core.async.coroutines.dispatcher.DispatcherContext
import xyz.dean.kotlin_core.util.log
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

sealed class Status {
    class Create(val continuation: Continuation<Unit>) : Status()
    class Yield<P>(val continuation: Continuation<P>) : Status()
    class Resumed<R>(val continuation: Continuation<R>) : Status()
    object Dead : Status()
}

class Coroutine<P, R>(
    override val context: CoroutineContext = EmptyCoroutineContext,
    @Suppress("RemoveRedundantQualifierName")
    block: suspend Coroutine<P, R>.CoroutineScope.() -> R
) : Continuation<R> {

    private val scope = CoroutineScope()
    private val status: AtomicReference<Status>

    val isActive: Boolean
        get() = status.get() != Status.Dead

    init {
        val start = block.createCoroutine(scope, this)
        status = AtomicReference(Status.Create(start))
    }

    private var initParam: P? = null
    suspend fun resume(value: P): R = suspendCoroutine { continuation ->
        val previousStatus = status.getAndUpdate {
            when (it) {
                is Status.Create -> {
                    initParam = value
                    Status.Resumed(continuation)
                }
                is Status.Yield<*> -> {
                    Status.Resumed(continuation)
                }
                is Status.Resumed<*> -> throw IllegalArgumentException("Already resumed!")
                Status.Dead -> throw IllegalArgumentException("Already dead!")
            }
        }
        @Suppress("UNCHECKED_CAST")
        when (previousStatus) {
            is Status.Create -> previousStatus.continuation.resume(Unit)
            is Status.Yield<*> -> (previousStatus as Status.Yield<P>).continuation.resume(value ?: initParam!!)
        }
    }

    override fun resumeWith(result: Result<R>) {
        val previousStatus = status.getAndUpdate {
            when (it) {
                is Status.Create -> throw IllegalArgumentException("Not started!")
                is Status.Yield<*> -> throw IllegalArgumentException("Already yield!")
                is Status.Resumed<*> -> {
                    Status.Dead
                }
                Status.Dead -> throw IllegalArgumentException("Already dead!")
            }
        }
        @Suppress("UNCHECKED_CAST")
        (previousStatus as? Status.Resumed<R>)
            ?.continuation
            ?.resumeWith(result)
    }

    @Suppress("unused")
    suspend fun <SymT> SymCoroutine<SymT>.yield(value: R): P {
        return scope.yield(value)
    }

    inner class CoroutineScope {
        suspend fun yield(value: R): P = suspendCoroutine { continuation ->
            val previousStatus = status.getAndUpdate {
                when (it) {
                    is Status.Create -> throw IllegalArgumentException("Not started!")
                    is Status.Yield<*> -> throw IllegalArgumentException("Already yielded!")
                    is Status.Resumed<*> -> Status.Yield(continuation)
                    Status.Dead -> throw IllegalArgumentException("Already dead!")
                }
            }
            @Suppress("UNCHECKED_CAST")
            (previousStatus as? Status.Resumed<R>)
                ?.continuation
                ?.resume(value)
        }
    }

    companion object {
        fun <P, R> create(
            context: CoroutineContext = EmptyCoroutineContext,
            @Suppress("RemoveRedundantQualifierName")
            block: suspend Coroutine<P, R>.CoroutineScope.() -> R
        ): Coroutine<P, R> {
            return Coroutine(context, block)
        }
    }
}

suspend fun main() {
    val producer = Coroutine.create<Unit, Int>(DispatcherContext()) {
        yield(666)
        for (i in 0 .. 3) {
            log("send", i)
            yield(i)
        }
        log("return", 200)
        200
    }

    val consumer = Coroutine.create<Int, Unit>(DispatcherContext()) {
        for (i in 0 .. 3) {
            val value = yield(Unit)
            log("receive", value)
        }
    }

    while (producer.isActive && consumer.isActive) {
        val result = producer.resume(Unit)
        consumer.resume(result)
    }
}