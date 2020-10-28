package xyz.dean.kotlin_core.async.coroutines.imitate

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
): Job {
    val completion = StandardCoroutine(context)
    block.startCoroutine(completion)
    return completion
}