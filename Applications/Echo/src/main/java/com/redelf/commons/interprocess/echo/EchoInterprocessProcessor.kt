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