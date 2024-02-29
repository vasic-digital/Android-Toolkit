package com.redelf.commons.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redelf.commons.BuildConfig
import com.redelf.commons.R
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.exec
import com.redelf.commons.fcm.FcmService
import com.redelf.commons.firebase.FirebaseConfigurationManager
import com.redelf.commons.isNotEmpty
import com.redelf.commons.lifecycle.InitializationCondition
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.Data
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.persistance.Parser
import com.redelf.commons.persistance.Salter
import com.redelf.commons.recordException
import timber.log.Timber
import java.util.concurrent.RejectedExecutionException

abstract class BaseApplication :

    Application(),
    ContextAvailability,
    ActivityLifecycleCallbacks,
    LifecycleObserver

{

    companion object : ContextAvailability, ApplicationVersion {

        @SuppressLint("StaticFieldLeak")
        lateinit var CONTEXT: Context

        var STRICT_MODE_DISABLED = false

        override fun takeContext() = CONTEXT

        fun restart(context: Context) {

            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }

        override fun getVersion(): String {

            try {

                val context = takeContext()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                return packageInfo.versionName

            } catch (e: NameNotFoundException) {

                recordException(e)
            }

            return ""
        }

        @Suppress("DEPRECATION")
        @SuppressLint("ObsoleteSdkInt")
        override fun getVersionCode(): String {

            try {

                val context = takeContext()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                    packageInfo.longVersionCode.toString()

                } else {

                    packageInfo.versionCode.toString()
                }

            } catch (e: NameNotFoundException) {

                recordException(e)
            }

            return ""
        }
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

        if (BuildConfig.DEBUG) {

            Timber.plant(Timber.DebugTree())

            Timber.i("Application: Initializing")

            if (!STRICT_MODE_DISABLED) {

                enableStrictMode()
            }
        }


        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)

        managers.addAll(populateManagers())
        defaultManagerResources.putAll(populateDefaultManagerResources())

        doCreate()
    }

    private fun doCreate() {

        try {

            exec {

                Timber.i("Data: Initializing")

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

                val getParser = object : Obtain<Parser?> {

                    override fun obtain(): Parser {

                        return GsonParser(

                            object : Obtain<Gson> {

                                override fun obtain(): Gson {

                                    return GsonBuilder()
                                        .enableComplexMapKeySerialization()
                                        .create()
                                }
                            }
                        )
                    }
                }

                Data.init(applicationContext, getString(R.string.app_name), salter)
                    .setParser(getParser)
                    .build()

                Timber.i("Data: Initialized")

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

        Timber.v("Screen is OFF")
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

        LocalBroadcastManager.getInstance(this).registerReceiver(fcmTokenReceiver, tokenFilter)
        LocalBroadcastManager.getInstance(this).registerReceiver(fcmEventReceiver, eventFilter)

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

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        // Ignore
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {

        // Ignore

        super.onActivityPostCreated(activity, savedInstanceState)
    }

    override fun onActivityPreStarted(activity: Activity) {

        // Ignore

        super.onActivityPreStarted(activity)
    }

    override fun onActivityStarted(activity: Activity) {

        // Ignore
    }

    override fun onActivityPostStarted(activity: Activity) {

        // Ignore

        super.onActivityPostStarted(activity)
    }

    override fun onActivityPreResumed(activity: Activity) {

        // Ignore

        super.onActivityPreResumed(activity)
    }

    override fun onActivityResumed(activity: Activity) {

        // Ignore
    }

    override fun onActivityPostResumed(activity: Activity) {

        // Ignore

        super.onActivityPostResumed(activity)
    }

    override fun onActivityPrePaused(activity: Activity) {

        // Ignore

        super.onActivityPrePaused(activity)
    }

    override fun onActivityPaused(activity: Activity) {

        // Ignore
    }

    override fun onActivityPostPaused(activity: Activity) {

        // Ignore

        super.onActivityPostPaused(activity)
    }

    override fun onActivityPreStopped(activity: Activity) {

        // Ignore

        super.onActivityPreStopped(activity)
    }

    override fun onActivityStopped(activity: Activity) {

        // Ignore
    }

    override fun onActivityPostStopped(activity: Activity) {

        // Ignore

        super.onActivityPostStopped(activity)
    }

    override fun onActivityPreSaveInstanceState(activity: Activity, outState: Bundle) {

        // Ignore

        super.onActivityPreSaveInstanceState(activity, outState)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

        // Ignore
    }

    override fun onActivityPostSaveInstanceState(activity: Activity, outState: Bundle) {

        // Ignore

        super.onActivityPostSaveInstanceState(activity, outState)
    }

    override fun onActivityPreDestroyed(activity: Activity) {

        // Ignore

        super.onActivityPreDestroyed(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {

        // Ignore
    }

    override fun onActivityPostDestroyed(activity: Activity) {

        // Ignore

        super.onActivityPostDestroyed(activity)
    }



    private fun onManagersInitialized() {

        initializeFcm()
        onManagersReady()
    }

    private fun enableStrictMode() {

        Timber.v("Enable Strict Mode")

        StrictMode.setThreadPolicy(

            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(

            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}