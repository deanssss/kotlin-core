/*
 * 使用Kotlin协程实现javascript中的async/await。
 */

package xyz.dean.kotlin_core.async.coroutines

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import xyz.dean.kotlin_core.api.PhraseService
import xyz.dean.kotlin_core.api.core.ApiClient
import xyz.dean.kotlin_core.async.coroutines.dispatcher.Dispatcher
import xyz.dean.kotlin_core.async.coroutines.dispatcher.DispatcherContext
import kotlin.coroutines.*

interface AsyncScope

suspend fun <T> AsyncScope.await(block: () -> Call<T>) = suspendCoroutine<T> { continuation ->
    val call = block()
    call.enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                response.body()
                    ?.let { continuation.resume(it) }
                    ?: continuation.resumeWithException(NullPointerException())
            } else {
                continuation.resumeWithException(HttpException(response))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            continuation.resumeWithException(t)
        }
    })
}

fun async(context: CoroutineContext = EmptyCoroutineContext, block: suspend AsyncScope.() -> Unit) {
    val completion = AsyncCoroutine(context)
    block.startCoroutine(completion, completion)
}

class AsyncCoroutine(
    override val context: CoroutineContext = EmptyCoroutineContext
) : Continuation<Unit>, AsyncScope {
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
    }
}

fun main() {
    Looper.prepare()
    val apiClient = ApiClient.Builder("https://v1.hitokoto.cn")
        .build(oKHttpClientProvider())
    val phraseService = apiClient.createService(PhraseService::class)
    val handlerDispatcher = DispatcherContext(object : Dispatcher {
        val handler = Handler()
        override fun dispatch(block: () -> Unit) {
            handler.post(block)
        }
    })
    async(handlerDispatcher) {
        val phrase = await { phraseService.getPhrase() }
        log("Phrase:", phrase)
    }
    Looper.loop()
}

private fun oKHttpClientProvider(): OkHttpClient {
    val builder = OkHttpClient.Builder()
    val logger = object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            xyz.dean.kotlin_core.async.coroutines.log(message)
        }
    }
    builder.addInterceptor(HttpLoggingInterceptor(logger)
        .setLevel(HttpLoggingInterceptor.Level.BASIC))
    return builder.build()
}