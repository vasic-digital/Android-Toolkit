package com.redelf.commons.intention

import android.content.Intent

interface Intentional {

    fun takeIntent(): Intent?
}