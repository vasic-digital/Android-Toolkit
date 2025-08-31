package com.redelf.commons.extensions

import com.redelf.commons.atomic.Countdown
import java.util.concurrent.CountDownLatch

class CountDownLatch(

    count: Int,
    context: String = "",
    timeoutInSeconds: Long = 60,
    latch: CountDownLatch = CountDownLatch(count)

) : Countdown(

    context,
    count,
    timeoutInSeconds,
    latch

) {

    companion object {

        @JvmStatic
        fun instantiate(count: Int): com.redelf.commons.extensions.CountDownLatch {

            return com.redelf.commons.extensions.CountDownLatch(count)
        }
    }
}