package com.redelf.commons.management

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.connectivity.Connectivity
import com.redelf.commons.recordException
import com.redelf.commons.registration.Registration
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

abstract class LazyDataManagement<T> : DataManagement<T>(), Registration<Context> {

    protected open val lazySaving = false

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

                        if (takeContext().getActivityCount() >= 2) {

                            onBackground()
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

        try {

            val filter = IntentFilter()

            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
            filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
            filter.addAction("android.net.wifi.STATE_CHANGE")

            ctx.registerReceiver(connectivityListener, filter)

        } catch (e: Exception) {

            Timber.e(e)
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

    private fun onForeground() {

        Timber.v("Application is in foreground")
    }

    private fun onBackground() {

        if (!lazySaving) {

            return
        }

        if (isLocked()) {

            return
        }

        val tag = "Application went to background :: ${getWho()} ::"

        Timber.v("$tag START")

        try {

            Timber.v("$tag SAVING")

            val data = obtain()
            overwriteData(data)

            Timber.v("$tag SAVED")

        } catch (e: IllegalStateException) {

            Timber.e(tag, e)
        }

        Timber.v("$tag END")
    }
}