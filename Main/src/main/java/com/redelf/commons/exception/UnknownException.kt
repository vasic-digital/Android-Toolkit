package com.redelf.commons.exception

import com.redelf.commons.recordException

class UnknownException private constructor() : IllegalStateException("Something went wrong") {
    companion object {

        fun throwIt() : UnknownException {

            val exception = UnknownException()
            recordException(exception)
            throw exception
        }
    }
}