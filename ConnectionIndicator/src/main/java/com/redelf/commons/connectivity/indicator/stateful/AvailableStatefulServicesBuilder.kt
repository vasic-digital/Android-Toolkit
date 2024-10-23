package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableServiceSet
import com.redelf.commons.connectivity.indicator.implementation.FCMConnectionAvailabilityService
import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.Builder
import com.redelf.commons.extensions.isOnMainThread

class AvailableStatefulServicesBuilder : Builder<Set<AvailableStatefulService<*>>> {

    private val factory = AvailableStatefulServiceFactory()
    private val services = mutableSetOf<AvailableStatefulService<*>>()

    @Throws(IllegalArgumentException::class)
    fun addService(clazz: Class<*>): Builder<Set<AvailableStatefulService<*>>> {

        val instance = factory.build(clazz)
        services.add(instance)

        return this
    }

    @Throws(IllegalArgumentException::class)
    fun addService(set: AvailableServiceSet): Builder<Set<AvailableStatefulService<*>>> {

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

    @Throws(IllegalArgumentException::class)
    override fun build(): Set<AvailableStatefulService<*>> {

        if (isOnMainThread()) {

            throw IllegalArgumentException("You can't build AvailableServices on main thread")
        }

        return services
    }
}