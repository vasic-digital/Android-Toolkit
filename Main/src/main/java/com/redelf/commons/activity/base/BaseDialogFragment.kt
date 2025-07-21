package com.redelf.commons.activity.base

import android.app.Dialog
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import com.google.android.play.integrity.internal.ac
import com.redelf.commons.activity.fragment.ActivityPresentable
import com.redelf.commons.activity.fragment.FragmentWrapperActivity
import com.redelf.commons.activity.stateful.StatefulActivity
import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console

abstract class BaseDialogFragment : DialogFragment(), ActivityPresentable {

    override fun onResume() {
        super.onResume()

        fitInsideSystemBoundaries()
    }

    override fun showInActivity(context: TransitionEffectsActivity): Boolean {

        try {

            val intent = FragmentWrapperActivity.createIntent(context, this)
            context.startActivity(intent)

        } catch (e: Throwable) {

            recordException(e)
        }

        return true
    }

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

            return dialog
        }

        throw IllegalArgumentException("Activity must not be null")
    }

    protected open fun onBack() {

        Console.log("On back :: In='${this::class.simpleName}'")
    }
}