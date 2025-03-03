package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.TerminationAsync
import com.redelf.commons.lifecycle.TerminationSynchronized
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.registration.Registration
import com.redelf.commons.stateful.GetState
import com.redelf.commons.stateful.State
import java.util.concurrent.CopyOnWriteArraySet

class AvailableStatefulServices
@Throws(IllegalArgumentException::class)
constructor(

    builder: AvailableStatefulServicesBuilder

) : AvailableService, TerminationAsync, GetState<Int>, Registration<ConnectivityStateChanges> {

    private val tag: String = "Available stateful services ::"

    private val services: CopyOnWriteArraySet<AvailableStatefulService> = CopyOnWriteArraySet()

    init {

        services.clear()

        builder.build().forEach {

            addService(it)
        }

        if (services.toList().isEmpty()) {

            throw IllegalArgumentException("No services provided")
        }
    }

    override fun register(subscriber: ConnectivityStateChanges) {

        services.forEach { service ->

            service.register(subscriber)
        }
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        services.forEach { service ->

            service.unregister(subscriber)
        }
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        services.forEach { service ->

            if (service.isRegistered(subscriber)) {

                return true
            }
        }

        return false
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

        val tag = "$tag Get state ::"

        Console.log("$tag START")

        if (services.toList().isEmpty()) {

            Console.log("$tag No services")

            return ConnectionState.Unavailable
        }

        var failed = 0

        services.forEach { service ->

            if (service.getState() != ConnectionState.Connected) {

                Console.warning("$tag Service = ${service::class.simpleName} is not connected")

                failed++
            }
        }

        if (failed == 0) {

            Console.log("$tag All services are connected")

            return ConnectionState.Connected
        }

        if (failed == services.toList().size) {

            Console.log("$tag All services are disconnected")

            return ConnectionState.Disconnected
        }

        Console.log("$tag Some services are disconnected :: Disconnect count = $failed")

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

    override fun terminate(vararg args: Any) {

        val tag = "$tag Termination ::"
        val from = "$tag $args"

        Console.log("$tag START :: Args = ${args.joinToString()}")

        fun logServiceTermination(service: AvailableService) = Console.log(

            "$tag Service :: Termination :: OK :: ${service::class.simpleName} " +
                    "- ${service.hashCode()}"
        )

        fun logServiceSkipped(service: AvailableService) = Console.warning(

            "$tag Service :: Termination :: SKIPPED :: ${service::class.simpleName} " +
                    "- ${service.hashCode()}"
        )

        services.forEach { service ->

            /*
             * TODO: This snippet is repeated three times at least!
             *  We should move it into one single implementation and reuse!
             */
            if (service is TerminationAsync) {

                if (service is SingleInstantiated) {

                    logServiceSkipped(service)

                } else {

                    service.terminate("On dismiss")

                    logServiceTermination(service)
                }

            } else if (service is TerminationSynchronized) {

                if (service is SingleInstantiated) {

                    logServiceSkipped(service)

                } else {

                    service.terminate("On dismiss")

                    logServiceTermination(service)
                }

            } else {

                val msg = "Service cannot be terminated ${service.javaClass.simpleName}"
                val e = IllegalStateException(msg)
                recordException(e)

                Console.error("$tag ERROR :: $msg")
            }
        }

        services.clear()

        Console.log("$tag END")
    }

    override fun getWho() = "Available services"
}