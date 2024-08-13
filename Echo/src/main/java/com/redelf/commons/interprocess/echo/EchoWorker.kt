package com.redelf.commons.interprocess.echo

import android.content.Context
import android.content.Intent
import androidx.work.WorkerParameters
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.interprocess.InterprocessWorker
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain

class EchoWorker(ctx: Context, params: WorkerParameters) : InterprocessWorker(ctx, params) {

    companion object : Obtain<EchoWorker> {

        private var WORKER: EchoWorker? = null

        const val EXTRA_DATA = "data"
        const val ACTION_ECHO = "com.redelf.commons.interprocess.echo"
        const val ACTION_HELLO = "com.redelf.commons.interprocess.echo.hello"
        const val ACTION_ECHO_RESPONSE = "com.redelf.commons.interprocess.echo.response"

        @Throws(IllegalStateException::class)
        override fun obtain(): EchoWorker {

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

    override fun doWork(): Result {

        return Result.success()
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