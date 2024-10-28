package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.implementation.FCMConnectionAvailabilityService
import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.BuilderParametrized
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.registration.Registration
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap

class AvailableStatefulServiceFactory @Throws(IllegalArgumentException::class) constructor(

    private val origin: String

) :

    BuilderParametrized<Class<*>, AvailableStatefulService>,
    Registration<AvailableStatefulServiceFactoryRecipe>

{

    private val recipes = ConcurrentHashMap<String, Obtain<AvailableStatefulService>>()

    init {

        register(

            AvailableStatefulServiceFactoryRecipe(

                InternetConnectionAvailabilityService::class.java,
                InternetConnectionAvailabilityService.getObtainer(origin)
            )
        )

        register(

            AvailableStatefulServiceFactoryRecipe(

                FCMConnectionAvailabilityService::class.java,
                FCMConnectionAvailabilityService.getObtainer(origin)
            )
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun register(subscriber: AvailableStatefulServiceFactoryRecipe) {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException("The class must have a simple name")
        }

        recipes[sName] = subscriber.obtain
    }

    @Throws(IllegalArgumentException::class)
    override fun unregister(subscriber: AvailableStatefulServiceFactoryRecipe) {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException("The class must have a simple name")
        }

        recipes.remove(sName)
    }

    @Throws(IllegalArgumentException::class)
    override fun isRegistered(subscriber: AvailableStatefulServiceFactoryRecipe): Boolean {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException("The class must have a simple name")
        }

        return recipes.containsKey(sName)
    }

    @Throws(IllegalArgumentException::class)
    override fun build(input: Class<*>): AvailableStatefulService {

        val identifier = input.simpleName

        if (isEmpty(identifier)) {

            throw IllegalArgumentException("The class must have a simple name")
        }

        recipes[identifier]?.let {

            return it.obtain()
        }

        throw IllegalArgumentException("Not supported service with the identifier of: $input")
    }
}