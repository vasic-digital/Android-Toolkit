package com.redelf.commons.api

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.exception.credentials.CredentialsInvalidException
import com.redelf.commons.obtain.OnObtain
import retrofit2.Response
import java.io.IOException

class DefaultApiServiceDefaultResponseHandler<T> : ApiServiceResponseHandler<T>() {

    companion object {

        var DEBUG: Boolean? = null
    }

    override fun onResponse(

        response: Response<T>?,
        callback: OnObtain<T?>,
        useExpectedCodes: Boolean,
        additionalExpectedCodes: List<Int>

    ) {

        val body = response?.body()
        val code = response?.code() ?: 0

        val combinedExpectedCodes = expectedCodes + additionalExpectedCodes

        val ok = (response?.isSuccessful == true && body != null) ||
                (useExpectedCodes && combinedExpectedCodes.contains(code))

        if (code == 401) {

            val e = CredentialsInvalidException()
            callback.onFailure(e)

        } else if (ok) {

            if (useExpectedCodes) {

                callback.onCompleted(null)

            } else {

                body?.let {

                    callback.onCompleted(it)
                }
            }

        } else {

            val e = if (DEBUG ?: BaseApplication.DEBUG.get()) {

                val loc = response?.raw()?.request?.url ?: ""
                val codeStr = response?.code()?.toString() ?: ""
                IOException("Response is not successful $codeStr $loc".trim())

            } else {

                IOException("Response is not successful")
            }

            callback.onFailure(e)
        }
    }
}