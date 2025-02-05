package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class ShuttingDownException(

    private val who: String? = null,
    errorMessage: String = if (TextUtils.isEmpty(who)) {

        "Shutting down"

    } else {

        "$who is shutting down"
    }

) : IllegalStateException(errorMessage)