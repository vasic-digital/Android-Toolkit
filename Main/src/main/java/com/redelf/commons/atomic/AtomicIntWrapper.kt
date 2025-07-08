package com.redelf.commons.atomic

import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicInteger

class AtomicIntWrapper(

    context: String,
    private val atomicInt: AtomicInteger

) {

    private val tag = "$context :: ${this::class.simpleName} " +
            ":: ${hashCode()} :: ${atomicInt.hashCode()} ::"

    fun incrementAndGet(): Int {

        val value = atomicInt.incrementAndGet()

        Console.log("$tag Incremented to: $value")

        return value
    }

    fun decrementAndGet(): Int {

        val value = atomicInt.decrementAndGet()

        Console.log("$tag Decremented to: $value")

        return value
    }

    fun set(value: Int): Int {

        atomicInt.set(value)

        val value = get()

        Console.log("$tag Set to: $value")

        return value
    }

    fun get(): Int {

        val value = atomicInt.get()

        Console.log("$tag Value of: $value")

        return value
    }
}