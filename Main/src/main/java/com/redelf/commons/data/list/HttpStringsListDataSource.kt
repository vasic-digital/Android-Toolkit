package com.redelf.commons.data.list

import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.logging.Console
import com.redelf.commons.net.content.RemoteHttpContentFetcher
import com.redelf.commons.obtain.Obtain
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HttpStringsListDataSource(

    private val url: Obtain<String>,
    private val token: Obtain<String>? = null,
    private val throwOnError: Boolean = false

) : ListDataSource<String> {

    @Throws(IOException::class, IllegalStateException::class)
    override fun getList(): List<String> {

        val fetcher = RemoteHttpContentFetcher(

            endpoint = url.obtain(),
            token = token?.obtain() ?: "",
            throwOnError = throwOnError
        )

        val raw = fetcher.fetch()

        if (isNotEmpty(raw)) {

            return raw.split("\n")
        }

        return emptyList()
    }
}