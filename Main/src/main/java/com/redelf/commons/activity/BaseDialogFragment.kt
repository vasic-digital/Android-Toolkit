package com.redelf.commons.activity

import androidx.fragment.app.DialogFragment
import com.redelf.commons.extensions.fitInsideSystemBoundaries

abstract class BaseDialogFragment : DialogFragment() {

    override fun onResume() {
        super.onResume()

        fitInsideSystemBoundaries()
    }
}