package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.stateful.State
import com.redelf.commons.stateful.Stateful
import java.util.concurrent.ConcurrentHashMap

class AvailableStatefulServices<T>

@Throws(IllegalArgumentException::class) // TODO: Add exception handling for upcoming changes - #Availability
constructor(builder: AvailableStatefulServicesBuilder<T>) :

    AvailableService,
    TerminationAsync

{

    private class LocalStateful<T> (val service: AvailableStatefulService<T>) : Stateful<T> {

        override fun onStateChanged() { // TODO: Implement notifying mechanism - #Availability

            TODO("Not yet implemented")
        }

        override fun getState(): State<T> {

            TODO("Not yet implemented")
        }

        override fun setState(state: State<T>) {

            TODO("Not yet implemented")
        }

        override fun onState(state: State<T>) {

            TODO("Not yet implemented")
        }
    }

    private val services:
            ConcurrentHashMap<AvailableStatefulService<T>, Stateful<T>> = ConcurrentHashMap()

    init {

        services.clear()

        builder.build().forEach {

            addService(it)
        }
    }

    fun addService(service: AvailableStatefulService<T>) {

        val listener = LocalStateful(service)

        services[service] = listener

        service.register(listener)
    }

    fun removeService(service: AvailableStatefulService<T>) {

        services.remove(service)?.let {

            service.unregister(it)
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