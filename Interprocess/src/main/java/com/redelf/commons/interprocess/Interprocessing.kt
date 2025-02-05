package com.redelf.commons.interprocess

import android.content.Intent

interface Interprocessing {

    fun onIntent(intent: Intent)
}