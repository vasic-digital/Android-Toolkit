package com.redelf.commons.registration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap

class SafeBroadcastRegistrar(private val context: Context) {

    private val registered = ConcurrentHashMap<BroadcastReceiver, IntentFilter>()

    fun register(receiver: BroadcastReceiver, filter: IntentFilter) {

        val tag = "SafeBroadcastRegistrar :: Register :: ${receiver::class.simpleName} ::"

        if (registered.containsKey(receiver)) {

            Console.warning("$tag Already registered")
            return
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

            } else {

                context.registerReceiver(receiver, filter)
            }

            registered[receiver] = filter

            Console.log("$tag Registered")

        } catch (e: Exception) {

            Console.error("$tag Failed: ${e.message}")
        }
    }

    fun unregister(receiver: BroadcastReceiver) {

        val tag = "SafeBroadcastRegistrar :: Unregister :: ${receiver::class.simpleName} ::"

        if (!registered.containsKey(receiver)) {

            Console.warning("$tag Not registered")
            return
        }

        try {

            context.unregisterReceiver(receiver)

            Console.log("$tag Unregistered")

        } catch (e: IllegalArgumentException) {

            Console.warning("$tag Already unregistered: ${e.message}")

        } catch (e: Exception) {

            Console.error("$tag Failed: ${e.message}")

        } finally {

            registered.remove(receiver)
        }
    }

    fun unregisterAll() {

        val tag = "SafeBroadcastRegistrar :: Unregister all ::"

        Console.log("$tag START :: Count = ${registered.size}")

        val receivers = ArrayList(registered.keys)

        for (receiver in receivers) {

            unregister(receiver)
        }

        Console.log("$tag END")
    }

    fun isRegistered(receiver: BroadcastReceiver): Boolean {

        return registered.containsKey(receiver)
    }

    fun registeredCount(): Int {

        return registered.size
    }
}
