package com.redelf.commons.interprocess

import android.content.Intent
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.registration.Registration
import java.util.concurrent.ConcurrentHashMap

object Interprocessor : Interprocessing, Registration<InterprocessProcessor> {

    private val processors = ConcurrentHashMap<Int, InterprocessProcessor>()

    fun send(

        receiver: String,
        function: String,
        content: String? = null

    ): Boolean {

        return BaseApplication.takeContext().sendBroadcastIPC(

            content = content,
            function = function,
            receiver = receiver,
            action = InterprocessReceiver.ACTION,
            tag = "IPC :: Interprocessor :: Send ::",
            receiverClass = InterprocessReceiver::class
        )
    }

    override fun register(subscriber: InterprocessProcessor) {

        if (processors.contains(subscriber)) {

            return
        }

        processors[subscriber.hashCode()] = subscriber
    }

    override fun unregister(subscriber: InterprocessProcessor) {

        if (processors.contains(subscriber)) {

            processors.values.remove(subscriber)
        }
    }

    override fun isRegistered(subscriber: InterprocessProcessor): Boolean {

        return processors.contains(subscriber)
    }

    override fun onIntent(intent: Intent) {

        exec(

            onRejected = { err -> recordException(err) }

        ) {

            processors.values.forEach { processor ->

                processor.process(intent)
            }
        }
    }
}