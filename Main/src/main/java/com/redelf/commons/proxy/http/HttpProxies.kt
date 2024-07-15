package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.proxy.Proxies
import com.redelf.commons.proxy.Proxy
import java.util.concurrent.PriorityBlockingQueue

class HttpProxies(private val ctx: Context) : Proxies {

    override fun obtain(): PriorityBlockingQueue<Proxy> {

        TODO("Not yet implemented")
    }
}