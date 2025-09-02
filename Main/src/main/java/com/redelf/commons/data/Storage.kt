@file:Suppress("UNCHECKED_CAST")

package com.redelf.commons.data

import androidx.room.concurrent.AtomicBoolean
import com.redelf.commons.extensions.CountDownLatch
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object Storage {

    val DEBUG = AtomicBoolean()

    fun <T> put(key: String, value: T?): Boolean {

        return DataManagement.STORAGE.push(key, value)
    }

    fun <T> get(key: String, from: String): T? {

        val tag = "Storage :: Get :: Key='$key' ::"

        if (DEBUG.get()) {

            Console.log("$tag START")
        }

        val ctx = "Storage.get.$key"

        val result: T? = sync(

            ctx,
            "$ctx.(from='$from')"

        ) { callback ->

            DataManagement.STORAGE.pull(

                key,
                callback
            )
        }

        Console.log("$tag END :: Has value = ${result != null}")

        return result
    }

    fun <T> get(key: String, defaultValue: T, from: String): T {

        val tag = "Storage :: Get (w.def) :: Key='$key' ::"

        if (DEBUG.get()) {

            Console.log("$tag START")
        }

        val ctx = "Storage.get.$key"

        val result = sync(

            ctx,
            "$ctx.(from='$from')"

        ) { callback ->

            DataManagement.STORAGE.pull(key, callback)

        } ?: defaultValue

        if (DEBUG.get()) {

            Console.log("$tag END :: Has value = ${result != null}")
        }

        return result
    }

    fun delete(key: String): Boolean {

        var result = false
        val latch = CountDownLatch(1, "Storage.delete")

        DataManagement.STORAGE.delete(

            key,

            object : OnObtain<Boolean?> {

                override fun onCompleted(data: Boolean?) {

                    result = data == true

                    latch.countDown()
                }

                override fun onFailure(error: Throwable) {

                    recordException(error)

                    latch.countDown()
                }
            }
        )

        try {

            if (!latch.await(2, TimeUnit.SECONDS)) {

                val e = TimeoutException("Storage latch expired")
                recordException(e)
            }

        } catch (e: Throwable) {

            recordException(e)
        }

        return result
    }

    fun deleteAll(): Boolean {

        return DataManagement.STORAGE.erase()
    }

    fun contains(key: String, from: String): Boolean {

        val ctx = "Storage.contains.$key"

        return sync(

            ctx,
            "$ctx.(from='$from')"

        ) { callback ->

            DataManagement.STORAGE.contains(

                key, callback
            )

        } == true
    }
}
