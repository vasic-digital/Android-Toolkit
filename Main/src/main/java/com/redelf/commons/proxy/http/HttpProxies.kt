package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.proxy.Proxies
import java.util.concurrent.PriorityBlockingQueue

class HttpProxies(private val ctx: Context) : Proxies<HttpProxy> {

    override fun obtain(): PriorityBlockingQueue<HttpProxy> {

        TODO("Not yet implemented")
    }
}