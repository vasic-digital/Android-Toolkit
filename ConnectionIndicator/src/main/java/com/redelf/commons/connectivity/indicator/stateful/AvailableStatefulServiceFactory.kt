/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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

            throw IllegalArgumentException("The class must have a simple name")
        }

        recipes[sName] = subscriber
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