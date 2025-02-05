package com.redelf.commons.extensions

import android.graphics.Color
import androidx.core.graphics.ColorUtils

fun String.toColor(): Int {

    return Color.parseColor(this)
}

fun String.toOpaqueColor(opacity: Int = 128): Int {

    return toColor().toOpaqueColor(opacity)
}

fun Int.toOpaqueColor(opacity: Int = 128): Int {

    return ColorUtils.setAlphaComponent(this, 128)
}