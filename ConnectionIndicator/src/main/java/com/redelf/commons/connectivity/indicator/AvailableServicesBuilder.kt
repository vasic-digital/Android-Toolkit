package com.redelf.commons.connectivity.indicator

import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.Builder
import com.redelf.commons.extensions.isOnMainThread

class AvailableServicesBuilder : Builder<Set<AvailableService>> {

    private val services = mutableSetOf<AvailableService>()

    @Throws(IllegalStateException::class)
    @Suppress("DEPRECATION")
    fun <T> addService(clazz: Class<out AvailableService>): Builder<Set<AvailableService>>

        where T : AvailableService

    {

        val instance = clazz.newInstance()

        services.add(instance)

        return this
    }

    fun addService(set: AvailableServiceSet): Builder<Set<AvailableService>> {

        when (set) {

            AvailableServiceSet.DEFAULT -> {

                addService(InternetConnectionAvailabilityService::class.java)
            }

            else -> {

                addService(InternetConnectionAvailabilityService::class.java)
            }
        }

        return this
    }

    @Throws(IllegalArgumentException::class)
    override fun build(): Set<AvailableService> {

        if (isOnMainThread()) {

            throw IllegalArgumentException("You can't build AvailableServices on main thread")
        }

        return services
    }
}