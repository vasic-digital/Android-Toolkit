package com.redelf.commons.exception

import android.text.TextUtils
import com.redelf.commons.extensions.recordException

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

        @Throws(UnknownException::class)
        fun throwIt(reason: String = "") : UnknownException {

            val exception = UnknownException(reason)
            recordException(exception)
            throw exception
        }
    }
}