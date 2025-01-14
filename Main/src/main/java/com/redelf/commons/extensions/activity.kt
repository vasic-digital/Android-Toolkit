package com.redelf.commons.extensions

import android.app.Activity
import android.os.Build
import android.view.WindowInsets

fun Activity.fitInsideSystemBoundaries() {

    window.decorView.setOnApplyWindowInsetsListener { view, insets ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(0, systemBarsInsets.top, 0, systemBarsInsets.bottom)
        }

        insets
    }
}