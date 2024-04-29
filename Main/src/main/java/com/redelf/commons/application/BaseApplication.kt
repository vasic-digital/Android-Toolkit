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
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.profileinstaller.ProfileInstaller
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redelf.commons.BuildConfig
import com.redelf.commons.R
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.detectAllExpect
import com.redelf.commons.exec
import com.redelf.commons.fcm.FcmService
import com.redelf.commons.firebase.FirebaseConfigurationManager
import com.redelf.commons.isNotEmpty
import com.redelf.commons.logging.LogsGathering
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.managers.ManagersInitializer
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
        var TOP_ACTIVITY = mutableListOf<Class<out Activity>>()

        const val ACTIVITY_LIFECYCLE_TAG = "Activity lifecycle ::"

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

    private var telecomManager: TelecomManager? = null
    private var telephonyManager: TelephonyManager? = null

    val managers = mutableListOf<List<DataManagement<*>>>(

        listOf(FirebaseConfigurationManager)
    )

    val defaultManagerResources = mutableMapOf<Class<*>, Int>()

    protected open val detectPhoneCallReceived = false

    protected abstract fun onDoCreate()
    protected abstract fun takeSalt(): String

    protected open fun populateManagers() = listOf<List<DataManagement<*>>>()

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

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {

            super.onCallStateChanged(state, phoneNumber)

            when (state) {

                TelephonyManager.CALL_STATE_RINGING -> {

                    onPhoneIsRinging()
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {

                    Timber.v("Phone is OFF-HOOK")
                }

                TelephonyManager.CALL_STATE_IDLE -> {

                    Timber.v("Phone is IDLE")
                }
            }
        }
    }

    protected open fun onPhoneIsRinging() {

        Timber.v("Phone is RINGING")
    }

    override fun takeContext() = CONTEXT

    override fun onCreate() {
        super.onCreate()

        disableActivityAnimations(applicationContext)

        CONTEXT = applicationContext

        if (BuildConfig.DEBUG) {

            Timber.plant(Timber.DebugTree())

            Timber.i("Application: Initializing")

            enableStrictMode()
        }

        DataManagement.initialize(applicationContext)

        LogsGathering.ENABLED = isLogsGatheringEnabled()

        if (LogsGathering.ENABLED) {

            Timber.i("Logs gathering is enabled")
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

                onDoCreate()

                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_SCREEN_ON)
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                registerReceiver(screenReceiver, intentFilter)

                initializeManagers()
                onManagers()

                Timber.v("Installing profile: START")
                ProfileInstaller.writeProfile(applicationContext)
                Timber.v("Installing profile: END")

                if (detectPhoneCallReceived) {

                    telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
                }
            }

        } catch (e: RejectedExecutionException) {

            recordException(e)

            throw e
        }
    }

    protected open fun isLogsGatheringEnabled(): Boolean {

        return applicationContext.resources.getBoolean(R.bool.logs_gathering_enabled)
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

    private fun initializeManagers(): Boolean {

        var success = true

        managers.forEach {

            val result = ManagersInitializer().initializeManagers(

                managers = it,
                context = applicationContext,
                defaultResources = defaultManagerResources
            )

            if (!result) {

                success = false
            }
        }

        return success
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

        val clazz = activity::class.java

        TOP_ACTIVITY.add(clazz)

        Timber.v("$ACTIVITY_LIFECYCLE_TAG PRE-RESUMED :: ${clazz.simpleName}")

        Timber.d("$ACTIVITY_LIFECYCLE_TAG Top activity: ${clazz.simpleName}")

        super.onActivityPreResumed(activity)
    }

    override fun onActivityResumed(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG RESUMED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPostResumed(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG POST-RESUMED :: ${activity.javaClass.simpleName}")

        super.onActivityPostResumed(activity)
    }

    override fun onActivityPrePaused(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG PRE-PAUSED :: ${activity.javaClass.simpleName}")

        super.onActivityPrePaused(activity)
    }

    override fun onActivityPaused(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG PAUSED :: ${activity.javaClass.simpleName}")

        super.onActivityPrePaused(activity)
    }

    override fun onActivityPostPaused(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG POST-PAUSED :: ${activity.javaClass.simpleName}")

        super.onActivityPostPaused(activity)
    }

    override fun onActivityPreStopped(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG PRE-STOPPED :: ${activity.javaClass.simpleName}")

        super.onActivityPreStopped(activity)
    }

    override fun onActivityStopped(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG STOPPED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPostStopped(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG POST-STOPPED :: ${activity.javaClass.simpleName}")

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

        val iterator = TOP_ACTIVITY.iterator()

        while (iterator.hasNext()) {

            val item = iterator.next()

            if (item == activity::class.java) {

                iterator.remove()
            }
        }

        Timber.v("$ACTIVITY_LIFECYCLE_TAG PRE-DESTROYED :: ${activity.javaClass.simpleName}")

        if (TOP_ACTIVITY.isEmpty()) {

            Timber.d("$ACTIVITY_LIFECYCLE_TAG No top activity")

        } else {

            val clazz = TOP_ACTIVITY.last()

            Timber.d("$ACTIVITY_LIFECYCLE_TAG Top activity: ${clazz.simpleName}")
        }

        super.onActivityPreDestroyed(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {

        Timber.v("$ACTIVITY_LIFECYCLE_TAG DESTROYED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPostDestroyed(activity: Activity) {

        // Ignore

        super.onActivityPostDestroyed(activity)
    }

    private fun onManagers() {

        initializeFcm()
        onManagersReady()
    }

    private fun enableStrictMode() {

        Timber.v("Enable Strict Mode, disabled=$STRICT_MODE_DISABLED")

        if (STRICT_MODE_DISABLED) {

            return
        }

        StrictMode.setThreadPolicy(

            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(

            StrictMode.VmPolicy.Builder()
                .detectAllExpect("android.os.StrictMode.onUntaggedSocket")
                .build()
        )
    }

    @Suppress("DEPRECATION")
    protected fun disableActivityAnimations(context: Context) {

        try {

            val scale = 0
            val contentResolver = context.contentResolver

            Settings.System.putFloat(
                contentResolver,
                Settings.System.WINDOW_ANIMATION_SCALE,
                scale.toFloat()
            )

            Settings.System.putFloat(
                contentResolver,
                Settings.System.TRANSITION_ANIMATION_SCALE,
                scale.toFloat()
            )

            Settings.System.putFloat(
                contentResolver,
                Settings.System.ANIMATOR_DURATION_SCALE,
                scale.toFloat()
            )

        } catch (e: Throwable) {

            Timber.e(e)
        }
    }
}