package com.redelf.commons.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe utility that tracks broadcast receiver registrations and ensures
 * they are properly unregistered to prevent leak warnings.
 *
 * Usage:
 * ```kotlin
 * private val registrar = SafeBroadcastRegistrar()
 *
 * // In onCreate/onStart:
 * registrar.register(context, myReceiver, IntentFilter("MY_ACTION"))
 *
 * // In onDestroy/onStop:
 * registrar.unregisterAll(context)
 * ```
 */
class SafeBroadcastRegistrar {

    private val receivers = ConcurrentHashMap<BroadcastReceiver, String>()

    /**
     * Register a broadcast receiver and track it for later cleanup.
     *
     * @param context The context to register with (preferably Application context for long-lived receivers)
     * @param receiver The BroadcastReceiver to register
     * @param filter The IntentFilter for the receiver
     * @param tag Optional tag for logging purposes
     * @return true if registration succeeded, false otherwise
     */
    fun register(
        context: Context,
        receiver: BroadcastReceiver,
        filter: IntentFilter,
        tag: String = receiver.javaClass.simpleName
    ): Boolean {

        if (receivers.containsKey(receiver)) {

            Console.warning("SafeBroadcastRegistrar :: Already registered: $tag")
            return false
        }

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

            } else {

                context.registerReceiver(receiver, filter)
            }

            receivers[receiver] = tag
            Console.log("SafeBroadcastRegistrar :: Registered: $tag")
            true

        } catch (e: Exception) {

            Console.error("SafeBroadcastRegistrar :: Failed to register $tag: ${e.message}")
            false
        }
    }

    /**
     * Unregister a specific receiver.
     *
     * @param context The context the receiver was registered with
     * @param receiver The BroadcastReceiver to unregister
     * @return true if unregistration succeeded, false otherwise
     */
    fun unregister(context: Context, receiver: BroadcastReceiver): Boolean {

        val tag = receivers.remove(receiver) ?: return false

        return try {

            context.unregisterReceiver(receiver)
            Console.log("SafeBroadcastRegistrar :: Unregistered: $tag")
            true

        } catch (e: IllegalArgumentException) {

            Console.warning("SafeBroadcastRegistrar :: Already unregistered: $tag")
            true
        }
    }

    /**
     * Unregister all tracked receivers. Call this in onDestroy() or onStop().
     *
     * @param context The context the receivers were registered with
     */
    fun unregisterAll(context: Context) {

        val tag = "SafeBroadcastRegistrar :: UnregisterAll ::"
        val size = receivers.size

        if (size == 0) return

        Console.log("$tag START :: Count = $size")

        val iterator = receivers.entries.iterator()

        while (iterator.hasNext()) {

            val entry = iterator.next()

            try {

                context.unregisterReceiver(entry.key)
                Console.log("$tag Unregistered: ${entry.value}")

            } catch (e: IllegalArgumentException) {

                Console.warning("$tag Already unregistered: ${entry.value}")
            }

            iterator.remove()
        }

        Console.log("$tag END")
    }

    /**
     * Check if a receiver is currently registered.
     */
    fun isRegistered(receiver: BroadcastReceiver): Boolean = receivers.containsKey(receiver)

    /**
     * Get the count of currently registered receivers.
     */
    fun size(): Int = receivers.size
}
