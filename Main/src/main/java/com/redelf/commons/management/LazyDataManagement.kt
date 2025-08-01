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
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.registration.Registration
import com.redelf.commons.versioning.Versionable
import java.util.concurrent.atomic.AtomicBoolean

abstract class LazyDataManagement<T> :

    DataManagement<T>(),
    Registration<Context> where T : Versionable

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

                    onBackground("Termination received")
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

                                onBackground(it.action ?: "")

                            } else {

                                Console.log("$tag SKIPPING")
                            }
                        }
                    }

                    BaseApplication.BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND -> {

                        exec {

                            onBackground(it.action ?: "")
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

                    onBackground("Offline")
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

    override fun pushData(

        data: T?,
        from: String,
        notify: Boolean,
        callback: OnObtain<DataPushResult?>?

    ) {

        val from = "lazy.pushData(from='$from').withData.withCallback"

        if (!isEnabled()) {

            callback?.onCompleted(data = DataPushResult(from, false))

            return
        }

        if (lazySaving) {

            saved.set(false)

            val result = DataPushResult(from, true)

            callback?.onCompleted(data = result)

            if (notify) {

                notifyOnPushCompleted(data = result)
            }

        } else {

            super.pushData(data, from, notify, callback)
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

    private fun onBackground(from: String) {

        if (!isEnabled()) {

            return
        }

        if (!lazySaving) {

            return
        }

        val tag =
            "Lazy :: Who = '${getWho()}', From = '$from' :: BACKGROUND (${takeContext().getActivityCount()}) ::"

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

            obtain(

                object : OnObtain<T?> {

                    override fun onCompleted(data: T?) {

                        var empty: Boolean? = null

                        if (data is Empty) {

                            empty = data.isEmpty()
                        }

                        data?.let {

                            overwriteData(it)

                            doPushData(

                                it,
                                "onBackground(from='$from')",
                                false
                            )
                        }

                        empty?.let {

                            if (DEBUG.get()) Console.log("$tag SAVED :: Empty = $it")
                        }

                        if (empty == null) {

                            if (DEBUG.get()) Console.log("$tag SAVED")
                        }
                    }

                    override fun onFailure(error: Throwable) {

                        recordException(error)
                    }
                }
            )

        } catch (e: Throwable) {

            recordException(e)
        }

        if (DEBUG.get()) Console.log("$tag END")
    }
}