package com.redelf.commons.net.cronet

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationParametrized
import com.redelf.commons.lifecycle.InitializationParametrizedSync
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import org.chromium.net.CronetEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object Cronet : InitializationParametrizedSync<Boolean, Context>, Obtain<CronetEngine?> {

    private val ready = AtomicBoolean()
    private var engine: CronetEngine? = null
    private val tag = "Cronet ::"

    override fun initialize(param: Context) : Boolean {

        val tag = "$tag INIT ::"
        val start = System.currentTimeMillis()

        Console.log("$tag START")

        try {

            CronetProviderInstaller.installProvider(param).addOnCompleteListener { task ->

                Console.log(

                    "$tag Provider installation task completed after" +
                            " ${System.currentTimeMillis() - start} ms"
                )

                if (task.isSuccessful) {

                    Console.log("$tag Provider has been installed")

                    engine = CronetEngine.Builder(param).build()

                } else {

                    Console.error("$tag Provider was not installed")
                }

                ready.set(true)
            }

            ready.set(true)

        } catch (e: Exception) {

            Console.error("$tag ERROR: ${e.message}")

            recordException(e)

            ready.set(false)
        }

        return ready.get()
    }

    override fun obtain() = engine

    override fun isInitialized() = ready.get()

    override fun isInitializing() = !isInitialized()

    override fun initializationCompleted(e: Exception?) {

        recordException(Exception(e))
    }
}