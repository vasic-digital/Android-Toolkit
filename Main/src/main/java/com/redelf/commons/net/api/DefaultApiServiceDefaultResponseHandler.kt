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

        if (response == null) {

            callback.onFailure(IOException("Null response received"))
            return
        }

        val body = response.body()
        val code = response.code()
        val combinedExpectedCodes = expectedCodes + additionalExpectedCodes

        when {

            code == 401 -> {
                callback.onFailure(CredentialsInvalidException())
            }

            code in 500..599 -> {

                callback.onFailure(IOException("Internal Server Error with code $code"))
            }

            response.isSuccessful && body != null -> {

                callback.onCompleted(body)
            }

            useExpectedCodes && combinedExpectedCodes.contains(code) -> {

                callback.onCompleted(null)
            }

            additionalExpectedCodes.contains(code) -> {

                callback.onCompleted(null)
            }

            else -> {

                val error = if (DEBUG.get()) {

                    val url = response.raw().request.url

                    val errorBody = try {

                        response.errorBody()?.string()

                    } catch (e: IOException) {

                        "Unable to read error body: ${e.message}"
                    }

                    IOException("Request failed with code $code\nURL: $url\nError: $errorBody")

                } else {

                    IOException("Request failed with code $code")
                }

                callback.onFailure(error)
            }
        }
    }
}