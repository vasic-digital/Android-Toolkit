package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.proxy.Proxies
import java.util.concurrent.PriorityBlockingQueue

class HttpProxies(private val ctx: Context) : Proxies<HttpProxy> {

    private val proxies = PriorityBlockingQueue<HttpProxy>()

    override fun obtain(): PriorityBlockingQueue<HttpProxy> {

        if (proxies.isEmpty()) {

            // Add proxies here
        }

        return proxies
    }

    override fun clear() {

        proxies.clear()
    }
}