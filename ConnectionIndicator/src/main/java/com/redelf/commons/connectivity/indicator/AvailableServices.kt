package com.redelf.commons.connectivity.indicator

import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.stateful.State
import com.redelf.commons.stateful.Stateful

class AvailableServices(private val services: MutableSet<AvailableService>) :

    AvailableService,
    TerminationAsync,
    Stateful<Any> // TODO: <---

{

    override fun onStateChanged() {

        TODO("Not yet implemented")
    }

    override fun getState(): State<Any> {

        TODO("Not yet implemented")
    }

    override fun setState(state: State<Any>) {

        TODO("Not yet implemented")
    }

    override fun onState(state: State<Any>) {

        TODO("Not yet implemented")
    }

    fun addService(service: AvailableService) {

        services.add(service)
    }

    fun removeService(service: AvailableService) {

        services.remove(service)
    }

    fun hasService(service: AvailableService): Boolean {

        return services.contains(service)
    }

    fun getServices(): List<AvailableService> {

        return services.toList()
    }

    override fun terminate() {

        services.forEach {

            if (it is TerminationAsync) {

                it.terminate()
            }

            if (it is TerminationSynchronized) {

                it.terminate()
            }
        }

        services.clear()
    }
}