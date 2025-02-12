package com.redelf.commons.extensions

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment

fun Activity.fitInsideSystemBoundaries() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        window.decorView.setOnApplyWindowInsetsListener { view, insets ->

            val systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(0, systemBarsInsets.top, 0, systemBarsInsets.bottom)
            insets
        }
    }
}

fun Activity.getSystemBarsInsets(onInsetsChanged: (top: Int, bottom: Int) -> Unit) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        val rootView: View = window.decorView.findViewById(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->

            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            onInsetsChanged(systemBarsInsets.top, systemBarsInsets.bottom)
            insets
        }

        rootView.requestApplyInsets()
    }
}

fun DialogFragment.fitInsideSystemBoundaries() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        activity?.getSystemBarsInsets { top, bottom ->

            view?.setPadding(0, top, 0, bottom)
        }
    }
}
