package com.redelf.commons.atomic

import com.redelf.commons.logging.Console
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class Countdown(

    context: String,
    private val count: Int,
    private val timeoutInSeconds: Long = 60,
    private val latch: CountDownLatch = CountDownLatch(count)

) {

    companion object {

        val DEBUG = AtomicBoolean(false)
    }

    private val tag = "Countdown :: ${hashCode()} :: Context='$context' ::"

    fun await(): Boolean {

        log("Await :: START")

        try {

            val success = latch.await(timeoutInSeconds, java.util.concurrent.TimeUnit.SECONDS)

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

        log("Count down $latch")
    }

    private fun log(message: String) {

        if (DEBUG.get()) {

            Console.log("$tag $message")
        }
    }

    private fun error(message: String) {

        Console.error("$tag $message")
    }
}