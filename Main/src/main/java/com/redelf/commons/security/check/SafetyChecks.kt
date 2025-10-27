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


package com.redelf.commons.security.check

import com.google.firebase.crashlytics.internal.common.CommonUtils
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.execution.Executor
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

class SafetyChecks {

    private val checking = AtomicBoolean()
    private val callbacks = Callbacks<SafetyCheckCallback>(identifier = "Safety check")

    private val check = Runnable {

        val isRooted = CommonUtils.isRooted()
        callbacks.doOnAll(

            object : CallbackOperation<SafetyCheckCallback> {
                override fun perform(callback: SafetyCheckCallback) {

                    callback.onRootingCheck(isRooted)
                    callbacks.unregister(callback)
                }
            },
            "Safety check"
        )
        checking.set(false)
    }

    fun checkRooted(callback: SafetyCheckCallback) {

        callbacks.register(callback)
        if (checking.get()) {

            Console.warning("Root check is already in prgress")
            return
        }
        checking.set(true)
        Executor.MAIN.execute(check)
    }
}