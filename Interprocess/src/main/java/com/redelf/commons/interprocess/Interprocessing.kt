package com.redelf.commons.interprocess

import android.content.Intent

fun interface Interprocessing {

    fun onIntent(intent: Intent)
}