package com.redelf.commons.activity.popup

import android.os.Bundle
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

    override fun onBack() {

        closePopup("onBack")
    }
}