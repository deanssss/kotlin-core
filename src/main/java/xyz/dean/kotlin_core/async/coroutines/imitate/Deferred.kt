package xyz.dean.kotlin_core.async.coroutines.imitate

interface Deferred<T> : Job {
    suspend fun await(): T
}