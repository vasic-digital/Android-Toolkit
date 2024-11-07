package com.redelf.commons.management

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.data.Empty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.Connectivity
import com.redelf.commons.registration.Registration
import java.util.concurrent.atomic.AtomicBoolean

abstract class LazyDataManagement<T> : DataManagement<T>(), Registration<Context> {

    protected open val lazySaving = false
    protected open val triggerOnBackgroundForScreenOff = false

    private val saved = AtomicBoolean()
    private val registered = AtomicBoolean()

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                when (it.action) {

                    BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_FOREGROUND -> {

                        onForeground()
                    }

                    BaseApplication.BROADCAST_ACTION_APPLICATION_SCREEN_OFF -> {

                        if (takeContext().getActivityCount() >= 1) {

                            val tag = "Application is in background for screen off ::"

                            if (triggerOnBackgroundForScreenOff) {

                                if (DEBUG.get()) Console.log("$tag OK")

                                onBackground()

                            } else {

                                Console.log("$tag SKIPPING")
                            }
                        }
                    }

                    BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND -> {

                        onBackground()
                    }
                }
            }
        }
    }

    private val connectivityListener = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            context?.let {

                val conn = Connectivity()

                if (!conn.isNetworkAvailable(it)) {

                    onBackground()
                }
            }
        }
    }

    override fun injectContext(ctx: BaseApplication) {

        if (lazySaving) {

            try {

                val filter = IntentFilter()

                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
                filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
                filter.addAction("android.net.wifi.STATE_CHANGE")

                ctx.registerReceiver(connectivityListener, filter)

            } catch (e: Exception) {

                Console.error(e)
            }
        }
    }

    override fun register(subscriber: Context) {

        if (registered.get()) {

            return
        }

        try {

            val filter = IntentFilter()

            filter.addAction(BaseApplication.BROADCAST_ACTION_APPLICATION_SCREEN_OFF)
            filter.addAction(BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND)
            filter.addAction(BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_FOREGROUND)

            LocalBroadcastManager
                .getInstance(subscriber.applicationContext)
                .registerReceiver(receiver, filter)

            registered.set(true)

        } catch (e: Exception) {

            recordException(e)
        }
    }

    override fun unregister(subscriber: Context) {

        if (!registered.get()) {

            return
        }

        try {

            LocalBroadcastManager
                .getInstance(subscriber.applicationContext)
                .unregisterReceiver(receiver)

            registered.set(false)

        } catch (e: Exception) {

            recordException(e)
        }
    }

    override fun isRegistered(subscriber: Context) = registered.get()

    @Throws(IllegalStateException::class)
    override fun pushData(data: T) {

        if (lazySaving) {

            saved.set(false)

        } else {

            super.pushData(data)
        }
    }

    override fun onDataPushed(success: Boolean?, err: Throwable?) {
        super.onDataPushed(success, err)

        success?.let {

            if (it) {

                saved.set(true)
            }
        }
    }

    protected open fun isLazyReady() = true

    private fun onForeground() {

        if (DEBUG.get()) Console.log("Application is in foreground")
    }

    private fun onBackground() {

        if (!lazySaving) {

            return
        }

        if (isLazyReady()) {

            if (DEBUG.get()) Console.log("Lazy :: Ready :: Who = ${getWho()}")

        } else {

            if (DEBUG.get()) Console.warning("Lazy :: Not ready :: Who = ${getWho()}")

            return
        }

        if (isLocked()) {

            if (DEBUG.get()) Console.log("Locked")
            return
        }

        val tag = "Application went to background :: ${getWho()} ::"

        if (DEBUG.get()) Console.log("$tag START")

        try {

            if (DEBUG.get()) Console.log("$tag SAVING")

            val data = obtain()
            var empty: Boolean? = null

            if (data is Empty) {

                empty = data.isEmpty()
            }

            overwriteData(data)

            data?.let {

                doPushData(it)
            }

            empty?.let {

                if (DEBUG.get()) Console.log("$tag SAVED :: Empty = $it")
            }

            if (empty == null) {

                if (DEBUG.get()) Console.log("$tag SAVED")
            }

        } catch (e: IllegalStateException) {

            Console.error(tag, e)
        }

        if (DEBUG.get()) Console.log("$tag END")
    }
}