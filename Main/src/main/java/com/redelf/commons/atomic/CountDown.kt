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

        "Count down :: ${hashCode()} :: Context='$context' :: Count = $count ::"
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