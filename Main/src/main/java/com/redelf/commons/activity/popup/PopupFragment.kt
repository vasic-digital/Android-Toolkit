package com.redelf.commons.activity.popup

import androidx.fragment.app.Fragment
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.logging.Console

abstract class PopupFragment : Fragment() {

    fun goBack() = onBack()

    protected open fun onBack() {

        Console.warning("On back skipped in ${this::class.simpleName}")
    }

    override fun onResume() {
        super.onResume()

        activity?.fitInsideSystemBoundaries()
    }

    open fun dismiss() {

        if (activity?.isFinishing == true) {

            return
        }

        activity?.finish()
    }
}