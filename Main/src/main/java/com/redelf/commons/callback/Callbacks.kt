package com.redelf.commons.callback

import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class Callbacks<T>(private val identifier: String) : Registration<T> {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    val tag = "Callbacks '${getTagName()}' ::"

    private var callbacks = ConcurrentLinkedQueue<T>()

    private fun getTagName() = "$identifier ${hashCode()}"

    override fun register(subscriber: T) {

        val tag = "$tag ON  ::"

        if (DEBUG.get()) Console.log(

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

        if (DEBUG.get()) Console.debug(

            "$tag Subscriber registered: ${subscriber.hashCode()}"
        )

        if (DEBUG.get()) Console.log(

            "$tag End :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )
    }

    override fun unregister(subscriber: T) {

        val tag = "$tag OFF ::"

        if (DEBUG.get()) Console.log(

            "$tag Start :: ${subscriber.hashCode()} :: ${callbacks.size}"
        )

        val iterator = callbacks.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item == null || item === subscriber) {

                if (item == null) {

                    Console.warning("$tag Releasing null pointing reference")

                } else {

                    if (DEBUG.get()) Console.debug(

                        "$tag Subscriber unregistered: ${subscriber.hashCode()}"
                    )
                }

                iterator.remove()
            }
        }

        if (DEBUG.get()) Console.log(

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

                if (DEBUG.get()) Console.debug(

                    "$operationName performing operation for subscriber: ${item.hashCode()}"
                )

                operation.perform(item)
                count++
            }
        }

        if (count > 0) {

            if (DEBUG.get()) Console.debug(

                "$operationName performed for $count subscribers"
            )

        } else {

            if (DEBUG.get()) Console.log("$operationName performed for no subscribers")
        }
    }

    fun hasSubscribers() = callbacks.isNotEmpty()

    fun getSubscribersCount() = callbacks.size

    fun getSubscribers() : List<T> {

        val list = mutableListOf<T>()

        list.addAll(callbacks)

        return list
    }

    fun clear() {

        callbacks.clear()
    }
}