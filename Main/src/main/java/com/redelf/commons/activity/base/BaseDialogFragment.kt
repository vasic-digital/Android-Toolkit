package com.redelf.commons.activity.base

import androidx.fragment.app.DialogFragment
import com.redelf.commons.activity.fragment.ActivityPresentable
import com.redelf.commons.activity.fragment.FragmentWrapperActivity
import com.redelf.commons.activity.stateful.StatefulActivity
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console

abstract class BaseDialogFragment : DialogFragment(), ActivityPresentable {

    override fun onResume() {
        super.onResume()

        fitInsideSystemBoundaries()
    }

    override fun showInActivity(): Boolean {

        activity?.let {

            if (it is StatefulActivity) {

                val intent = FragmentWrapperActivity.createIntent(it, this)
                it.startActivity(intent)

                return true

            } else {

                val msg = "Activity must be a ${StatefulActivity::class.simpleName} " +
                        "to present in ${FragmentWrapperActivity::class.simpleName}"

                val e = IllegalArgumentException(msg)
                recordException(e)
            }
        }

        val msg = "Activity must not be null and be a ${StatefulActivity::class.simpleName} " +
                "to present in ${FragmentWrapperActivity::class.simpleName}"

        val e = IllegalArgumentException(msg)
        recordException(e)

        return false
    }
}