@file:Suppress("UNCHECKED_CAST")

package com.redelf.commons.data

import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object Storage {

    fun <T> put(key: String, value: T?): Boolean {

        return DataManagement.STORAGE.push(key, value)
    }

    fun <T> get(key: String): T? {

        var result: T? = null
        val latch = CountDownLatch(1)

        exec(

            onRejected = { e ->

                recordException(e)

                latch.countDown()
            }

        ) {

            DataManagement.STORAGE.pull<Any?>(

                key,

                object : OnObtain<Any?> {

                    override fun onCompleted(data: Any?) {

                        data?.let {

                            try {

                                result = data as T?

                            } catch (e: Throwable) {

                                recordException(e)
                            }
                        }

                        latch.countDown()
                    }

                    override fun onFailure(error: Throwable) {

                        recordException(error)

                        latch.countDown()
                    }
                }
            )
        }

        try {

            if (!latch.await(10, TimeUnit.SECONDS)) {

                val e = TimeoutException("Storage latch expired")
                recordException(e)
            }

        } catch (e: Throwable) {

            recordException(e)
        }

        return result
    }

    fun <T> get(key: String, defaultValue: T): T {

        var result: T = defaultValue
        val latch = CountDownLatch(1)

        exec(

            onRejected = { e ->

                recordException(e)

                latch.countDown()
            }

        ) {

            DataManagement.STORAGE.pull<Any?>(

                key,

                object : OnObtain<Any?> {

                    override fun onCompleted(data: Any?) {

                        data?.let {

                            try {

                                result = data as T

                            } catch (e: Throwable) {

                                recordException(e)
                            }
                        }

                        if (data == null) {

                            result = defaultValue
                        }

                        latch.countDown()
                    }

                    override fun onFailure(error: Throwable) {

                        recordException(error)

                        latch.countDown()
                    }
                }
            )
        }

        try {

            if (!latch.await(10, TimeUnit.SECONDS)) {

                val e = TimeoutException("Storage latch expired")
                recordException(e)
            }

        } catch (e: Throwable) {

            recordException(e)
        }

        return result
    }

    fun delete(key: String): Boolean {

        var result = false
        val latch = CountDownLatch(1)

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

    fun contains(key: String): Boolean {

        var result = false
        val latch = CountDownLatch(1)

        DataManagement.STORAGE.contains(

            key,

            object : OnObtain<Boolean> {

                override fun onCompleted(data: Boolean) {

                    result = data

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
}
