package com.redelf.commons.net.endpoint.http

import android.content.Context
import com.redelf.commons.R
import com.redelf.commons.data.list.ListDataSource
import com.redelf.commons.data.list.RawStringsListDataSource
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.yieldWhile
import com.redelf.commons.logging.Console
import com.redelf.commons.net.endpoint.Endpoints
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class HttpEndpoints(

    private val ctx: Context,
    private val alive: Boolean = true,

    private val sources: List<ListDataSource<String>> =
        listOf(RawStringsListDataSource(ctx, R.raw.proxy_endpoints)),

    private val combineSources: Boolean = true,

) : Endpoints<HttpEndpoint> {

    private val endpoints = PriorityQueue(HttpEndpoint.QUALITY_COMPARATOR)

    override fun obtain(): PriorityQueue<HttpEndpoint> {

        if (endpoints.isEmpty()) {

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

                        fun addEndpoint(endpoint: HttpEndpoint) {

                            if (!endpoints.contains(endpoint)) {

                                endpoints.add(endpoint)
                            }

                            if (!combineSources) {

                                next.set(endpoints.isEmpty())
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

                                        val httpEndpoint = HttpEndpoint(ctx, line.trim())

                                        if (alive) {

                                            if (httpEndpoint.isAlive(ctx)) {

                                                addEndpoint(httpEndpoint)
                                            }

                                        } else {

                                            addEndpoint(httpEndpoint)
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

        return endpoints
    }

    override fun clear() {

        endpoints.clear()
    }
}