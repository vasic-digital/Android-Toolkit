package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.stateful.GetState
import com.redelf.commons.stateful.State
import java.util.concurrent.CopyOnWriteArraySet

class AvailableStatefulServices
@Throws(IllegalArgumentException::class)
constructor(

    builder: AvailableStatefulServicesBuilder

) : AvailableService, TerminationAsync, GetState<Int> {

    private val tag: String = "Available stateful services ::"

    private val services: CopyOnWriteArraySet<AvailableStatefulService> = CopyOnWriteArraySet()

    init {

        services.clear()

        builder.build().forEach {

            addService(it)
        }

        if (services.isEmpty()) {

            throw IllegalArgumentException("No services provided")
        }
    }

    fun addService(service: AvailableStatefulService) {

        services.add(service)
    }

    fun removeService(service: AvailableStatefulService) {

        services.remove(service)
    }

    fun hasService(service: AvailableService): Boolean {

        return services.contains(service)
    }

    fun getServiceInstances(): List<AvailableService> {

        return services.toList()
    }

    fun getServiceClasses(): List<Class<*>> {

        val items = mutableSetOf<Class<*>>()

        services.forEach { service ->

            items.add(service::class.java)
        }

        return items.toList()
    }

    override fun getState(): State<Int> {

        if (services.isEmpty()) {

            return ConnectionState.Unavailable
        }

        var failed = 0

        services.forEach { service ->

            if (service.getState() != ConnectionState.Connected) {

                failed++
            }
        }

        if (failed == 0) {

            return ConnectionState.Connected
        }

        if (failed == services.size) {

            return ConnectionState.Disconnected
        }

        return ConnectionState.Warning
    }

    fun getState(clazz: Class<*>): State<Int> {

        val name = clazz.simpleName

        services.forEach { service ->

            if (service::class.simpleName == name) {

                return service.getState()
            }
        }

        return ConnectionState.Unavailable
    }

    override fun terminate() {

        val tag = "$tag Termination ::"

        Console.log("$tag START")

        services.forEach { service ->

            if (service is TerminationAsync) {

                Console.log("$tag Service = ${service::class.simpleName}")

                service.terminate()
            }

            if (service is TerminationSynchronized) {

                Console.log("$tag Service = ${service::class.simpleName}")

                service.terminate()
            }
        }

        services.clear()

        Console.log("$tag END")
    }

    override fun getWho() = "Available services"
}