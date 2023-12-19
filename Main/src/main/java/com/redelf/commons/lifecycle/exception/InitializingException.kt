package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class InitializingException(

    private val who: String? = null,
    errorMessage: String = if (TextUtils.isEmpty(who)) {

        "Initializing"

    } else {

        "$who is initializing"
    }

) : IllegalStateException(errorMessage)