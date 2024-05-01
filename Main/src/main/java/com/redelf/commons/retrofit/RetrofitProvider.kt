package com.redelf.commons.retrofit

import com.redelf.commons.BuildConfig
import com.redelf.commons.obtain.ObtainParametrized
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

object RetrofitProvider : ObtainParametrized<Retrofit, RetrofitApiParameters> {

    val PINNED_CERTIFICATES = mutableMapOf<String, String>()

    override fun obtain(param: RetrofitApiParameters): Retrofit {

        var interceptor: HttpLoggingInterceptor? = null

        if (BuildConfig.DEBUG) {

            interceptor = HttpLoggingInterceptor()

            if (param.bodyLog) {

                interceptor.level = HttpLoggingInterceptor.Level.BODY

            } else {

                interceptor.level = HttpLoggingInterceptor.Level.BASIC
            }
        }

        val ctx = param.ctx.applicationContext
        val rTime = param.readTimeoutInSeconds
        val wTime = param.writeTimeoutInSeconds
        val cTime = param.connectTimeoutInSeconds
        val baseUrl = ctx.getString(param.endpoint)

        val client = newHttpClient(interceptor, rTime, cTime, wTime)

        val converter: Converter.Factory = if (param.scalar) {

            ScalarsConverterFactory.create()

        } else {

            GsonConverterFactory.create()
        }

        val callFactory = Call.Factory { request ->

            val call = client.newCall(request)
            val tag = request.url.toString()

            param.callsWrapper[tag] = call

            call
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(converter)
            .callFactory(callFactory)
            .build()
    }

    private fun newHttpClient(

        loggingInterceptor: HttpLoggingInterceptor?,
        readTime: Long,
        connTime: Long,
        writeTime: Long = -1L

    ): OkHttpClient {

        val pool = ConnectionPool(

            maxIdleConnections = 7,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )

        val builder = OkHttpClient
            .Builder()
            .readTimeout(readTime, TimeUnit.SECONDS)
            .connectTimeout(connTime, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(pool)

        loggingInterceptor?.let {

            builder.addInterceptor(it)
        }

        if (writeTime > 0) {

            builder.writeTimeout(writeTime, TimeUnit.SECONDS)
        }

        if (PINNED_CERTIFICATES.isNotEmpty()) {

            builder.certificatePinner(createCertificatePins())
        }

        return builder.build()

    }

    private fun createCertificatePins(): CertificatePinner {

        val builder = CertificatePinner.Builder()

        PINNED_CERTIFICATES.forEach { (pattern, pins) ->

            try {

                builder.add(

                    pattern,
                    pins,
                )

            } catch (e: IllegalArgumentException) {

                Timber.e(e)
            }
        }

        return builder.build()
    }
}