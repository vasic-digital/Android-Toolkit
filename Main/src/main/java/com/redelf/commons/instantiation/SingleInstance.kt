package com.redelf.commons.instantiation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.desription.Subject
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.reset.Resettable

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

        val newManager = instantiate()

        Console.log("$tag New instance: ${newManager.hashCode()}")

        val result = newManager != instance
        instance = newManager

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