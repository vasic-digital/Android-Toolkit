package com.redelf.commons.interprocess.echo

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.redelf.commons.extensions.isOnMainThread
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.interprocess.InterprocessService
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain

class EchoService {

    // FIXME: Rewrite this

    companion object {

        const val EXTRA_DATA = "data"
        const val ACTION_ECHO = "com.redelf.commons.interprocess.echo"
        const val ACTION_HELLO = "com.redelf.commons.interprocess.echo.hello"
        const val ACTION_ECHO_RESPONSE = "com.redelf.commons.interprocess.echo.response"
    }

//    private val echo = "Echo"
//    override val tag = "IPC service :: $echo ::"
//
//    init {
//
//        actions = listOf(ACTION_ECHO, ACTION_HELLO)
//    }
//
//    fun hello() {
//
//        Console.log("$tag Supported actions: ${actions.joinToString()}")
//    }
//
//    override fun onIntent(intent: Intent) {
//
//        when (intent.action) {
//
//            ACTION_HELLO -> hello()
//
//            ACTION_ECHO -> echo(intent)
//        }
//    }
//
//    private fun echo(intent: Intent) {
//
//        val message = intent.getStringExtra(EXTRA_DATA)
//
//        Console.log("$tag Received echo request :: $message")
//
//        val responseIntent = Intent(ACTION_ECHO_RESPONSE)
//        responseIntent.putExtra(EXTRA_DATA, "$echo = $message")
//
//        applicationContext.sendBroadcast(responseIntent)
//
//        Console.log("$tag Sent echo response :: $message")
//    }
}