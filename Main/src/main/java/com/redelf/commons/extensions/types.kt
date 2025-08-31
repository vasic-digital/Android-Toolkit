package com.redelf.commons.extensions

import com.redelf.commons.atomic.CountDown
import java.util.concurrent.CountDownLatch

class CountDownLatch(

    count: Int,
    context: String = "",
    timeoutInSeconds: Long = 60,
    latch: CountDownLatch = CountDownLatch(count)

) : CountDown(

    context,
    count,
    timeoutInSeconds,
    latch

) {

    companion object {

        @JvmStatic
        fun instantiate(count: Int, context: String): com.redelf.commons.extensions.CountDownLatch {

            return CountDownLatch(count, context)
        }
    }
}