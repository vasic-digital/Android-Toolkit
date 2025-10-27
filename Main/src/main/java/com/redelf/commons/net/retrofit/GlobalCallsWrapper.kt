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


package com.redelf.commons.net.retrofit

import com.redelf.commons.interruption.Abort
import com.redelf.commons.logging.Console
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

object GlobalCallsWrapper : Abort {

    val CALLS = ConcurrentHashMap<String, Call>()

    override fun abort() {

        val tag = "GlobalCallsWrapper :: Abort ::"

        Console.log("$tag START")

        CALLS.forEach { (k, v) ->

            Console.log("$tag Cancel :: $k")

            try {

                v.cancel()

            } catch (e: Throwable) {

                Console.error("$tag Cancel failed: $k", e)
            }
        }

        CALLS.clear()

        Console.log("$tag END")
    }
}