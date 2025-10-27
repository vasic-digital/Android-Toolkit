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


package com.redelf.commons.callback

import com.redelf.commons.Debuggable
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class Callbacks<T>(private val identifier: String) : Registration<T>, Debuggable {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val debug = AtomicBoolean(DEBUG.get())
    private var callbacks = LinkedBlockingQueue<T>()
    private val tag = "Callbacks '${getTagName()}' ::"

    private fun getTagName() = "$identifier ${hashCode()}"

    fun getTag() = tag

    override fun register(subscriber: T) {

        val tag = "$tag ON  ::"

        if (isDebug()) Console.log(

            "$tag Start :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )

        val iterator = callbacks.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item == null) {

                Console.warning("$tag Releasing null pointing reference")
                iterator.remove()

            } else if (item === subscriber) {

                Console.warning("$tag Already subscribed: ${subscriber.hashCode()}")

                return
            }
        }

        callbacks.add(subscriber)

        if (isDebug()) Console.debug(

            "$tag Subscriber registered: ${subscriber.hashCode()}"
        )

        if (isDebug()) Console.log(

            "$tag End :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )
    }

    override fun unregister(subscriber: T) {

        val tag = "$tag OFF ::"

        if (isDebug()) Console.log(

            "$tag Start :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )

        val iterator = callbacks.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item == null || item === subscriber) {

                if (item == null) {

                    Console.warning("$tag Releasing null pointing reference")

                } else {

                    if (isDebug()) Console.debug(

                        "$tag Subscriber unregistered: ${subscriber.hashCode()}"
                    )
                }

                iterator.remove()
            }
        }

        if (isDebug()) Console.log(

            "$tag End :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )
    }

    override fun isRegistered(subscriber: T): Boolean {

        val iterator: Iterator<T> = callbacks.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item === subscriber) {

                return true
            }
        }
        return false
    }

    fun isRegistered() = callbacks.isNotEmpty()

    fun doOnAll(operation: CallbackOperation<T>, operationName: String) {

        var count = 0
        val iterator = callbacks.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item == null) {

                Console.warning("$operationName releasing null pointing reference")
                iterator.remove()

            } else {

                if (isDebug()) Console.debug(

                    "$operationName performing operation for subscriber: ${item.hashCode()}"
                )

                operation.perform(item)
                count++
            }
        }

        if (count > 0) {

            if (isDebug()) Console.debug(

                "$operationName performed for $count subscribers"
            )

        } else {

            if (isDebug()) Console.log("$operationName performed for no subscribers")
        }
    }

    fun hasSubscribers() = callbacks.isNotEmpty()

    fun size() = callbacks.size

    fun getSubscribersCount() = size()

    fun getSubscribers() : List<T> {

        val list = mutableListOf<T>()

        list.addAll(callbacks)

        return list
    }

    fun clear() {

        callbacks.clear()
    }

    override fun setDebug(debug: Boolean) {

        this.debug.set(debug)
    }

    
    override fun isDebug(): Boolean {

        return debug.get() || DEBUG.get()
    }
}