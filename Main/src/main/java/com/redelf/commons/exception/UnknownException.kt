package com.redelf.commons.exception

import android.text.TextUtils
import com.redelf.commons.recordException

class UnknownException
    private constructor(reason: String = "") : IllegalStateException(getMessage(reason)) {
    companion object {

        private fun getMessage(reason: String = "") : String {

            val msg = "Something went wrong"

            if (!TextUtils.isEmpty(reason)) {

                return "$msg, reason: $reason"
            }
            return msg
        }

        fun throwIt(reason: String = "") : UnknownException {

            val exception = UnknownException(reason)
            recordException(exception)
            throw exception
        }
    }
}