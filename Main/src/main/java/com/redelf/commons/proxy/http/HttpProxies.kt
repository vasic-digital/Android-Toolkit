package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.readRawTextFile
import com.redelf.commons.logging.Console
import com.redelf.commons.proxy.Proxies
import java.util.concurrent.PriorityBlockingQueue

class HttpProxies(private val ctx: Context) : Proxies<HttpProxy> {

    private val proxies = PriorityBlockingQueue<HttpProxy>()

    override fun obtain(): PriorityBlockingQueue<HttpProxy> {

        if (proxies.isEmpty()) {

            val raw = ctx.readRawTextFile(R.raw.proxies)

            if (isNotEmpty(raw)) {

                val lines = raw.split("\n")

                lines.forEach { line ->

                    try {

                        val proxy = HttpProxy(ctx, line.trim())

                        proxies.add(proxy)

                    } catch (e: IllegalArgumentException) {

                        Console.error(e)
                    }
                }
            }
        }

        return proxies
    }

    override fun clear() {

        proxies.clear()
    }
}