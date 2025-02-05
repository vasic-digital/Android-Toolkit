package com.redelf.commons.settings

import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.loading.Loadable
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

class SettingsManager private constructor() :

    Loadable,
    SingleInstantiated,
    SettingsManagement,
    ContextualManager<Settings>()

{

    companion object : SingleInstance<SettingsManager>() {

        override fun instantiate(vararg params: Any): SettingsManager {

            return SettingsManager()
        }
    }

    private val loaded = AtomicBoolean()

    override val storageKey = "main_settings"
    override val instantiateDataObject = true

    override fun getLogTag() = "SettingsManager :: ${hashCode()} :: $storageKey ::"

    override fun createDataObject() = Settings()

    override fun reset(): Boolean {

        loaded.set(false)

        return super.reset()
    }

    override fun isLazyReady() = loaded.get()

    override fun load() = loaded.set(true)

    override fun isLoaded() = isLazyReady()

    override fun <T> put(key: String, value: T): Boolean {

        when (value) {

            is Boolean -> {

                return putBoolean(key, value)
            }

            is String -> {

                return putString(key, value)
            }
        }

        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, defaultValue: T): T {

        when (defaultValue) {

            is Boolean -> {

                return (getBoolean(key, defaultValue) as T) ?: defaultValue
            }

            is String -> {

                return (getString(key, defaultValue) as T) ?: defaultValue
            }
        }

        return defaultValue
    }

    override fun putBoolean(key: String, value: Boolean): Boolean {

        try {

            val settings = obtain()

            settings?.let {

                it.flags?.set(key, value)
                pushData(it)

                return true
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return false
    }

    override fun putString(key: String, value: String): Boolean {

        try {

            val settings = obtain()

            settings?.let {

                it.values?.set(key, value)
                pushData(it)

                return true
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return false
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {

        try {

            val settings = obtain()

            settings?.let {

                return it.flags?.get(key) ?: defaultValue
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return defaultValue
    }



    override fun getString(key: String, defaultValue: String): String {

        try {

            val settings = obtain()

            settings?.let {

                return it.values?.get(key) ?: defaultValue
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {

        try {

            val settings = obtain()

            settings?.let {

                return it.numbers?.get(key) ?: defaultValue
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return defaultValue
    }

    override fun putLong(key: String, value: Long): Boolean {

        try {

            val settings = obtain()

            settings?.let {

                it.numbers?.set(key, value)
                pushData(it)

                return true
            }

        } catch (e: IllegalStateException) {

            Console.error(e)
        }

        return false
    }
}