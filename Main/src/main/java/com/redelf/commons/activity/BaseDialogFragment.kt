package com.redelf.commons.activity

import androidx.fragment.app.DialogFragment
import com.redelf.commons.extensions.fitInsideSystemBoundaries

abstract class BaseDialogFragment : DialogFragment() {

    protected open val fitInsideSystemBoundaries: Boolean = true

    override fun onResume() {
        super.onResume()

        if (fitInsideSystemBoundaries) {

            fitInsideSystemBoundaries()
        }
    }
}