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


package com.redelf.commons.lifecycle.initialization

import com.redelf.commons.extensions.exec
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.lifecycle.exception.NotInitializedException
import com.redelf.commons.logging.Console
import java.util.concurrent.Callable

interface InitializationAsync<T> : InitializationCondition {

    companion object {

        @Throws(NotInitializedException::class)
        fun waitForInitialization(

            who: InitializationAsync<*>,
            timeoutInSeconds: Long = 60L,
            initLogTag: String = "${who::class.simpleName} initialization ::"

        ) {

            val callable = object : Callable<Boolean> {

                override fun call(): Boolean {

                    if (who.isNotInitialized()) {

                        Console.log("$initLogTag not initialized yet")
                    }

                    if (who.isInitializing()) {

                        Console.log("$initLogTag still initializing")
                    }

                    while (who.isInitializing()) {

                        Thread.yield()
                    }

                    if (who.isNotInitialized()) {

                        Thread.yield()
                    }

                    if (who.isInitialized()) {

                        Console.log("$initLogTag initialized")

                        return true
                    }

                    return false
                }
            }

            exec(

                callable = callable,
                logTag = initLogTag
            )

            if (!who.isInitialized()) {

                throw NotInitializedException()
            }
        }
    }

    fun initialize(callback: LifecycleCallback<T>)
}