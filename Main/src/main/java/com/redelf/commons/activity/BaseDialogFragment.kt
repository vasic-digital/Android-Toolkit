package com.redelf.commons.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.redelf.commons.extensions.fitInsideSystemBoundaries

abstract class BaseDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fitInsideSystemBoundaries()
    }

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {

        fitInsideSystemBoundaries()

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}