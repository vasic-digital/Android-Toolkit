package com.redelf.commons.execution

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TaskExecutor private constructor(

    corePoolSize: Int,
    maximumPoolSize: Int,
    workQueue: BlockingQueue<Runnable>?

) : ThreadPoolExecutor(

    corePoolSize,
    maximumPoolSize,
    0L,
    TimeUnit.MILLISECONDS,
    workQueue

) {

    companion object {

        fun instantiate(capacity: Int): ThreadPoolExecutor {

            return TaskExecutor(

                capacity,
                capacity * 100,
                LinkedBlockingQueue(capacity * 1000)
            )
        }

        fun instantiateSingle(queueCapacity: Int = 10 * 1000): ThreadPoolExecutor {

            return ThreadPoolExecutor(

                1, // corePoolSize
                1, // maximumPoolSize
                0L, // keepAliveTime
                TimeUnit.MILLISECONDS, // unit for keepAliveTime
                LinkedBlockingQueue(queueCapacity)
            )
        }
    }
}