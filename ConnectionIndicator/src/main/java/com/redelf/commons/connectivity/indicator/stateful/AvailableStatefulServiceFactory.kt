package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.connectivity.indicator.implementation.FCMConnectionAvailabilityService
import com.redelf.commons.connectivity.indicator.implementation.InternetConnectionAvailabilityService
import com.redelf.commons.creation.BuilderParametrized
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.registration.Registration
import java.util.concurrent.ConcurrentHashMap

class AvailableStatefulServiceFactory @Throws(IllegalArgumentException::class) constructor(

    origin: String

) :

    BuilderParametrized<Class<*>, AvailableStatefulService>,
    Registration<AvailableStatefulServiceFactoryRecipe>

{

    companion object {
        private const val ERROR_MESSAGE_CLASS_MUST_HAVE_SIMPLE_NAME = "The class must have a simple name"
    }

    private val recipes = ConcurrentHashMap<String, AvailableStatefulServiceFactoryRecipe>()

    init {

        val internetServiceObtainer = InternetConnectionAvailabilityService.getObtainer(origin)

        register(

            AvailableStatefulServiceFactoryRecipe(

                clazz = InternetConnectionAvailabilityService::class.java,
                obtain = internetServiceObtainer
            )
        )

        register(

            AvailableStatefulServiceFactoryRecipe(

                clazz = FCMConnectionAvailabilityService::class.java,
                obtain = FCMConnectionAvailabilityService.getObtainer(origin),
                dependencies = listOf(internetServiceObtainer)
            )
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun register(subscriber: AvailableStatefulServiceFactoryRecipe) {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException(ERROR_MESSAGE_CLASS_MUST_HAVE_SIMPLE_NAME)
        }

        recipes[sName] = subscriber
    }

    @Throws(IllegalArgumentException::class)
    override fun unregister(subscriber: AvailableStatefulServiceFactoryRecipe) {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException(ERROR_MESSAGE_CLASS_MUST_HAVE_SIMPLE_NAME)
        }

        recipes.remove(sName)
    }

    @Throws(IllegalArgumentException::class)
    override fun isRegistered(subscriber: AvailableStatefulServiceFactoryRecipe): Boolean {

        val sName = subscriber.clazz.simpleName

        if (isEmpty(sName)) {

            throw IllegalArgumentException(ERROR_MESSAGE_CLASS_MUST_HAVE_SIMPLE_NAME)
        }

        return recipes.containsKey(sName)
    }

    
    @Throws(IllegalArgumentException::class)
    override fun build(input: Class<*>): AvailableStatefulService {

        val identifier = input.simpleName

        if (isEmpty(identifier)) {

            throw IllegalArgumentException(ERROR_MESSAGE_CLASS_MUST_HAVE_SIMPLE_NAME)
        }

        recipes[identifier]?.let {

            val instance = it.obtain.obtain()

            it.dependencies.forEach {

                val dependency = it.obtain()

                instance.chain(dependency)
            }

            return instance
        }

        throw IllegalArgumentException("Not supported service with the identifier of: $input")
    }
}