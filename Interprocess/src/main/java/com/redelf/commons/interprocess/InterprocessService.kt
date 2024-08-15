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

        private const val tag = "Interprocess service ::"

        fun send(function: String, content: String? = null) {

            val intent = Intent(InterprocessReceiver.ACTION)
            val data = InterprocessData(function, content)
            val json = Gson().toJson(data)

            intent.putExtra(InterprocessProcessor.EXTRA_DATA, json)

            BaseApplication.takeContext().sendBroadcast(intent)
        }
    }

    private val binder = InterprocessBinder()

    private val processors = mutableSetOf<InterprocessProcessor>()

    override fun onBind(intent: Intent?): IBinder {

        Console.log("$tag Bound to client")

        return binder
    }

    inner class InterprocessBinder : Binder() {

        fun getService(): InterprocessService = this@InterprocessService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Console.log("$tag Service started")

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