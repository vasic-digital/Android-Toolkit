package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class TerminatedException(

    private val who: String? = null,
    errorMessage: String = if (TextUtils.isEmpty(who)) {

        "Terminated"

    } else {

        "$who is terminated"
    }

) : IllegalStateException(errorMessage)