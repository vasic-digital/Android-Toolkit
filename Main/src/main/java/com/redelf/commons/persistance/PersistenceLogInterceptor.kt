package com.redelf.commons.persistance

import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.base.Persistence
import java.util.concurrent.atomic.AtomicBoolean

object PersistenceLogInterceptor : LogInterceptor {

    /*
        TODO: Refactor - Move away from the static context access
    */
    val DEBUG = AtomicBoolean()

    override fun onLog(message: String?) {

        if (DEBUG.get()) Console.log("${Persistence.TAG} $message")
    }

    override fun onDebug(message: String?) {

        if (DEBUG.get()) Console.debug("${Persistence.TAG} $message")
    }

    override fun onError(message: String?) {

        Console.error("${Persistence.TAG} $message")
    }
}