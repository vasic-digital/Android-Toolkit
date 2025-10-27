/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.messaging.firebase

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.defaults.ResourceDefaults
import com.redelf.commons.extensions.CountDownLatch
import com.redelf.commons.extensions.recordException
import com.redelf.commons.loading.Loadable
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataPushResult
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager :

    Loadable,
    ResourceDefaults,
    ContextualManager<FirebaseConfiguration>() {

    override val persist = false
    override val storageKey = "remote_configuration"

    private const val LOG_TAG = "FirebaseConfigurationManager ::"

    override fun getLogTag() = "$LOG_TAG ${hashCode()} ::"

    override fun getWho(): String? = FirebaseConfigurationManager::class.java.simpleName

    private val loaded = AtomicBoolean()

    override fun isLazyReady() = loaded.get()

    override fun isLoaded() = isLazyReady()

    override fun reset(arg: String, callback: OnObtain<Boolean?>) {

        super.reset(

            "${getWho()}.reset(from='$arg')",

            object : OnObtain<Boolean?> {

                override fun onCompleted(data: Boolean?) {

                    if (data == true) {

                        loaded.set(false)
                    }

                    callback.onCompleted(data == true)
                }

                override fun onFailure(error: Throwable) {

                    callback.onFailure(error)
                }
            }
        )
    }

    override fun load() {

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(6 * 60 * 60)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        Console.log("$LOG_TAG Config params fetching")

        val latch = CountDownLatch(1, "FirebaseConfigurationManager.load")

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val updated = task.result
                    val msg = "$LOG_TAG Config params updated: $updated"

                    if (updated) {

                        Console.debug(msg)

                    } else {

                        Console.log(msg)
                    }

                    val all = remoteConfig.all

                    Console.log("$LOG_TAG Config params obtained: $all")

                    val newMap = FirebaseConfiguration()
                    newMap.putAll(all)

                    apply(

                        newMap,

                        "remoteConfig.fetchAndActivate.success",

                        false,

                        object : OnObtain<DataPushResult?> {

                            override fun onCompleted(data: DataPushResult?) {

                                loaded.set(true)

                                latch.countDown()
                            }

                            override fun onFailure(error: Throwable) {

                                recordException(error)

                                loaded.set(true)

                                latch.countDown()
                            }
                        }
                    )

                } else {

                    Console.error("$LOG_TAG Config params update failed")

                    loaded.set(true)

                    latch.countDown()
                }

            }.addOnFailureListener {

                loaded.set(true)

                Console.error("$LOG_TAG Config params update failed (2)")

                latch.countDown()
            }

        try {

            if (latch.await(60, TimeUnit.SECONDS)) {

                Console.log("$LOG_TAG SUCCESS")

            } else {

                val e =
                    TimeoutException("Timeout-ed while waiting for firebase manager to load data")

                recordException(e)

                loaded.set(true)
            }

        } catch (e: Throwable) {

            loaded.set(true)

            recordException(e)
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun setDefaults(defaults: Int) {

        if (defaults <= 0) {

            throw IllegalArgumentException("Defaults must be a valid resource id")
        }

        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(defaults)
    }
}