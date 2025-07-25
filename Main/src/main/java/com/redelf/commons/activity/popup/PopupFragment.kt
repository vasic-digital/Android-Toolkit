package com.redelf.commons.activity.popup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import com.redelf.commons.R
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.logging.Console

abstract class PopupFragment : Fragment() {

    protected open val fragmentTheme = R.style.FragmentWrapper
    protected open var logTag = "Popup Fragment :: ${this::class.simpleName} ::"

    fun goBack() = onBack()

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

    protected open fun onBack() {

        Console.warning("On back skipped in ${this::class.simpleName}")
    }

    protected fun inflateView(

        inflater: LayoutInflater,
        container: ViewGroup?, layout: Int

    ) : View {

        val themedContext = ContextThemeWrapper(requireContext(), fragmentTheme)
        val themedInflater = inflater.cloneInContext(themedContext)

        return themedInflater.inflate(layout, container, false)
    }
}