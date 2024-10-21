package com.redelf.commons.connectivity.indicator

import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.Builder
import com.redelf.commons.extensions.isOnMainThread

class AvailableServicesBuilder : Builder<Set<AvailableService>> {

    private val services = mutableSetOf<AvailableService>()

    @Suppress("DEPRECATION")
    @Throws(

        IllegalStateException::class,
        IllegalAccessException::class,
        InstantiationException::class
    )
    fun <T> addService(clazz: Class<out T>): Builder<Set<AvailableService>>

        where T : AvailableService

    {

        val instance = clazz.newInstance()

        services.add(instance)

        return this
    }

    @Throws(

        IllegalStateException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        IllegalArgumentException::class
    )
    fun addService(set: AvailableServiceSet): Builder<Set<AvailableService>> {

        when (set) {

            AvailableServiceSet.DEFAULT,
            AvailableServiceSet.INTERNET -> {

                addService(InternetConnectionAvailabilityService::class.java)
            }

            else -> {

                throw IllegalArgumentException("Unsupported service set: ${set.name}")
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