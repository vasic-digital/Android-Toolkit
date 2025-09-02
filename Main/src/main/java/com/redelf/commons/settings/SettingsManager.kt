package com.redelf.commons.settings

import com.redelf.commons.context.ContextualManager
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.loading.Loadable
import com.redelf.commons.management.DataPushResult
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.atomic.AtomicBoolean

class SettingsManager private constructor() :

    Loadable,
    SingleInstantiated,
    SettingsManagement,
    ContextualManager<Settings>() {

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

    override fun reset(arg: String, callback: OnObtain<Boolean?>) {

        loaded.set(false)

        super.reset("${getWho()}.reset(from='$arg')", callback)
    }

    override fun isLazyReady() = loaded.get()

    override fun load() = loaded.set(true)

    override fun isLoaded() = isLazyReady()

    override fun <T> put(key: String, value: T, callback: OnObtain<Boolean>) {

        when (value) {

            is Boolean -> {

                putBoolean(key, value, callback)
                return
            }

            is String -> {

                putString(key, value, callback)
                return
            }
        }

        callback.onCompleted(false)
    }

    fun <T> get(key: String, defaultValue: T): T {

        if (isOnMainThread()) {

            val e = IllegalStateException("Obtain settings value from the main thread")
            recordException(e)
        }

        return sync("settings.get.$key", "${getWho()}.getWithDefault") { callback ->

            get(key, defaultValue, callback)

        } ?: defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, defaultValue: T, callback: OnObtain<T>) {

        when (defaultValue) {

            is Boolean -> {

                getBoolean(

                    key, defaultValue,

                    object : OnObtain<Boolean> {

                        override fun onCompleted(data: Boolean) {

                            callback.onCompleted(data as T)
                        }

                        override fun onFailure(error: Throwable) {

                            callback.onFailure(error)
                        }
                    }
                )

                return
            }

            is String -> {

                getString(

                    key, defaultValue,

                    object : OnObtain<String> {

                        override fun onCompleted(data: String) {

                            callback.onCompleted(data as T)
                        }

                        override fun onFailure(error: Throwable) {

                            callback.onFailure(error)
                        }
                    }
                )

                return
            }
        }

        callback.onCompleted(defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean, callback: OnObtain<Boolean>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            it.flags?.set(key, value)

                            apply(

                                it,

                                "putBoolean.$key",

                                true,

                                object : OnObtain<DataPushResult?> {

                                    override fun onCompleted(data: DataPushResult?) {

                                        callback.onCompleted(data?.success == true)
                                    }

                                    override fun onFailure(error: Throwable) {

                                        callback.onFailure(error)
                                    }
                                }
                            )
                        }

                        if (data == null) {

                            callback.onCompleted(false)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }

    override fun putString(key: String, value: String, callback: OnObtain<Boolean>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            it.values?.set(key, value)

                            apply(

                                it,

                                "putString.$key",

                                true,

                                object : OnObtain<DataPushResult?> {

                                    override fun onCompleted(data: DataPushResult?) {

                                        callback.onCompleted(data?.success == true)
                                    }

                                    override fun onFailure(error: Throwable) {

                                        callback.onFailure(error)
                                    }
                                }
                            )
                        }

                        if (data == null) {

                            callback.onCompleted(false)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean, callback: OnObtain<Boolean>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            val result = it.flags?.get(key) ?: defaultValue

                            callback.onCompleted(result)
                        }

                        if (data == null) {

                            callback.onCompleted(false)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }


    override fun getString(key: String, defaultValue: String, callback: OnObtain<String>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            val result = it.values?.get(key) ?: defaultValue

                            callback.onCompleted(result)
                        }

                        if (data == null) {

                            callback.onCompleted("")
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }

    override fun getLong(key: String, defaultValue: Long, callback: OnObtain<Long>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            val result = it.numbers?.get(key) ?: defaultValue

                            callback.onCompleted(result)
                        }

                        if (data == null) {

                            callback.onCompleted(0)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }

    override fun putLong(key: String, value: Long, callback: OnObtain<Boolean>) {

        try {

            obtain(

                object : OnObtain<Settings?> {

                    override fun onCompleted(data: Settings?) {

                        data?.let {

                            it.numbers?.set(key, value)

                            apply(

                                it,

                                "putLong.$key",

                                true,

                                object : OnObtain<DataPushResult?> {

                                    override fun onCompleted(data: DataPushResult?) {

                                        callback.onCompleted(data?.success == true)
                                    }

                                    override fun onFailure(error: Throwable) {

                                        callback.onFailure(error)
                                    }
                                }
                            )
                        }

                        if (data == null) {

                            callback.onCompleted(false)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        callback.onFailure(error)
                    }
                }
            )

        } catch (e: Throwable) {

            callback.onFailure(e)
        }
    }
}