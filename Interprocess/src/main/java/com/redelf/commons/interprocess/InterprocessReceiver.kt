package com.redelf.commons.interprocess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.redelf.commons.logging.Console

class InterprocessReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION = "com.redelf.commons.interprocess.action"
    }

    private val tag = "IPC :: Receiver ::"

    init {

        Console.log("$tag Created")
    }

    init {

        Console.log("$tag Created")
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        Console.log("$tag Received intent")

        intent?.let {

            if (it.action == ACTION) {

                Console.log("$tag Intent action :: ${it.action}")

                Interprocessor.onIntent(it)
            }
        }
    }
}
