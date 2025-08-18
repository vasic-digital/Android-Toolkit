package com.redelf.commons.media.player.wrapped

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.executeWithWorkManager
import com.redelf.commons.extensions.recordException
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.OnObtain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


object Util {

    @JvmStatic
    fun onWorker(runnable: Runnable) {

        val ctx = BaseApplication.takeContext()

        ctx.executeWithWorkManager {

            runnable.run()
        }
    }

    @JvmStatic
    fun <T> syncOnWorkerJava(obtainable: Obtain<T?>): T? {

        return runBlocking {

            syncOnWorker(obtainable)
        }
    }

    @JvmStatic
    suspend fun <T> syncOnWorker(

        obtainable: Obtain<T?>,
        context: CoroutineContext = Dispatchers.IO

    ): T? = withContext(context) {

        val ctx = BaseApplication.takeContext()

        suspendCoroutine { continuation ->

            ctx.executeWithWorkManager {

                try {

                    val result = obtainable.obtain()

                    continuation.resume(result)

                } catch (e: Throwable) {

                    continuation.resumeWithException(e)
                }
            }
        }
    }
}