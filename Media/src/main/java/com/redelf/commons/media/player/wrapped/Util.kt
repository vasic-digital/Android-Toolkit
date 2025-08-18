package com.redelf.commons.media.player.wrapped

import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.executeWithWorkManager
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.sync
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain

object Util {

    @JvmStatic
    fun onWorker(runnable: Runnable) {

        val ctx = BaseApplication.takeContext()

        ctx.executeWithWorkManager {

            runnable.run()
        }
    }

    @JvmStatic
    fun <T> syncOnWorker(obtainable: Obtain<T?>): T? {

        return sync("") { callback ->

            onWorkerAsync(obtainable, callback)
        }
    }

    @JvmStatic
    fun <T> onWorkerAsync(obtainable: Obtain<T?>, callback: OnObtain<T?>) {

        val ctx = BaseApplication.takeContext()

        ctx.executeWithWorkManager {

            try {

                val result = obtainable.obtain()

                callback.onCompleted(result)

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }
}