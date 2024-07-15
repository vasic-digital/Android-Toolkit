package com.redelf.commons.proxy

import android.content.Context
import com.redelf.commons.R
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL

class HttpProxy(address: String, port: Int) : Proxy(address, port) {

    fun get(): java.net.Proxy {

        return java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(address, port))
    }

    override fun isAlive(ctx: Context): Boolean {

        return try {

            val proxy = get()
            val url = URL(ctx.getString(R.string.proxy_alive_check_url))

            val connection = url.openConnection(proxy) as HttpURLConnection

            connection.requestMethod = "GET"
            connection.readTimeout = 5000 // 5 seconds timeout
            connection.connectTimeout = 5000 // 5 seconds timeout

            connection.connect()

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == 200

        } catch (e: Exception) {

            false
        }
    }
}