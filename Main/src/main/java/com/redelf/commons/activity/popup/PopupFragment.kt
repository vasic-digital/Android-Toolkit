package com.redelf.commons.activity.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.activity.fragment.ActivityPresentable
import com.redelf.commons.activity.fragment.FragmentWrapperActivity
import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.registration.Registration

abstract class PopupFragment :

    DialogFragment(),
    ActivityPresentable,
    Registration<DialogInterface.OnDismissListener>

{

    private val callbacks = Callbacks<DialogInterface.OnDismissListener>("onDismiss")

    private val onDismiss = DialogInterface.OnDismissListener { dialog ->

        callbacks.doOnAll(object : CallbackOperation<DialogInterface.OnDismissListener> {

            override fun perform(callback: DialogInterface.OnDismissListener) {

                callback.onDismiss(dialog)
                callbacks.unregister(callback)
            }

        }, operationName = "Dialog dismiss")
    }

    override fun register(subscriber: DialogInterface.OnDismissListener) {

        if (isRegistered(subscriber)) {

            return
        }

        callbacks.register(subscriber)
    }

    override fun unregister(subscriber: DialogInterface.OnDismissListener) {

        if (isRegistered(subscriber)) {

            callbacks.unregister(subscriber)
        }
    }

    override fun isRegistered(subscriber: DialogInterface.OnDismissListener): Boolean {

        return callbacks.isRegistered(subscriber)
    }

    override fun onResume() {
        super.onResume()

        fitInsideSystemBoundaries()
    }

    override fun showInActivity(

        activity: Class<*>,
        context: TransitionEffectsActivity,

    ): Boolean {

        try {

            val intent = FragmentWrapperActivity.createIntent(context, this, activity)
            context.startActivity(intent)

        } catch (e: Throwable) {

            recordException(e)
        }

        return true
    }

    @SuppressLint("DialogFragmentCallbacksDetector")
    @Throws(IllegalArgumentException::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        activity?.let { a ->

            if (a !is BaseActivity) {

                throw IllegalArgumentException(

                    "Activity must be a ${BaseActivity::class.simpleName}"
                )
            }

            val dialog = object : Dialog(a, theme) {}

            dialog.setOnShowListener {

                val callback = object : OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {

                        onBack()
                    }
                }

                a.onBackPressedDispatcher.addCallback(a, callback)
            }

            dialog.setOnDismissListener(onDismiss)

            return dialog
        }

        throw IllegalArgumentException("Activity must not be null")
    }

    protected open fun onBack() {

        dismiss()

        Console.log("On back :: In='${this::class.simpleName}'")
    }
}