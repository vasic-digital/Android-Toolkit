package com.redelf.commons.interprocess

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

abstract class InterprocessApplication : BaseApplication() {

    protected abstract fun getProcessors(): List<InterprocessProcessor>

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            Console.log("Service connected")

            service?.let {

                if (it is InterprocessService.InterprocessBinder) {

                    val processors = getProcessors()
                    val interprocessService = it.getService()

                    processors.forEach { processor -> interprocessService.register(processor) }
                }
            }

            unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

            Console.log("Service disconnected")
        }
    }

    override fun onDoCreate() {
        super.onDoCreate()

        val ctx = applicationContext
        val serviceIntent = Intent(ctx, InterprocessService::class.java)

        ctx.startService(serviceIntent)
        ctx.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
}