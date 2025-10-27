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


package com.redelf.commons.creation.instantiation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.desription.Subject
import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.locking.Lockable
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain

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
    protected var instance: T? = null

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