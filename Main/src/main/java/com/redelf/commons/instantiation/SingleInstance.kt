package com.redelf.commons.instantiation

import com.redelf.commons.obtain.Obtain
import com.redelf.commons.reset.Resettable

abstract class SingleInstance<T> : Instantiable<T>, Obtain<T>, Resettable {

    private var instance: T? = null

    @Throws(InstantiationException::class)
    override fun obtain(): T {

        if (instance == null) {

            instance = instantiate()
        }

        instance?.let {

            return it
        }

        throw InstantiationException("Object is null")
    }

    override fun reset(): Boolean {

        val newManager = instantiate()
        val result = newManager != instance
        instance = newManager

        return result
    }
}