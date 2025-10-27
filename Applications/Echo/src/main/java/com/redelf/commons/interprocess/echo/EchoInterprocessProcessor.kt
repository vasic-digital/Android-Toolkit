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


package com.redelf.commons.interprocess.echo

import android.content.Context
import android.content.Intent
import com.redelf.commons.interprocess.InterprocessData
import com.redelf.commons.interprocess.InterprocessProcessor
import com.redelf.commons.logging.Console

class EchoInterprocessProcessor(private val ctx: Context) : InterprocessProcessor() {

    companion object {

        const val ACTION_ECHO = "com.redelf.commons.interprocess.echo"
        const val ACTION_HELLO = "com.redelf.commons.interprocess.echo.hello"
        const val ACTION_ECHO_RESPONSE = "com.redelf.commons.interprocess.echo.response"
    }

    private val echo = "Echo"
    private val tag = "IPC :: Processor :: $echo ::"

    init {

        Console.log("$tag Created")
    }

    override fun onData(data: InterprocessData) {

        Console.log("$tag Received data: $data")

        val function = data.function

        when (function) {

            ACTION_HELLO -> hello()

            ACTION_ECHO -> echo(data.content ?: "")
        }
    }

    private fun hello() {

        Console.log("$tag Hello from the Echo IPC")
    }

    private fun echo(message: String) {

        Console.log("$tag Request :: $message")

        val responseIntent = Intent(ACTION_ECHO_RESPONSE)
        responseIntent.putExtra(InterprocessData.BUNDLE_KEY, "$echo = $message")
        ctx.applicationContext.sendBroadcast(responseIntent)

        Console.log("$tag Response :: $message")
    }
}