package com.redelf.commons.connectivity.indicator

import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.stateful.State
import com.redelf.commons.stateful.Stateful
import java.util.concurrent.ConcurrentHashMap

class AvailableServices

@Throws(IllegalArgumentException::class)
constructor(builder: AvailableServicesBuilder) :

    AvailableService,
    TerminationAsync

{

    private class LocalStateful(val service: AvailableStatefulService<*>) : Stateful<Any> {

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
    }

    private val services:
            ConcurrentHashMap<AvailableStatefulService<*>, Stateful<Any>> = ConcurrentHashMap()

    init {

        services.clear()

        builder.build().forEach {

            if (it is AvailableStatefulService<*>) {

                addService(it)
            }
        }
    }

    fun addService(service: AvailableStatefulService<*>) {

        val listener = LocalStateful(service)

        services[service] = listener

        service.register(listener)
    }

    fun removeService(service: AvailableStatefulService<Any>) {

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