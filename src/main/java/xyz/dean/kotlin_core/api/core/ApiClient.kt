package xyz.dean.kotlin_core.api.core

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass

class ApiClient private constructor(
    private val retrofit: Retrofit
) {
    fun <S> createService(serviceClass: Class<S>): S =
        retrofit.create(serviceClass)

    fun <S : Any> createService(serviceClass: KClass<S>): S =
        retrofit.create(serviceClass.java)

    class Builder(
        private val baseUrl: String,
        private val retrofitBuilder: Retrofit.Builder = defaultRetrofitBuilder()
    ) {
        fun build(okHttpClient: OkHttpClient = defaultOKHttpClientProvider()): ApiClient {
            retrofitBuilder.baseUrl(baseUrl)
            val retrofit = retrofitBuilder.client(okHttpClient).build()
            return ApiClient(retrofit)
        }
    }
}

private fun defaultRetrofitBuilder() =
    Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())

private const val UNSAFE_SSL = false
private const val ALLOW_LOG = true

@Suppress("ConstantConditionIf")
fun defaultOKHttpClientProvider(): OkHttpClient {
    val builder = OkHttpClient.Builder()

    if (ALLOW_LOG) {
        builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    }

    if (UNSAFE_SSL) {
        unsafeSSLSocketFactory()?.also { (factory, trustManager) ->
            builder.sslSocketFactory(factory, trustManager)
                .hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
    }

    return builder.build()
}

private fun unsafeSSLSocketFactory(): Pair<SSLSocketFactory, X509TrustManager>? {
    val sslContext = SSLContext.getInstance("TLS")
    val trustManager = unsafeTrustManager()
    sslContext.init(null, arrayOf(trustManager), SecureRandom())
    return sslContext.socketFactory to trustManager
}

private fun unsafeTrustManager(): X509TrustManager {
    return object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) { }
    }
}