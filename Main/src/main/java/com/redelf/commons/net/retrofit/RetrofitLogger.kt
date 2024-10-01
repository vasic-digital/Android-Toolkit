package com.redelf.commons.net.retrofit

import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

class RetrofitLogger : HttpLoggingInterceptor.Logger {

    override fun log(message: String) {

        Timber.v("Retrofit $message" )
    }
}