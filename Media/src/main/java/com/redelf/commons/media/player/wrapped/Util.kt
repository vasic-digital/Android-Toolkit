package com.redelf.commons.media.player.wrapped

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.executeWithWorkManager

object Util {

    @JvmStatic
    fun onWorker(runnable: Runnable) {

        val ctx = BaseApplication.takeContext()

        ctx.executeWithWorkManager {

            runnable.run()
        }
    }
}