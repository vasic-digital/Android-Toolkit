package com.redelf.commons.net.api

import com.redelf.commons.authentification.exception.CredentialsInvalidException
import com.redelf.commons.obtain.OnObtain
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class DefaultApiServiceDefaultResponseHandler<T> : ApiServiceResponseHandler<T>() {

    companion object {

        val DEBUG = AtomicBoolean()
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

            if (additionalExpectedCodes.contains(code)) {

                callback.onCompleted(null)

            } else {

                val e = if (DEBUG.get()) {

                    val loc = response?.raw()?.request?.url ?: ""
                    val codeStr = code.toString()

                    IOException("Response is not successful $codeStr $loc".trim())

                } else {

                    IOException("Response is not successful")
                }

                callback.onFailure(e)
            }
        }
    }
}