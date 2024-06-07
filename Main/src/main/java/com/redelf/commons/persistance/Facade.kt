package com.redelf.commons.persistance

import android.content.Context
import com.redelf.commons.lifecycle.InitializationWithContext
import com.redelf.commons.lifecycle.ShutdownSynchronized
import com.redelf.commons.lifecycle.TerminationSynchronized

interface Facade : ShutdownSynchronized, TerminationSynchronized, InitializationWithContext {
    fun <T> put(key: String?, value: T): Boolean

    fun <T> get(key: String?): T?

    fun <T> get(key: String?, defaultValue: T): T?

    fun count(): Long

    fun deleteAll(): Boolean

    fun delete(key: String?): Boolean


    fun contains(key: String?): Boolean

    val isBuilt: Boolean

    fun destroy()

    class EmptyFacade : Facade {
        override fun <T> put(key: String?, value: T): Boolean {
            throwValidation()
            return false
        }

        override fun <T> get(key: String?): T? {
            throwValidation()
            return null
        }

        override fun <T> get(key: String?, defaultValue: T): T? {
            throwValidation()
            return null
        }

        override fun count(): Long {
            throwValidation()
            return 0
        }

        override fun deleteAll(): Boolean {
            throwValidation()
            return false
        }

        override fun delete(key: String?): Boolean {
            throwValidation()
            return false
        }

        override fun contains(key: String?): Boolean {
            throwValidation()
            return false
        }

        override fun isBuilt(): Boolean {
            return false
        }

        override fun destroy() {
            throwValidation()
        }

        override fun shutdown(): Boolean {
            return true
        }

        override fun terminate(): Boolean {
            return true
        }

        override fun initialize(ctx: Context) {
            // Ignore
        }

        private fun throwValidation() {
            throw IllegalStateException(
                "Data is not built. " +
                        "Please call build() and wait the initialisation finishes."
            )
        }
    }
}
