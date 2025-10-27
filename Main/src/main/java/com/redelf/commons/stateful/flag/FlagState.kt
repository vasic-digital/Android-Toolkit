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


package com.redelf.commons.stateful.flag

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.registration.Registration
import com.redelf.commons.value.Get
import com.redelf.commons.value.Set
import java.util.concurrent.atomic.AtomicBoolean

class FlagState(context: String) :

    Get<Boolean>,
    Set<Boolean>,
    Registration<OnObtain<Boolean>>

{

    private val value = AtomicBoolean()
    private val callbacks = Callbacks<OnObtain<Boolean>>(context)

    override fun isRegistered(subscriber: OnObtain<Boolean>): Boolean {

        return callbacks.isRegistered(subscriber)
    }

    override fun register(subscriber: OnObtain<Boolean>) {

        if (callbacks.isRegistered(subscriber)) {

            return
        }

        callbacks.register(subscriber)

        subscriber.onCompleted(value.get())
    }

    override fun unregister(subscriber: OnObtain<Boolean>) {

        if (!callbacks.isRegistered(subscriber)) {

            return
        }

        callbacks.unregister(subscriber)
    }

    override fun get(): Boolean {

        return value.get()
    }

    override fun set(value: Boolean) {

        val oldValue = this.value.getAndSet(value)

        if (oldValue != value) {

            callbacks.doOnAll(

                object : CallbackOperation<OnObtain<Boolean>> {

                    override fun perform(callback: OnObtain<Boolean>) {

                        callback.onCompleted(value)
                    }
                }, "set"
            )
        }
    }
}