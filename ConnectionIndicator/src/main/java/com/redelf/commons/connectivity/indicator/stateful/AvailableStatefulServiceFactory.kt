package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.creation.BuilderParametrized
import java.lang.IllegalArgumentException

class AvailableStatefulServiceFactory<T> :
    BuilderParametrized<Class<*>, AvailableStatefulService<T>>

{

    // TODO: Recipes and recipes registration(s)

    @Throws(IllegalArgumentException::class)
    override fun build(input: Class<*>): AvailableStatefulService<T> {

        when (input) {

            else -> {

                val msg = "Not supported service class: ${input.simpleName}"

                throw IllegalArgumentException(msg)
            }
        }
    }
}