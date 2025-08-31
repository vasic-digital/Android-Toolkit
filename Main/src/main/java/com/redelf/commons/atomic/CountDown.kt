package com.redelf.commons.atomic

import com.redelf.commons.logging.Console
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class CountDown(

    context: String,
    private val count: Int,
    private val timeoutInSeconds: Long = 60,
    private val latch: CountDownLatch = CountDownLatch(count)

) {

    companion object {

        val DEBUG = AtomicBoolean(false)
    }

    private val counted: AtomicInteger = AtomicInteger()

    private val tag = if (context.isEmpty()) {

        "Count down :: ${hashCode()} :: Count = $count ::"

    } else {

        "Count down :: ${hashCode()} :: Context='$context' :: Count  =$count ::"
    }

    fun await(): Boolean {

        return await(timeoutInSeconds, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun await(howMuch: Long, unit: java.util.concurrent.TimeUnit): Boolean {

        log("Await :: START")

        if (counted.get() > 0) {

            warning("Await :: ALREADY COUNTED DOWN :: Counted = ${counted.get()} of $count")
        }

        try {

            val success = latch.await(howMuch, unit)

            if (success) {

                log("Await :: END")

                return true

            } else {

                error("Await :: TIMEOUT")
            }

        } catch (e: Throwable) {

            error("Await :: ERROR=${e.message ?: e.javaClass.simpleName}")
        }

        return false
    }

    fun countDown() {

        latch.countDown()

        val c = counted.incrementAndGet()

        log("Count down :: Counted = $c of $count")
    }

    private fun log(message: String) {

        if (DEBUG.get()) {

            Console.log("$tag $message")
        }
    }

    private fun warning(message: String) {

        Console.warning("$tag $message")
    }

    private fun error(message: String) {

        Console.error("$tag $message")
    }
}