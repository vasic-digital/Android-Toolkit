package com.redelf.commons.activity.popup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.redelf.commons.activity.transition.TransitionEffectsActivity
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

    override fun onBack() {

        closePopup("onBack")
    }

    override fun dismiss() {

        onDismissed?.let {

            Console.log("$tag Triggering the callback")

            it.onCompleted(getDismissResult())
        }

        if (readyToDismiss()) {

            super.dismiss()
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

    protected open fun closePopup(from: String) {

        dismiss()

        activity?.let {

            try {

                if (it is TransitionEffectsActivity) {

                    it.finishFrom("Popup.Close(from='$from')")

                } else {

                    it.finish()
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }
}