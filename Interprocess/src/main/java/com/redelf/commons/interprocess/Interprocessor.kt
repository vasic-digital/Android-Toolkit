package com.redelf.commons.interprocess

import android.content.Intent
import com.google.gson.Gson
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Interprocessor : Interprocessing, Registration<InterprocessProcessor> {

    private const val TAG = "IPC :: Interprocessor ::"
    private val processors = ConcurrentHashMap<Int, InterprocessProcessor>()

    fun send(

        receiver: String,
        function: String,
        content: String? = null

    ): Boolean {

        Console.log("$TAG Sending intent :: START")

        val intent = Intent()
        val data = InterprocessData(function, content)
        val json = Gson().toJson(data)

        intent.setAction(InterprocessReceiver.ACTION)

        Console.log("$TAG Sending intent :: Action = ${intent.action}")
        Console.log("$TAG Sending intent :: Data = $data")
        Console.log("$TAG Sending intent :: JSON = $json")

        intent.putExtra(InterprocessProcessor.EXTRA_DATA, json)

        val clazz: KClass<InterprocessReceiver> = InterprocessReceiver::class
        val cName = clazz.qualifiedName ?: ""


        if (isEmpty(cName)) {

            Console.error("$TAG Sending intent :: Failed :: Class name is empty")

            return false
        }

        if (isEmpty(receiver)) {

            Console.error("$TAG Sending intent :: Failed :: Package name is empty")

            return false
        }

        Console.log("$TAG Sending intent :: Class = $cName")
        Console.log("$TAG Sending intent :: Target receiver = $receiver")

        intent.setClassName(receiver, cName)

        if (BaseApplication.takeContext().sendBroadcastWithResult(intent, local = false)) {

            Console.log("$TAG Sending intent :: END")

            return true

        } else {

            Console.error("$TAG Sending intent :: Failed")
        }

        return false
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