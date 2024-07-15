package com.redelf.commons.proxy.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.data.list.ListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.readRawTextFile
import com.redelf.commons.logging.Console
import com.redelf.commons.proxy.Proxies
import java.util.PriorityQueue

class HttpProxies(

    private val ctx: Context,
    private val alive: Boolean = true,
    private val source: ListDataSource<String> = RawStringsListDataSource(ctx, R.raw.proxies)

) : Proxies<HttpProxy> {

    private val proxies = PriorityQueue(HttpProxy.QUALITY_COMPARATOR)

    override fun obtain(): PriorityQueue<HttpProxy> {

        if (proxies.isEmpty()) {

            val lines = source.getList()

            lines.forEach { line ->

                try {

                    val proxy = HttpProxy(ctx, line.trim())

                    if (alive) {

                        if (proxy.isAlive(ctx)) {

                            proxies.add(proxy)
                        }

                    } else {

                        proxies.add(proxy)
                    }

                } catch (e: IllegalArgumentException) {

                    Console.error(e)
                }
            }
        }

        return proxies
    }

    override fun clear() {

        proxies.clear()
    }
}