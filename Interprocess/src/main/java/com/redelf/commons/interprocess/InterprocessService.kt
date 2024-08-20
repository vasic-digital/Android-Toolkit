package com.redelf.commons.interprocess

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.gson.Gson
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration

class InterprocessService : Service(), Registration<InterprocessProcessor> {

    companion object {

        private const val TAG = "Interprocess service ::"

        fun send(function: String, content: String? = null) : Boolean {

            Console.log("$TAG Sending intent :: START")

            val intent = Intent(InterprocessReceiver.ACTION)
            val data = InterprocessData(function, content)
            val json = Gson().toJson(data)

            Console.log("$TAG Sending intent :: Action = ${InterprocessReceiver.ACTION}")
            Console.log("$TAG Sending intent :: Data = $data")
            Console.log("$TAG Sending intent :: JSON = $json")

            intent.putExtra(InterprocessProcessor.EXTRA_DATA, json)

            if (BaseApplication.takeContext().sendBroadcastWithResult(intent)) {

                Console.log("$TAG Sending intent :: END")

                return true

            } else {

                Console.error("$TAG Sending intent :: Failed")
            }

            return false
        }
    }

    private val binder = InterprocessBinder()

    private val processors = mutableSetOf<InterprocessProcessor>()

    override fun onBind(intent: Intent?): IBinder {

        Console.log("$TAG Bound to client")

        return binder
    }

    inner class InterprocessBinder : Binder() {

        fun getService(): InterprocessService = this@InterprocessService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Console.log("$TAG Service started")

        return START_STICKY
    }

    override fun register(subscriber: InterprocessProcessor) {

        processors.add(subscriber)
    }

    override fun unregister(subscriber: InterprocessProcessor) {

        processors.remove(subscriber)
    }

    override fun isRegistered(subscriber: InterprocessProcessor): Boolean {

        return processors.contains(subscriber)
    }

    fun onIntent(intent: Intent) {

        exec(

            onRejected = { err -> recordException(err) }

        ) {

            processors.forEach { processor ->

                processor.process(intent)
            }
        }
    }
}