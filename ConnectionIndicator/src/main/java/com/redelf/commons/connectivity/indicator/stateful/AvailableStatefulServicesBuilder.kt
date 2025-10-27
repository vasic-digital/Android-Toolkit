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


package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableServiceSet
import com.redelf.commons.connectivity.indicator.connection.ConnectivityStateCallback
import com.redelf.commons.connectivity.indicator.implementation.FCMConnectionAvailabilityService
import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.Builder
import com.redelf.commons.extensions.isOnMainThread

class AvailableStatefulServicesBuilder(origin: String) : Builder<Set<AvailableStatefulService>> {

    private var debug = false
    private val factory = AvailableStatefulServiceFactory(origin)
    private val services = mutableSetOf<AvailableStatefulService>()
    private val callbacks = mutableSetOf<ConnectivityStateCallback>()

    @Throws(IllegalArgumentException::class)
    fun addService(clazz: Class<*>): AvailableStatefulServicesBuilder {

        val instance = factory.build(clazz)
        services.add(instance)

        return this
    }

    @Throws(IllegalArgumentException::class)
    fun addServicesSet(set: AvailableServiceSet): AvailableStatefulServicesBuilder {

        when (set) {

            AvailableServiceSet.DEFAULT -> {

                addService(InternetConnectionAvailabilityService::class.java)
                addService(FCMConnectionAvailabilityService::class.java)
            }

            AvailableServiceSet.INTERNET -> {

                addService(InternetConnectionAvailabilityService::class.java)
            }

            else -> {

                throw IllegalArgumentException("Unsupported service set: ${set.name}")
            }
        }

        return this
    }

    fun addCallback(

        callback: ConnectivityStateCallback

    ): AvailableStatefulServicesBuilder {

        callbacks.add(callback)

        return this
    }

    fun removeCallback(

        callback: ConnectivityStateCallback

    ): AvailableStatefulServicesBuilder {

        callbacks.remove(callback)

        return this
    }

    fun setDebug(value: Boolean): AvailableStatefulServicesBuilder {

        debug = value

        return this
    }

    @Throws(IllegalArgumentException::class)
    override fun build(): Set<AvailableStatefulService> {

        if (isOnMainThread()) {

            throw IllegalArgumentException("You can't build AvailableServices on main thread")
        }

        services.forEach { service ->

            callbacks.forEach { callback ->

                service.register(callback)
            }

            service.setDebug(debug)
        }

        return services
    }
}