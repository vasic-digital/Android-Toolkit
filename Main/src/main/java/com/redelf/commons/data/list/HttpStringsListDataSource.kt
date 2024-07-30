package com.redelf.commons.data.list

import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.logging.Console
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HttpStringsListDataSource(

    private val url: String,
    private val throwOnError: Boolean = false

) : ListDataSource<String> {

    @Throws(IOException::class, IllegalStateException::class)
    override fun getList(): List<String> {

        /*
        * TODO: Use Retrofit
        */
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        try {

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {

                val raw = response.body?.string()

                if (isNotEmpty(raw)) {

                    return raw?.split("\n") ?: emptyList()
                }
            }

        } catch (e: IOException) {

            if (throwOnError) {

                throw e
            }

            Console.error(e)
        }

        return emptyList()
    }
}