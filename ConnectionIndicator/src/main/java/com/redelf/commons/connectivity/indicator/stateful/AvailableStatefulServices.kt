package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.stateful.State
import com.redelf.commons.stateful.Stateful
import java.util.concurrent.ConcurrentHashMap

class AvailableStatefulServices
@Throws(IllegalArgumentException::class)
constructor(builder: AvailableStatefulServicesBuilder) : AvailableService, TerminationAsync {

    private class LocalStateful<T>(val service: AvailableStatefulService<T>) : Stateful<T> {

        // TODO: Implement notifying mechanism - #Availability

        override fun onStateChanged() {

            Console.log("onStateChanged")
        }

        override fun getState(): State<T> {

            return service.getState()
        }

        override fun setState(state: State<T>) {

            service.setState(state)
        }

        override fun onState(state: State<T>) {

            Console.log("onState")
        }
    }

    private val services:
            ConcurrentHashMap<AvailableStatefulService<*>, Stateful<*>> = ConcurrentHashMap()

    init {

        services.clear()

        builder.build().forEach {

            addService(it)
        }

        if (services.isEmpty()) {

            throw IllegalArgumentException("No services provided")
        }
    }

    fun <T> addService(service: AvailableStatefulService<T>) {

        val listener = LocalStateful(service)

        services[service] = listener

        service.register(listener)
    }

    fun <T> removeService(service: AvailableStatefulService<T>) {

        services.remove(service)?.let {

            service.unregister(service)
        }
    }

    fun hasService(service: AvailableService): Boolean {

        return services.contains(service)
    }

    fun getServices(): List<AvailableService> {

        return services.keys().toList()
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