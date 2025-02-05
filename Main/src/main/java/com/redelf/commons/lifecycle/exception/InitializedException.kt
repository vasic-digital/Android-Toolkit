package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class InitializedException(

    private val who: String? = null,
    errorMessage: String = if (TextUtils.isEmpty(who)) {

        "Initialized"

    } else {

        "$who is initialized"
    }

) : IllegalStateException(errorMessage)