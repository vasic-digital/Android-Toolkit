package com.redelf.commons.connectivity.indicator

import java.util.PriorityQueue

abstract class AvailableServices(private val services: PriorityQueue<AvailableService>) {



    fun getServices(): List<AvailableService> {

        return services.toList()
    }
}