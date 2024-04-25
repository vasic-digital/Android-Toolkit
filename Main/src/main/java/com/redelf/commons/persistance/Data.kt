package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.TerminationSynchronized

class Data private constructor(private val facade: Facade):

    TerminationSynchronized,
    InitializationWithContext

{

    companion object {

        fun instantiate(persistenceBuilder: PersistenceBuilder): Data {

            val facade = DefaultFacade.initialize(persistenceBuilder)

            return Data(facade)
        }
    }

    override fun shutdown(): Boolean {

        return facade.shutdown()
    }

    override fun initialize(ctx: Context) {

        return facade.initialize(ctx)
    }

    fun <T> put(key: String?, value: T): Boolean = facade.put(key, value)

    operator fun <T> get(key: String?): T? = facade.get(key)

    operator fun <T> get(key: String?, defaultValue: T): T {

        return facade.get(key, defaultValue) ?: defaultValue
    }

    fun count(): Long = facade.count()

    fun delete(key: String?): Boolean = facade.delete(key)

    operator fun contains(key: String?): Boolean = facade.contains(key)

    /*
         DANGER ZONE:
    */
    fun destroy() {

        facade.destroy()
    }

    fun deleteAll(): Boolean {

        return facade.deleteAll()
    }
}

