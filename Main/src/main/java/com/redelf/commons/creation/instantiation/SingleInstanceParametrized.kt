package com.redelf.commons.creation.instantiation

import com.redelf.commons.obtain.ObtainParametrized

abstract class SingleInstanceParametrized<T> :

    SingleInstance<T>(),
    ObtainParametrized<T, Any>

{

    @Throws(InstantiationException::class)
    override fun obtain(): T {

        throw InstantiationException("This method should not be used without parameters")
    }

    @Throws(IllegalArgumentException::class)
    override fun obtain(param: Any): T {

        if (instance == null) {

            instance = instantiate(param)
        }

        instance?.let {

            if (it !is SingleInstantiated) {

                val msg = "${it::class.simpleName} " +
                        "does not implement ${SingleInstantiated::class.simpleName} " +
                        "interface"

                throw InstantiationException(msg)
            }

            return it
        }

        throw InstantiationException("Object is null")
    }
}