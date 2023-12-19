package com.redelf.commons.lifecycle.exception

import android.text.TextUtils

class NotInitializedException(

    private val who: String? = null,
    whoMsgPart: String = if (TextUtils.isEmpty(who)) {

        ""

    } else {

        "$who :: "
    }

) : IllegalStateException("${whoMsgPart}Not initialized")