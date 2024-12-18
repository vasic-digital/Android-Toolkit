package com.redelf.commons.callback

import com.redelf.commons.Debuggable
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class Callbacks<T>(private val identifier: String) : Registration<T>, Debuggable {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val debug = AtomicBoolean(DEBUG.get())
    private var callbacks = ConcurrentLinkedQueue<T>()
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