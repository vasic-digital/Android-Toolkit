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


package com.redelf.commons.activity.popup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.redelf.commons.activity.fragment.FragmentWrapperActivity
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain
import java.util.concurrent.atomic.AtomicBoolean


abstract class Popup : PopupFragment() {

    override var logTag = "Popup :: ${this::class.simpleName} ::"

    var onDismissed: OnObtain<Boolean>? = null

    private val instanceStateSaved = AtomicBoolean()

    fun readyToDismiss() = !isDetached &&
            activity?.supportFragmentManager?.isDestroyed == false &&
            isVisible &&
            !instanceStateSaved.get()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        Console.log(

            "Popup :: ${this::class.simpleName} :: " +
                    "${hashCode()} :: STATE=Attached"
        )
    }

    /*
        Back action is handled by the parent activity
    */
    override fun onBack() {

        getPopupActivity()?.onBack()
    }

    override fun dismiss() {

        onDismissed?.let {

            Console.log("$logTag Triggering the callback")

            it.onCompleted(getDismissResult())
        }

        val readyTo = readyToDismiss()

        if (readyTo) {

            val pActivity = getPopupActivity()

            pActivity?.onBack()

        } else {

            Console.warning("$logTag Dismiss skipped")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        instanceStateSaved.set(true)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        instanceStateSaved.set(false)
    }

    protected open fun getDismissResult() = true

    protected open fun closePopup(from: String): Boolean {

        onDismissed?.let {

            Console.log("$logTag Triggering the callback")

            it.onCompleted(getDismissResult())
        }

        val ctx = getPopupActivity()

        ctx?.let {

            it.finishFrom("Popup.Close(from='$from')")

            return true
        }

        return false
    }

    override fun startActivity(intent: Intent) {

        getPopupActivity()?.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {

        getPopupActivity()?.startActivity(intent, options)
    }

    fun getPopupActivity(): FragmentWrapperActivity? {

        activity?.let {

            if (it !is FragmentWrapperActivity) {

                val msg = "Popup must be used with " +
                        "${FragmentWrapperActivity::class.simpleName}, " +
                        "current is '${it::class.simpleName}'"

                val e = IllegalArgumentException(msg)
                recordException(e)

                return null
            }

            return it
        }

        return null
    }
}