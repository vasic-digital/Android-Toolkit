package com.redelf.commons.interprocess

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import java.util.concurrent.LinkedBlockingQueue

class InterprocessReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION = "com.redelf.commons.interprocess.action"
    }

    private val tag = "IPC :: Receiver ::"
    private val queue = LinkedBlockingQueue<Intent>()

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            Console.log("$tag Service connected")

            val context = BaseApplication.takeContext()

            service?.let {

                if (it is InterprocessService.InterprocessBinder) {

                    val interprocessService = it.getService()
                    val iterator = queue.iterator()

                    while (iterator.hasNext()) {

                        val intent = iterator.next()
                        interprocessService.onIntent(intent)
                    }
                }
            }

            context.unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

            Console.log("$tag Service disconnected")
        }
    }

    init {

        Console.log("$tag Created")
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        /*
        * FIXME: Nothing is received here
        */
        Console.log("$tag Received intent")

        intent?.let {

            if (it.action == ACTION) {

                Console.log("$tag Intent action :: ${it.action}")

                context?.let { ctx ->

                    queue.put(it)

                    val serviceIntent = Intent(ctx, InterprocessService::class.java)

                    ctx.startService(serviceIntent)
                    ctx.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }
}
