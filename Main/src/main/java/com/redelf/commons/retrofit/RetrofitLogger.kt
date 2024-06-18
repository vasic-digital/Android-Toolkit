package com.redelf.commons.retrofit

import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

class RetrofitLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {

        Timber.v(message)
    }
}