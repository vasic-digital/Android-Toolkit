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

            Console.log("$tag Triggering the callback")

            it.onCompleted(getDismissResult())
        }

        if (readyToDismiss()) {

            getPopupActivity()?.onBack()
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