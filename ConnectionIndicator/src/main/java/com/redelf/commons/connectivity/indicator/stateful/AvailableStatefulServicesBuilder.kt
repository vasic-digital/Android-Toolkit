package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableServiceSet
import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.Builder
import com.redelf.commons.extensions.isOnMainThread

class AvailableStatefulServicesBuilder<T> : Builder<Set<AvailableStatefulService<T>>> {

    private val factory = AvailableStatefulServiceFactory<T>()
    private val services = mutableSetOf<AvailableStatefulService<T>>()

    @Throws(

        IllegalArgumentException::class
    )
    fun addService(clazz: Class<*>): Builder<Set<AvailableStatefulService<T>>> {


        val instance = factory.build(clazz) // TODO: Add exception handling for upcoming changes - #Availability
        services.add(instance)

        return this
    }

    @Throws(

        IllegalArgumentException::class
    )
    fun addService(set: AvailableServiceSet): Builder<Set<AvailableStatefulService<T>>> {

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
    override fun build(): Set<AvailableStatefulService<T>> {

        if (isOnMainThread()) {

            throw IllegalArgumentException("You can't build AvailableServices on main thread")
        }

        return services
    }
}