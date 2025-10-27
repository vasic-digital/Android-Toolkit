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


package com.redelf.commons.net.cronet

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.initialization.InitializationParametrizedSync
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import org.chromium.net.CronetEngine
import java.util.concurrent.atomic.AtomicBoolean

object Cronet : InitializationParametrizedSync<Boolean, Context>, Obtain<CronetEngine?> {

    private val tag = "Cronet ::"
    private val ready = AtomicBoolean()
    private var engine: CronetEngine? = null

    override fun initialize(param: Context) : Boolean {

        val tag = "$tag INIT ::"
        val start = System.currentTimeMillis()

        Console.log("$tag START")

        try {

            CronetProviderInstaller.installProvider(param).addOnCompleteListener { task ->

                Console.log(

                    "$tag Provider installation task completed after" +
                            " ${System.currentTimeMillis() - start} ms"
                )

                if (task.isSuccessful) {

                    Console.log("$tag Provider has been installed")

                    engine = CronetEngine.Builder(param).build()

                } else {

                    Console.error("$tag Provider was not installed")
                }

                ready.set(true)
            }

            ready.set(true)

        } catch (e: Throwable) {

            Console.error("$tag ERROR: ${e.message}")

            recordException(e)

            ready.set(false)
        }

        return ready.get()
    }

    override fun obtain() = engine

    override fun isInitialized() = ready.get()

    override fun isInitializing() = !isInitialized()

    override fun initializationCompleted(e: Exception?) {

        recordException(Exception(e))
    }
}