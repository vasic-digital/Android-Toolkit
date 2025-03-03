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