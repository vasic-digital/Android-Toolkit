package com.redelf.commons.retrofit

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Timber
import com.redelf.commons.obtain.ObtainParametrized
import com.redelf.commons.retrofit.gson.SerializationBenchmarkLoggingInterceptor
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider : ObtainParametrized<Retrofit, RetrofitApiParameters> {

    var DEBUG: Boolean? = null

    val PINNED_CERTIFICATES = mutableMapOf<String, String>()

    override fun obtain(param: RetrofitApiParameters): Retrofit {

        if (param.verbose == true) Timber.v("Retrofit :: Obtain: $param")

        var interceptor: HttpLoggingInterceptor? = null

        if (DEBUG ?: BaseApplication.DEBUG.get()) {

            if (param.verbose == true) Timber.v("Retrofit :: Debug :: ON")

            interceptor = HttpLoggingInterceptor()

            if (param.bodyLog == true) {

                if (param.verbose == true) Timber.v("Retrofit :: Debug :: BODY")

                interceptor.level = HttpLoggingInterceptor.Level.BODY

            } else {

                if (param.verbose == true) Timber.v("Retrofit :: Debug :: BASIC")

                interceptor.level = HttpLoggingInterceptor.Level.BASIC
            }
        }

        val ctx = param.ctx.applicationContext
        val rTime = param.readTimeoutInSeconds
        val wTime = param.writeTimeoutInSeconds
        val cTime = param.connectTimeoutInSeconds

        val baseUrl = ctx.getString(param.endpoint ?: 0)

        val client = newHttpClient(

            interceptor,

            readTime = rTime ?: 0,
            connTime = cTime ?: 0,
            writeTime = wTime ?: 0,

            verbose = param.bodyLog == true || param.verbose == true
        )

        val converter: Converter.Factory = if (param.scalar == true) {

            if (param.verbose == true) Timber.v("Retrofit :: Converter: Scalar")

            ScalarsConverterFactory.create()

        } else if (param.jackson == true) {

            if (param.verbose == true) Timber.v("Retrofit :: Converter: Jackson")

            val objectMapper = ObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)

            JacksonConverterFactory.create(objectMapper)

        } else {

            if (param.verbose == true) Timber.v("Retrofit :: Converter: GSON")

            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setLenient()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .create()

            GsonConverterFactory.create(GsonBuilder().create())
        }

        val callFactory = Call.Factory { request ->

            val call = client.newCall(request)
            val tag = request.url.toString()

            param.callsWrapper?.set(tag, call)

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
        writeTime: Long = -1L,
        verbose: Boolean = false

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

        if (DEBUG ?: BaseApplication.DEBUG.get() && verbose) {

            val benchInterceptor = SerializationBenchmarkLoggingInterceptor()
            builder.addInterceptor(benchInterceptor)
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