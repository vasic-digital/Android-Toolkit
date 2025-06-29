package com.redelf.commons.management

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.application.OnClearFromRecentService
import com.redelf.commons.data.Empty
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.Connectivity
import com.redelf.commons.registration.Registration
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.atomic.AtomicBoolean

abstract class LazyDataManagement<T> :

    DataManagement<T>(),
    Registration<Context>

        where T : Versionable

{

    protected open val lazySaving = false
    protected open val savingOnTermination = false
    protected open val triggerOnBackgroundForScreenOff = false

    private val saved = AtomicBoolean()
    private val registered = AtomicBoolean()
    private val terminationRegistered = AtomicBoolean()

    private val terminationReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (!isEnabled()) {

                return
            }

            if (!savingOnTermination) {

                return
            }

            intent?.let {

                if (it.action == OnClearFromRecentService.ACTION) {

                    onBackground("Termination received", sync = false)
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (!isEnabled()) {

                return
            }

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

                                onBackground(it.action ?: "", sync = false)

                            } else {

                                Console.log("$tag SKIPPING")
                            }
                        }
                    }

                    BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND -> {

                        exec {

                            onBackground(it.action ?: "", sync = false)
                        }
                    }
                }
            }
        }
    }

    private val connectivityListener = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (!isEnabled()) {

                return
            }

            context?.let {

                val conn = Connectivity()

                if (!conn.isNetworkAvailable(it)) {

                    onBackground("Offline", sync = false)
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

            } catch (e: Throwable) {

                Console.error(e)
            }
        }
    }

    override fun register(subscriber: Context) {

        if (!isEnabled()) {

            return
        }

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

        } catch (e: Throwable) {

            recordException(e)
        }

        if (savingOnTermination) {

            try {

                val filter = IntentFilter()

                filter.addAction(OnClearFromRecentService.ACTION)

                LocalBroadcastManager
                    .getInstance(subscriber.applicationContext)
                    .registerReceiver(terminationReceiver, filter)

                terminationRegistered.set(true)

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }

    override fun unregister(subscriber: Context) {

        if (!isEnabled()) {

            return
        }

        if (registered.get()) {

            try {

                LocalBroadcastManager
                    .getInstance(subscriber.applicationContext)
                    .unregisterReceiver(receiver)

                registered.set(false)

            } catch (e: Throwable) {

                recordException(e)
            }
        }

        if (terminationRegistered.get()) {

            try {

                LocalBroadcastManager
                    .getInstance(subscriber.applicationContext)
                    .unregisterReceiver(terminationReceiver)

                terminationRegistered.set(false)

            } catch (e: Throwable) {

                recordException(e)
            }
        }
    }

    override fun isRegistered(subscriber: Context) = registered.get() && terminationRegistered.get()

    @Throws(IllegalStateException::class)
    override fun pushData(data: T, sync: Boolean) {

        if (!isEnabled()) {

            return
        }

        if (lazySaving) {

            saved.set(false)

        } else {

            super.pushData(data, sync)
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

    protected open fun isLazyReady() = isEnabled()

    private fun onForeground() {

        if (!isEnabled()) {

            return
        }

        if (DEBUG.get()) Console.log("Application is in foreground")
    }

    protected fun forceSave(sync: Boolean) {

        if (!isEnabled()) {

            return
        }

        onBackground("forceSave", sync)
    }

    /*
    * FIXME: The method is fired even we are surfing through the screens
    */
    private fun onBackground(from: String, sync: Boolean) {

        if (!isEnabled()) {

            return
        }

        if (!lazySaving) {

            return
        }

        val tag = "Lazy :: Who = '${getWho()}', From = '$from' :: BACKGROUND ::"

        if (isLazyReady()) {

            if (DEBUG.get()) Console.log("$tag READY")

        } else {

            if (DEBUG.get()) Console.warning("$tag NOT READY")

            return
        }

        if (isLocked()) {

            if (DEBUG.get()) Console.log("LOCKED")

            return
        }

        if (DEBUG.get()) Console.log("$tag START")

        try {

            if (DEBUG.get()) Console.log("$tag SAVING")

            val data = obtain()
            var empty: Boolean? = null

            if (data is Empty) {

                empty = data.isEmpty()
            }

            data?.let {

                overwriteData(data)

                doPushData(it, sync = sync)
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