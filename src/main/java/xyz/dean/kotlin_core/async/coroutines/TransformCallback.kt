package xyz.dean.kotlin_core.async.coroutines

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import xyz.dean.kotlin_core.api.PhraseService
import xyz.dean.kotlin_core.api.core.ApiClient
import xyz.dean.kotlin_core.api.core.defaultOKHttpClientProvider
import xyz.dean.kotlin_core.util.log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Call<T>.await(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            response.takeIf { it.isSuccessful }
                ?.body()
                ?.let { continuation.resume(it) }
                ?: continuation.resumeWithException(HttpException(response))
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            continuation.resumeWithException(t)
        }
    })
}

suspend fun <T> Handler.run(block: () -> T): T = suspendCoroutine { continuation ->
    post {
        try {
            continuation.resume(block())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

suspend fun main() {
    Looper.prepare()
    val handler = Handler()

    GlobalScope.launch {
        val apiClient = ApiClient.Builder("https://v1.hitokoto.cn")
            .build(defaultOKHttpClientProvider())
        val phraseService = apiClient.createService(PhraseService::class)

        val phrase = phraseService.getPhrase().await()
        log(phrase.phrase)
        val onBack = handler.run {
            log("execute...")
            "xxx"
        }
        log("onBack: $onBack")
    }

    Looper.loop()
}