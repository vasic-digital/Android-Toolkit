package com.redelf.commons.net.api

import com.redelf.commons.obtain.OnObtain
import retrofit2.Response

abstract class ApiServiceResponseHandler<T> {

    protected open val expectedCodes = listOf(200, 201, 202, 203, 204, 205, 206, 207, 208, 226)

    abstract fun onResponse(

        response: Response<T>?,
        callback: OnObtain<T?>,
        useExpectedCodes: Boolean = false,
        additionalExpectedCodes: List<Int> = emptyList()
    )
}