package com.redelf.commons.net.proxy.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.data.list.ListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.net.proxy.Proxies
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class HttpProxies(

    private val ctx: Context,
    private val alive: Boolean = true,

    private val sources: List<ListDataSource<String>> =
        listOf(RawStringsListDataSource(ctx, R.raw.proxies)),

    private val combineSources: Boolean = true,

) : Proxies<HttpProxy> {

    private val proxies = PriorityQueue(HttpProxy.QUALITY_COMPARATOR)

    override fun obtain(): PriorityQueue<HttpProxy> {

        if (proxies.isEmpty()) {

            val completed = AtomicInteger()
            val waitingFor = AtomicInteger()
            val next = AtomicBoolean(true)
            val sourcesIterator = sources.iterator()

            while (sourcesIterator.hasNext() && next.get()) {

                waitingFor.incrementAndGet()

                val source = sourcesIterator.next()

                exec(

                    onRejected = { err ->

                        Console.error(err)

                        completed.incrementAndGet()
                    }

                ) {

                    val obtained = source.getList()

                    if (obtained.isNotEmpty()) {

                        fun addProxy(proxy: HttpProxy) {

                            if (!proxies.contains(proxy)) {

                                proxies.add(proxy)
                            }

                            if (!combineSources) {

                                next.set(proxies.isEmpty())
                            }
                        }

                        obtained.forEach { line ->

                            waitingFor.incrementAndGet()

                            exec(

                                onRejected = { err ->

                                    Console.error(err)

                                    completed.incrementAndGet()
                                }

                            ) {

                                if (next.get()) {

                                    try {

                                        val proxy = HttpProxy(ctx, line.trim())

                                        if (alive) {

                                            if (proxy.isAlive(ctx)) {

                                                addProxy(proxy)
                                            }

                                        } else {

                                            addProxy(proxy)
                                        }

                                    } catch (e: IllegalArgumentException) {

                                        Console.error(e)
                                    }
                                }

                                completed.incrementAndGet()
                            }
                        }
                    }

                    completed.incrementAndGet()
                }

                yieldWhile {

                    waitingFor.get() != completed.get()
                }
            }
        }

        return proxies
    }

    override fun clear() {

        proxies.clear()
    }
}