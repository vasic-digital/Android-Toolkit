package com.redelf.commons.instantiation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.desription.Subject
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Timber
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

        Timber.v("$tag START")

        instance?.let {

            Timber.v("$tag To lock")

            if (it is Lockable) {

                it.lock()

                Timber.v("$tag Locked")
            }
        }

        val newManager = instantiate()

        Timber.v("$tag New instance: ${newManager.hashCode()}")

        val result = newManager != instance
        instance = newManager

        Timber.v("$tag New instance confirmed: ${instance.hashCode()}")

        if (result) {

            Timber.v("$tag END")

        } else {

            Timber.e("$tag END: Instance was not changed")
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