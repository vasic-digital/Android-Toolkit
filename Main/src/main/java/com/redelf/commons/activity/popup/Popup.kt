package com.redelf.commons.activity.popup

import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.extensions.recordException

open class Popup : PopupFragment() {

    protected open fun closePopup() {

        dismiss()

        activity?.let {

            try {

                if (it is TransitionEffectsActivity) {

                    it.finishFrom("Popup.Close")

                } else {

                    it.finish()
                }

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }
}