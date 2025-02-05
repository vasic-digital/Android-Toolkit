package com.redelf.commons.net.cronet

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.redelf.commons.extensions.recordException
import com.redelf.commons.lifecycle.InitializationParametrizedSync
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import org.chromium.net.CronetEngine
import java.util.concurrent.atomic.AtomicBoolean

object Cronet : InitializationParametrizedSync<Boolean, Context>, Obtain<CronetEngine?> {

    private val tag = "Cronet ::"
    private val ready = AtomicBoolean()
    private var engine: CronetEngine? = null

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