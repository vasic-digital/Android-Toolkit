package com.redelf.commons.net.retrofit

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.redelf.commons.logging.Console
import com.redelf.commons.net.retrofit.gson.SerializationBenchmarkLoggingInterceptor
import com.redelf.commons.obtain.ObtainParametrized
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
import useCronet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object RetrofitProvider : ObtainParametrized<Retrofit, RetrofitApiParameters> {

    // TODO: Incorporate the support for Proxy:
    //  https://stackoverflow.com/questions/32053413/using-retrofit-behind-a-proxy
    //  https://github.com/proxifly/free-proxy-list/tree/main
    //  https://github.com/proxifly/free-proxy-list/blob/main/proxies/protocols/http/data.txt
    //  Proxy to be picked dynamically!

    val DEBUG: AtomicBoolean = AtomicBoolean()
    val PINNED_CERTIFICATES = mutableMapOf<String, String>()

    override fun obtain(param: RetrofitApiParameters): Retrofit {

        if (param.verbose == true) Console.log("Retrofit :: Obtain: $param")

        var interceptor: HttpLoggingInterceptor? = null

        if (DEBUG.get()) {

            if (param.verbose == true) Console.log("Retrofit :: Debug :: ON")

            interceptor = HttpLoggingInterceptor(RetrofitLogger())

            if (param.bodyLog == true) {

                if (param.verbose == true) Console.log("Retrofit :: Debug :: BODY")

                interceptor.level = HttpLoggingInterceptor.Level.BODY

            } else {

                if (param.verbose == true) Console.log("Retrofit :: Debug :: BASIC")

                interceptor.level = HttpLoggingInterceptor.Level.BASIC
            }
        }

        val ctx = param.ctx.applicationContext
        val rTime = param.readTimeoutInSeconds
        val wTime = param.writeTimeoutInSeconds
        val cTime = param.connectTimeoutInSeconds

        val baseUrl = ctx.getString(param.endpoint)

        val client = newHttpClient(

            interceptor,

            readTime = rTime ?: 0,
            connTime = cTime ?: 0,
            writeTime = wTime ?: 0,

            useCronet = param.useCronet?: true,
            verbose = param.bodyLog == true || param.verbose == true
        )

        val converter: Converter.Factory = if (param.scalar == true) {

            if (param.verbose == true) Console.log("Retrofit :: Converter: Scalar")

            ScalarsConverterFactory.create()

        } else if (param.jackson == true) {

            if (param.verbose == true) Console.log("Retrofit :: Converter: Jackson")

            val objectMapper = ObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)

            JacksonConverterFactory.create(objectMapper)

        } else {

            if (param.verbose == true) Console.log("Retrofit :: Converter: GSON")

            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setLenient() // FIXME: This is deprecated
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
        writeTime: Long,
        verbose: Boolean = false,
        useCronet: Boolean = true

    ): OkHttpClient {

        val pool = ConnectionPool(

            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )

        val builder = OkHttpClient.Builder()

        if (useCronet) {

            builder.useCronet()
        }

        builder
            .readTimeout(readTime, TimeUnit.SECONDS)
            .connectTimeout(connTime, TimeUnit.SECONDS)
            .writeTimeout(writeTime, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(pool)
            .addInterceptor(RetryInterceptor())

        loggingInterceptor?.let {

            builder.addInterceptor(it)
        }

        if (DEBUG.get() && verbose) {

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

                Console.error(e)
            }
        }

        return builder.build()
    }
}