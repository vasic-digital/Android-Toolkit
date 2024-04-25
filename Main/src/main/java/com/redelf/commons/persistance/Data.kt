package com.redelf.commons.persistance

import android.content.Context
import android.text.TextUtils
import com.redelf.commons.doExec
import com.redelf.commons.exec
import com.redelf.commons.execution.Execution
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.persistance.Facade.EmptyFacade
import com.redelf.commons.recordException
import timber.log.Timber
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException

object Data : TerminationSynchronized {

    private var facade: Facade? = EmptyFacade()

    fun init(

        context: Context,
        storageTag: String? = null,
        salter: Salter? = null

    ): PersistenceBuilder {

        PersistenceUtils.checkNull("Context", context)
        facade = null

        if (!TextUtils.isEmpty(storageTag) && storageTag != null) {

            salter?.let {

                return PersistenceBuilder(context, storageTag = storageTag, salter = it)
            }

            return PersistenceBuilder(context, storageTag = storageTag)
        }

        salter?.let {

            return PersistenceBuilder(context, salter = it)
        }

        return PersistenceBuilder(context)
    }

    fun build(persistenceBuilder: PersistenceBuilder) {

        DefaultFacade.initialize(persistenceBuilder)

        facade = DefaultFacade
    }

    override fun shutdown(): Boolean {

        val res = facade?.shutdown() ?: false
        facade = null
        return res
    }

    fun <T> put(key: String?, value: T): Boolean = facade?.put(key, value) ?: false

    operator fun <T> get(key: String?): T? = facade?.get(key)

    operator fun <T> get(key: String?, defaultValue: T): T {

        return facade?.get(key, defaultValue) ?: defaultValue
    }

    fun count(): Long = facade?.count() ?: 0

    fun delete(key: String?): Boolean = facade?.delete(key) ?: false

    operator fun contains(key: String?): Boolean = facade?.contains(key) ?: false

    val isBuilt: Boolean = facade?.isBuilt ?: false

    /*
         DANGER ZONE:
    */
    fun destroy() {

        facade?.destroy()
    }

    fun deleteAll(): Boolean {

        return facade?.deleteAll() ?: false
    }
}

