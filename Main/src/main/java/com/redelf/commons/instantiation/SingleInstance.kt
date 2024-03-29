package com.redelf.commons.instantiation

import com.redelf.commons.desription.Subject
import com.redelf.commons.isNotEmpty
import com.redelf.commons.locking.Lockable
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.reset.Resettable

abstract class SingleInstance<T> :

    Instantiable<T>,
    Obtain<T>,
    Resettable,
    Subject

{

    @Transient
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

        instance?.let {

            if (it is Lockable) {

                it.lock()
            }
        }

        val newManager = instantiate()
        val result = newManager != instance
        instance = newManager

        return result
    }

    override fun getWho(): String? {

        if (instance is Subject) {

            val who = (instance as Subject).getWho()

            who?.let {

                if (isNotEmpty(it)) {

                    return it
                }
            }
        }

        instance?.let { inst ->

            return inst::class.simpleName
        }

        return javaClass.simpleName
    }
}