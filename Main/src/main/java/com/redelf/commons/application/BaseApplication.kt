package com.redelf.commons.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.GsonBuilder
import com.redelf.commons.BuildConfig
import com.redelf.commons.R
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.exec
import com.redelf.commons.execution.Executor
import com.redelf.commons.fcm.FcmService
import com.redelf.commons.firebase.FirebaseConfigurationManager
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.InitializationCondition
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.persistance.Data
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.persistance.Salter
import com.redelf.commons.recordException

import timber.log.Timber
import java.util.concurrent.RejectedExecutionException

abstract class BaseApplication : Application(), ContextAvailability {

    companion object : ContextAvailability {

        @SuppressLint("StaticFieldLeak")
        lateinit var CONTEXT: Context

        override fun takeContext() = CONTEXT
    }

    val managers = mutableListOf<DataManagement<*>>(

        FirebaseConfigurationManager
    )

    val defaultManagerResources = mutableMapOf<Class<*>, Int>()

    protected abstract fun onDoCreate()
    protected abstract fun takeSalt(): String

    protected open fun populateManagers() = listOf<DataManagement<*>>()

    protected open fun populateDefaultManagerResources() = mapOf<Class<*>, Int>()

    private val screenReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            if (Intent.ACTION_SCREEN_ON == intent.action) {

                onScreenOn()
                return
            }

            if (Intent.ACTION_SCREEN_OFF == intent.action) {

                onScreenOff()
            }
        }
    }

    private val fcmTokenReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                if (FcmService.BROADCAST_ACTION_TOKEN == it.action) {

                    val token = it.getStringExtra(FcmService.BROADCAST_KEY_TOKEN)

                    token?.let { tkn ->

                        if (isNotEmpty(tkn)) {

                            onFcmToken(tkn)
                        }
                    }
                }
            }
        }
    }

    private val fcmEventReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                if (FcmService.BROADCAST_ACTION_EVENT == intent.action) {

                    onFcmEvent(it)
                }
            }
        }
    }

    override fun takeContext() = CONTEXT

    override fun onCreate() {
        super.onCreate()

        CONTEXT = applicationContext

        managers.addAll(populateManagers())
        defaultManagerResources.putAll(populateDefaultManagerResources())

        if (BuildConfig.DEBUG) {

            Timber.plant(Timber.DebugTree())

            Timber.i("Application: Initializing")
        }


        Timber.i("Data: Initializing")

        val parser = GsonParser(

            GsonBuilder()
                .enableComplexMapKeySerialization()
                .create()
        )

        val salter = object : Salter {

            override fun getSalt(): String {

                var salt = takeSalt()

                if (salt.length > 5) {

                    salt = salt.substring(0, 5)
                }

                Timber.v("Get salt :: Length: ${salt.length}")

                return salt
            }
        }

        Data.init(applicationContext, getString(R.string.app_name), salter)
            .setParser(parser)
            .build()

        Timber.i("Data: Initialized")

        doCreate()
    }

    private fun doCreate() {

        try {

            exec {

                onDoCreate()

                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_SCREEN_ON)
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                registerReceiver(screenReceiver, intentFilter)

                val managersInitializerCallback = object : ManagersInitializer.InitializationCallback {

                    override fun onInitialization(success: Boolean, error: Throwable?) {

                        if (success) {

                            onManagersInitialized()

                        } else {

                            error?.let {

                                recordException(it)

                                throw it
                            }
                        }
                    }

                    override fun onInitialization(

                        manager: Management,
                        success: Boolean,
                        error: Throwable?

                    ) {

                        if (success) {

                            if (manager is InitializationCondition) {

                                Timber.i(

                                    "Manager: ${manager.javaClass.simpleName} " +
                                            "initialized (${manager.isInitialized()})"
                                )

                            } else {

                                Timber.i(

                                    "Manager: ${manager.javaClass.simpleName} initialized"
                                )
                            }

                        } else {

                            error?.let {

                                recordException(it)

                                throw it
                            }
                        }
                    }
                }

                initializeManagers(managersInitializerCallback)
            }

        } catch (e: RejectedExecutionException) {

            recordException(e)

            throw e
        }
    }

    protected open fun onScreenOn() {

        Timber.v("Screen is ON")
    }

    protected open fun onScreenOff() {

        Timber.v("Screen is FF")
    }

    protected open fun onFcmToken(token: String) {

        Timber.v("FCM: Token => $token")
    }

    protected open fun onFcmEvent(intent: Intent) {

        Timber.v("FCM: Event => $intent")
    }

    protected open fun onManagersReady() {

        Timber.i("Managers: Ready")
    }

    private fun initializeManagers(callback: ManagersInitializer.InitializationCallback) {

        ManagersInitializer().initializeManagers(

            managers = managers,
            callback = callback,
            context = applicationContext,
            defaultResources = defaultManagerResources
        )
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initializeFcm() {

        Timber.i("FCM: Initializing")

        val tokenFilter = IntentFilter(FcmService.BROADCAST_ACTION_TOKEN)
        val eventFilter = IntentFilter(FcmService.BROADCAST_ACTION_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(fcmTokenReceiver, tokenFilter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(fcmEventReceiver, eventFilter, Context.RECEIVER_NOT_EXPORTED)

        } else {

            registerReceiver(fcmTokenReceiver, tokenFilter)
            registerReceiver(fcmEventReceiver, eventFilter)
        }

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener(

                OnCompleteListener { task ->

                    if (!task.isSuccessful) {

                        Timber.w("FCM: Fetching registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    val token = task.result

                    if (isNotEmpty(token)) {

                        Timber.i("FCM: Initialized, token => $token")

                        onFcmToken(token)

                    } else {

                        Timber.i("FCM: Initialized with no token")
                    }
                }
            )
    }

    private fun onManagersInitialized() {

        initializeFcm()
        onManagersReady()
    }
}