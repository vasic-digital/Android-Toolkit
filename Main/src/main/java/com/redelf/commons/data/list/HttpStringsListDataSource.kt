package com.redelf.commons.data.list

import android.content.Context
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.readRawTextFile
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HttpStringsListDataSource(

    private val ctx: Context,
    private val url: String

) : ListDataSource<String> {

    override fun getList(): List<String> {

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

            Console.error(e)
        }

        return emptyList()
    }
}