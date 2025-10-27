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


@file:Suppress("DEPRECATION")

package com.redelf.access.implementation.pin

import android.content.Intent
import com.redelf.access.implementation.AccessActivity
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration

abstract class PinAccessActivity : AccessActivity(), Registration<CommonExecutionCallback> {

    var activityRequestCode = 0
    private var pinAuthenticated = false

    private val executionCallbacks =
        Callbacks<CommonExecutionCallback>(identifier = "Common execution")

    private val executionCallback = object : CommonExecutionCallback {

        override fun onExecution(success: Boolean, calledFrom: String) {

            executionCallbacks.doOnAll(object : CallbackOperation<CommonExecutionCallback> {

                override fun perform(callback: CommonExecutionCallback) {

                    callback.onExecution(success, "executionCallback :: $calledFrom")
                    executionCallbacks.unregister(callback)
                }
            }, operationName = "Execution operation")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Console.log("onActivityResult(): $requestCode, ${resultCode == RESULT_OK}")

        if (requestCode == activityRequestCode) {

            pinAuthenticated = resultCode == RESULT_OK

            if (pinAuthenticated) {

                Console.log("PIN access success")

            } else {

                Console.error("PIN access failed")
            }
            activityRequestCode = 0
            executionCallback.onExecution(pinAuthenticated, "onActivityResult")

        } else {

            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun register(subscriber: CommonExecutionCallback) {

        executionCallbacks.register(subscriber)
    }

    override fun unregister(subscriber: CommonExecutionCallback) {

        executionCallbacks.unregister(subscriber)
    }

    override fun isRegistered(subscriber: CommonExecutionCallback): Boolean {

        return executionCallbacks.isRegistered(subscriber)
    }

    override fun isAuthenticated() = super.isAuthenticated() || pinAuthenticated
}