package com.redelf.commons.creation.instantiation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.desription.Subject
import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain

abstract class SingleInstance<T> :

    Instantiable<T>,
    Obtain<T>,
    Resettable,
    Subject

{

    @Transient
    @JsonIgnore
    @JsonProperty("instance")
    @SerializedName("instance")
    private var instance: T? = null

    @Throws(InstantiationException::class)
    override fun obtain(): T {

        if (instance == null) {

            instance = instantiate()
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

    override fun reset(): Boolean {

        var prefix = ""
        instance?.let {

            prefix = "${it::class.simpleName} :: ${it.hashCode()} :: "
        }
        val tag = "${prefix}Reset ::"

        Console.log("$tag START")

        instance?.let {

            Console.log("$tag To lock")

            if (it is Lockable) {

                it.lock()

                Console.log("$tag Locked")
            }
        }

        val newInstance = instantiate()

        Console.log("$tag New instance: ${newInstance.hashCode()}")

        val result = newInstance != instance
        instance = newInstance

        Console.log("$tag New instance confirmed: ${instance.hashCode()}")

        if (result) {

            Console.log("$tag END")

        } else {

            Console.error("$tag END: Instance was not changed")
        }

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