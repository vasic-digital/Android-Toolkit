package com.redelf.commons.persistance

import com.redelf.commons.logging.Timber

object PersistenceLogInterceptor : LogInterceptor {

    override fun onLog(message: String?) {

        Timber.v("${Persistence.tag} $message")
    }

    override fun onDebug(message: String?) {

        Timber.d("${Persistence.tag} $message")
    }

    override fun onError(message: String?) {

        Timber.e("${Persistence.tag} $message")
    }
}