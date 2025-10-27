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