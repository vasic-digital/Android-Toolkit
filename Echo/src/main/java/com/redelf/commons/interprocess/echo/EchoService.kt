package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.interprocess.InterprocessService
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain

class EchoService : InterprocessService() {

    companion object : Obtain<EchoService> {

        private var WORKER: EchoService? = null

        const val EXTRA_DATA = "data"
        const val ACTION_ECHO = "com.redelf.commons.interprocess.echo"
        const val ACTION_HELLO = "com.redelf.commons.interprocess.echo.hello"
        const val ACTION_ECHO_RESPONSE = "com.redelf.commons.interprocess.echo.response"

        @Throws(IllegalStateException::class)
        override fun obtain(): EchoService {

            if (isOnMainThread()) {

                throw IllegalStateException("Cannot obtain Echo worker from the main thread.")
            }

            yieldWhile {

                WORKER == null || WORKER?.isReady() == false
            }

            return WORKER ?: throw IllegalStateException("Echo worker not ready")
        }
    }

    private val echo = "Echo"
    override val tag = "IPC Worker :: $echo ::"

    init {

        actions = listOf(ACTION_ECHO, ACTION_HELLO)

        WORKER = this

        Console.log("$tag Initialized")
    }

    override fun isReady(): Boolean {

        return super.isReady() && WORKER != null
    }

    override fun hello() {
        super.hello()

        Console.log("$tag Supported actions: ${actions.joinToString()}")
    }

    override fun onIntent(intent: Intent) {

        when (intent.action) {

            ACTION_HELLO -> hello()

            ACTION_ECHO -> echo(intent)
        }
    }

    private fun echo(intent: Intent) {

        val message = intent.getStringExtra(EXTRA_DATA)

        Console.log("$tag Received echo request :: $message")

        val responseIntent = Intent(ACTION_ECHO_RESPONSE)
        responseIntent.putExtra(EXTRA_DATA, "$echo = $message")

        applicationContext.sendBroadcast(responseIntent)

        Console.log("$tag Sent echo response :: $message")
    }
}