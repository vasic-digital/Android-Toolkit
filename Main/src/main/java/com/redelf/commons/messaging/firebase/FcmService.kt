package com.redelf.commons.messaging.firebase

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.net.connectivity.ConnectionState
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.Reconnect
import com.redelf.commons.registration.Registration
import com.redelf.commons.service.Serving
import java.util.concurrent.atomic.AtomicInteger


open class FcmService : FirebaseMessagingService(), Serving {

    companion object : Registration<ConnectivityStateChanges>, Reconnect {

        private val connState = AtomicInteger(ConnectionState.Disconnected.getState())
        private val connStateCallbacks = Callbacks<ConnectivityStateChanges>("FCM")

        const val BROADCAST_KEY_TOKEN = "key.token"
        const val BROADCAST_ACTION_TOKEN = "action.token"
        const val BROADCAST_ACTION_EVENT = "action.event"
        const val BROADCAST_ACTION_TOKEN_APPLIED = "action.token.applied"

        fun getState(): ConnectionState = ConnectionState.getState(connState.get())

        fun isConnected(): Boolean = getState() == ConnectionState.Connected

        fun isDisconnected(): Boolean = !isConnected()

        override fun register(subscriber: ConnectivityStateChanges) {

            connStateCallbacks.register(subscriber)
        }

        override fun unregister(subscriber: ConnectivityStateChanges) {

            connStateCallbacks.unregister(subscriber)
        }

        override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

            return connStateCallbacks.isRegistered(subscriber)
        }

        override fun reconnect() {

            val tag = "FCM :: Reconnect ::"

            exec(

                onRejected = {

                        err ->
                    {

                        Console.error("$tag ERROR: ${err.message}")
                        recordException(err)
                    }

                }
            ) {

                fun sendToken(token: String) {

                    if (isEmpty(token)) {

                        Console.error("$tag Token is empty")
                        return
                    }

                    Console.log("$tag Sending token: $token")

                    val intent = Intent(BROADCAST_ACTION_TOKEN)
                    intent.putExtra(BROADCAST_KEY_TOKEN, token)

                    val ctx = BaseApplication.takeContext()
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)

                    Console.log("$tag END")
                }

                Console.log("$tag START")

                FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->

                    sendToken(token)

                }.addOnFailureListener { e: Exception? ->

                    e?.let {

                        recordException(it)
                    }

                }.addOnCanceledListener {

                    Console.log("$tag Reconnect cancelled")

                }.addOnCompleteListener { task: Task<String> ->

                    val token = task.result
                    sendToken(token)
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                if (it.action == BROADCAST_ACTION_TOKEN_APPLIED) {

                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->

                        if (isEmpty(token)) {

                            setDisconnected()

                        } else {

                            val recToken = intent.getStringExtra(BROADCAST_KEY_TOKEN)

                            if (token == recToken) {

                                setConnected()

                            } else {

                                setDisconnected()
                            }
                        }

                    }.addOnFailureListener { e: Exception? ->

                        e?.let {

                            recordException(it)
                        }

                        setDisconnected()

                    }.addOnCanceledListener {

                        setDisconnected()

                    }.addOnCompleteListener { task: Task<String> ->

                        val token = task.result
                        val recToken = intent.getStringExtra(BROADCAST_KEY_TOKEN)

                        if (isNotEmpty(token) && token == recToken) {

                            setConnected()

                        } else {

                            setDisconnected()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        Console.log("onCreate()")

        registerReceiver(receiver, IntentFilter(BROADCAST_ACTION_TOKEN_APPLIED))
    }

    override fun onDestroy() {
        super.onDestroy()

        Console.log("onDestroy()")

        unregisterReceiver(receiver)
    }

    override fun onLowMemory() {
        super.onLowMemory()

        val e = IllegalStateException("onLowMemory()")
        recordException(e)
    }

    override fun onNewToken(token: String) {

        Console.info("New token available: $token")

        val intent = Intent(BROADCAST_ACTION_TOKEN)
        intent.putExtra(BROADCAST_KEY_TOKEN, token)
        sendBroadcast(intent)

        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        super.onMessageReceived(message)

        val data = message.data

        Console.info("New FCM message received: $data")

        wakeUpScreen()

        val intent = Intent(BROADCAST_ACTION_EVENT)

        data.forEach { (key, value) ->

            intent.putExtra(key, value)
        }

        sendBroadcast(intent)
    }

    @Suppress("DEPRECATION")
    private fun wakeUpScreen() {

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (!isScreenOn) {

            val tag = "WakeLock:1"

            val wl = powerManager.newWakeLock(

                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                tag
            )

            wl.acquire(2000)

            val wlCpu = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
            wlCpu.acquire(2000)
        }
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        receiver?.let { r ->
            filter?.let { f ->

                LocalBroadcastManager.getInstance(applicationContext).registerReceiver(r, f)
            }
        }

        return null
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {

        receiver?.let {

            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(it)
        }
    }

    override fun sendBroadcast(intent: Intent?) {

        intent?.let {

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(it)
        }
    }

    private fun setConnected() {

        connState.set(ConnectionState.Connected.getState())

        notifyCallbacks()
    }

    private fun setDisconnected() {

        connState.set(ConnectionState.Disconnected.getState())

        notifyCallbacks()
    }

    private fun notifyCallbacks() {

        connStateCallbacks.doOnAll(

            object : CallbackOperation<ConnectivityStateChanges> {

                override fun perform(callback: ConnectivityStateChanges) {

                    callback.onStateChanged()
                    callback.onState(ConnectionState.getState(connState.get()))
                }
            },

            operationName = "FCM_State_Changed"
        )
    }
}