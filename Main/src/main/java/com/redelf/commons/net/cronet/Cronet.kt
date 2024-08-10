package com.redelf.commons.net.cronet

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationParametrized
import com.redelf.commons.lifecycle.InitializationParametrizedSync
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import org.chromium.net.CronetEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

object Cronet : InitializationParametrizedSync<Boolean, Context>, Obtain<CronetEngine?> {

    private val ready = AtomicBoolean()
    private var engine: CronetEngine? = null

    override fun initialize(param: Context) : Boolean {

        val latch = CountDownLatch(1)

        CronetProviderInstaller.installProvider(param).addOnCompleteListener { task ->

            if (task.isSuccessful) {

                Console.log("Cronet :: Provider installed successfully")

                engine = CronetEngine.Builder(param).build()
            }

            ready.set(true)

            latch.countDown()
        }

        latch.await()

        return engine != null
    }

    override fun obtain() = engine

    override fun isInitialized() = ready.get()

    override fun isInitializing() = !isInitialized()

    override fun initializationCompleted(e: Exception?) {

        recordException(Exception(e))
    }
}