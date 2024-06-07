package com.redelf.commons.persistance

import com.redelf.commons.logging.Timber
import com.redelf.commons.persistance.base.Persistence
import java.util.concurrent.atomic.AtomicBoolean

object PersistenceLogInterceptor : LogInterceptor {

    /*
        TODO: Refactor - Move away from the static context access
    */
    val DEBUG = AtomicBoolean()

    override fun onLog(message: String?) {

        if (DEBUG.get()) Timber.v("${Persistence.TAG} $message")
    }

    override fun onDebug(message: String?) {

        if (DEBUG.get()) Timber.d("${Persistence.TAG} $message")
    }

    override fun onError(message: String?) {

        Timber.e("${Persistence.TAG} $message")
    }
}