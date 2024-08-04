package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class NotInitializedException(

    private val who: String? = null,
    errorMessage: String = if (TextUtils.isEmpty(who)) {

        "Not initialized"

    } else {

        "$who is not initialized"
    }

) : IllegalStateException(errorMessage)