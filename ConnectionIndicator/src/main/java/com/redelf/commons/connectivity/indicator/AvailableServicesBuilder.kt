package com.redelf.commons.connectivity.indicator

import com.redelf.commons.creation.Builder

class AvailableServicesBuilder : Builder<Set<AvailableService>> {

    private val services = mutableSetOf<AvailableService>()

    fun addService(service: AvailableService): Builder<Set<AvailableService>>{

        services.add(service)
        return this
    }

    override fun build(): Set<AvailableService> {

        return services
    }
}