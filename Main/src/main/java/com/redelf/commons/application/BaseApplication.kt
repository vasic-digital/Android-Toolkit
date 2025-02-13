@file:Suppress("DEPRECATION")

package com.redelf.commons.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.BackgroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.redelf.commons.R
import com.redelf.commons.activity.ActivityCount
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.detectAllExpect
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.toast
import com.redelf.commons.intention.Intentional
import com.redelf.commons.interprocess.InterprocessData
import com.redelf.commons.loading.Loadable
import com.redelf.commons.logging.Console
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.managers.ManagersInitializer
import com.redelf.commons.messaging.firebase.FcmService
import com.redelf.commons.messaging.firebase.FirebaseConfigurationManager
import com.redelf.commons.migration.MigrationNotReadyException
import com.redelf.commons.net.cronet.Cronet
import com.redelf.commons.net.retrofit.RetryInterceptor
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.SharedPreferencesStorage
import com.redelf.commons.security.management.SecretsManager
import com.redelf.commons.security.obfuscation.DefaultObfuscator
import com.redelf.commons.security.obfuscation.Obfuscator
import com.redelf.commons.security.obfuscation.RemoteObfuscatorSaltProvider
import com.redelf.commons.settings.SettingsManager
import com.redelf.commons.updating.Updatable
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.reflect.KClass

abstract class BaseApplication :

    Intentional,
    Application(),
    ActivityCount,
    Updatable<Long>,
    LifecycleObserver,
    ActivityLifecycleCallbacks,
    ContextAvailability<BaseApplication>

{

    companion object :

        Intentional,
        ApplicationInfo,
        ContextAvailability<BaseApplication> {

        val DEBUG = AtomicBoolean()
        val STRICT_MODE_DISABLED = AtomicBoolean()

        @SuppressLint("StaticFieldLeak")
        lateinit var CONTEXT: BaseApplication

        var TOP_ACTIVITY = mutableListOf<Class<out Activity>>()
        var TOP_ACTIVITIES = mutableListOf<Class<out Activity>>()

        const val ACTIVITY_LIFECYCLE_TAG = "Activity lifecycle ::"
        const val BROADCAST_ACTION_APPLICATION_SCREEN_OFF = "APPLICATION_STATE.SCREEN_OFF"
        const val BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND = "APPLICATION_STATE.BACKGROUND"
        const val BROADCAST_ACTION_APPLICATION_STATE_FOREGROUND = "APPLICATION_STATE.FOREGROUND"

        val ALARM_SERVICE_JOB_ID_MIN = AtomicInteger(4001)
        val ALARM_SERVICE_JOB_ID_MAX = AtomicInteger(8000)

        override fun takeContext() = CONTEXT

        override fun takeIntent(): Intent? = CONTEXT.takeIntent()

        private var isAppInBackground = AtomicBoolean()

        fun restart(context: Context) {

            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }

        override fun getName(): String {

            try {

                val context = takeContext()
                val pm = context.packageManager
                val packageInfo = pm.getPackageInfo(context.packageName, 0)
                val ai = packageInfo.applicationInfo

                return if (ai != null) pm.getApplicationLabel(ai) as String else "Unknown"

            } catch (e: NameNotFoundException) {

                recordException(e)
            }

            return "Unknown"
        }

        override fun getVersion(): String {

            try {

                val context = takeContext()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                val vName = packageInfo.versionName

                if (isEmpty(vName)) {

                    return "unknown"
                }

                return vName ?: "unknown"

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

    open val useCronet = false
    open val detectAudioStreamed = false
    open val detectPhoneCallReceived = false
    open val toastOnApiCommunicationFailure = false
    open val secretsKey = "com.redelf.commons.security.secrets"
    open val defaultManagerResources = mutableMapOf<Class<*>, Int>()

    protected open val firebaseEnabled = true
    protected open val facebookEnabled = false
    protected open val firebaseAnalyticsEnabled = false

    protected open val managers = mutableListOf<List<DataManagement<*>>>(

        listOf(

            FirebaseConfigurationManager
        )
    )

    protected open val contextDependentManagers = mutableListOf<Obtain<DataManagement<*>>>(

        object : Obtain<DataManagement<*>> {

            override fun obtain(): DataManagement<*> {

                return SecretsManager.obtain()
            }
        }
    )

    protected val managersReady = AtomicBoolean()
    protected val audioFocusTag = "Audio focus ::"

    private val updating = AtomicBoolean()
    private val updatingTag = "Updating ::"
    private var secretKey: SecretKey? = null
    private val prefsKeyUpdate = "Preferences.Update"
    private var telecomManager: TelecomManager? = null
    private val lastCommunicationErrorTime = AtomicLong()
    private var telephonyManager: TelephonyManager? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private val registeredForPhoneCallsDetection = AtomicBoolean()
    private val registeredForAudioFocusDetection = AtomicBoolean()

    init {

        try {

            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256) // AES-256
            secretKey = keyGen.generateKey()

        } catch (e: Exception) {

            recordException(e)
        }
    }

    open fun getSecret() = secretKey

    open fun canRecordApplicationLogs() = false

    abstract fun isProduction(): Boolean

    protected abstract fun takeSalt(): String

    protected open fun onDoCreate() = Unit

    protected open fun populateManagers() = listOf<List<DataManagement<*>>>()

    protected open fun getDisabledManagers() = listOf<List<DataManagement<*>>>()

    protected open fun populateDefaultManagerResources() = mapOf<Class<*>, Int>()

    protected lateinit var prefs: SharedPreferencesStorage

    private val apiCommunicationFailureListener = object : BroadcastReceiver() {

        private val filterAction = RetryInterceptor.BROADCAST_ACTION_COMMUNICATION_FAILURE

        fun getIntentFilter() = IntentFilter(filterAction)

        override fun onReceive(ctx: Context?, intent: Intent?) {

            val tag = "${RetryInterceptor.TAG} Received broadcast ::"

            Console.log("$tag START")

            intent?.let {

                Console.log("$tag Intent OK")

                it.action?.let { action ->

                    Console.log("$tag Action :: Value = $action")

                    if (action == filterAction) {

                        Console.log("$tag Action :: OK")

                        if (System.currentTimeMillis() - lastCommunicationErrorTime.get() >= 3000) {

                            Console.log("$tag Show toast :: START")

                            val msg = getString(R.string.connectivity_failure)

                            toast(msg)

                            lastCommunicationErrorTime.set(System.currentTimeMillis())

                            Console.log("$tag Show toast :: END")

                        } else {

                            Console.warning("$tag Show toast :: SKIPPED")
                        }
                    }
                }
            }

            Console.log("$tag END")
        }
    }

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

                    Console.log("Phone is OFF-HOOK")
                }

                TelephonyManager.CALL_STATE_IDLE -> {

                    Console.log("Phone is IDLE")
                }
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->

        when (focusChange) {

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                Console.log("$audioFocusTag Transient or can dock")

                streamVolumeDown()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {

                streamStop()
            }

            AudioManager.AUDIOFOCUS_LOSS -> {

                Console.log("$audioFocusTag Lost")

                streamStop()
            }

            AudioManager.AUDIOFOCUS_GAIN -> {

                Console.log("$audioFocusTag Gained")

                if (streamPlay()) {

                    streamVolumeUp()
                }
            }
        }
    }

    fun registerPhoneStateListener() {

        val tag = "Register phone state listener ::"

        Console.log("$tag START")

        if (registeredForPhoneCallsDetection.get()) {

            Console.log("$tag Already registered")

            return
        }

        if (detectPhoneCallReceived) {

            Console.log("$tag Phone calls detection enabled")

            telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            try {

                @Suppress("DEPRECATION")
                telephonyManager?.listen(

                    phoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE
                )

                Console.log("$tag Phone state listener registered with success")

                registeredForPhoneCallsDetection.set(true)

            } catch (e: SecurityException) {

                Console.error(tag, e)
            }

        } else {

            Console.log("$tag Phone calls detection disabled")
        }
    }

    fun registerAudioFocusChangeListener() {

        val tag = "$audioFocusTag Register listener ::"

        Console.log("$tag START")

        if (registeredForAudioFocusDetection.get()) {

            Console.log("$tag Already registered")

            return
        }

        if (detectAudioStreamed) {

            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?

            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest)

            registeredForAudioFocusDetection.set(true)

            Console.log("$tag END")

        } else {

            Console.log("$tag Audio focus detection disabled")
        }
    }

    protected open fun onPhoneIsRinging() {

        Console.log("Phone is RINGING")
    }

    protected open fun streamStop() {

        Console.log("$audioFocusTag Stream :: STOP")
    }

    protected open fun streamVolumeUp() {

        Console.log("$audioFocusTag Stream :: VOLUME UP")
    }

    protected open fun streamVolumeDown() {

        Console.log("$audioFocusTag Stream :: VOLUME DOWN")
    }

    protected open fun streamPlay(): Boolean {

        Console.log("$audioFocusTag Stream :: PLAY")

        return false
    }

    override fun takeContext() = CONTEXT

    override fun takeIntent(): Intent? {

        try {

            return packageManager.getLaunchIntentForPackage(packageName)

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }

    protected open fun isStrictModeDisabled() = !DEBUG.get()

    fun enableLogsRecording() {

        if (DEBUG.get() || canRecordApplicationLogs()) {

            Console.initialize(canRecordApplicationLogs(), production = isProduction())

            Console.info("Application :: Initializing")

            enableStrictMode()
        }
    }

    override fun onCreate() {
        super.onCreate()

        initTerminationListener()
        initFirebaseWithAnalytics()
        initFacebook()

        prefs = SharedPreferencesStorage(applicationContext)

        disableActivityAnimations(applicationContext)

        CONTEXT = this
        DEBUG.set(CONTEXT.resources.getBoolean(R.bool.debug))
        STRICT_MODE_DISABLED.set(isStrictModeDisabled())

        enableLogsRecording()

        if (useCronet) {

            exec {

                Cronet.initialize(applicationContext)
            }
        }

        DataManagement.initialize(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)

        managers.addAll(populateManagers())

        val contextDependableManagers = mutableListOf<DataManagement<*>>()

        contextDependentManagers.forEach { contextDependentManager ->

            contextDependableManagers.add(contextDependentManager.obtain())
        }

        managers.addAll(listOf(contextDependableManagers))

        defaultManagerResources.putAll(populateDefaultManagerResources())

        doCreate()
    }

    fun initTerminationListener() {

        val tag = "${OnClearFromRecentService.TAG} INIT ::"

        Console.log("$tag START")

        if (OnClearFromRecentService.isRunning()) {

            Console.log("$tag ALREADY RUNNING")
            return
        }

        Console.log("$tag STARTING")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            try {

                val intent = Intent(applicationContext, OnClearFromRecentService::class.java)
                startService(intent)

                Console.log("$tag END")

            } catch (e: BackgroundServiceStartNotAllowedException) {

                Console.error("$tag ERROR: ${e.message}")

            } catch (e: Exception) {

                recordException(e)
            }

        } else {

            try {

                val intent = Intent(applicationContext, OnClearFromRecentService::class.java)
                startService(intent)

                Console.log("$tag END")

            } catch (e: Exception) {

                recordException(e)
            }
        }
    }

    /*
    * TODO: Incorporate support for Samsung AppStore, RuStore, Huawei AppGallery, etc.
    */
    fun checkGooglePlayServices(): Boolean {

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun doCreate() {

        try {

            exec {

                onPreCreate()

                onDoCreate()

                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_SCREEN_ON)
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                doRegisterReceiver(screenReceiver, intentFilter)

                if (toastOnApiCommunicationFailure) {

                    val apiIntentFilter = apiCommunicationFailureListener.getIntentFilter()
                    doRegisterReceiver(apiCommunicationFailureListener, apiIntentFilter)
                }

                beforeManagers()
                initializeManagers()
                onManagers()

                Console.log("Installing profile: START")
                ProfileInstaller.writeProfile(applicationContext)
                Console.log("Installing profile: END")

                onPostCreate()
            }

        } catch (e: RejectedExecutionException) {

            recordException(e)

            throw e
        }
    }

    protected open fun initFirebaseWithAnalytics() {

        if (firebaseEnabled) {

            FirebaseApp.initializeApp(applicationContext)

            if (firebaseAnalyticsEnabled) {

                firebaseAnalytics = Firebase.analytics
            }
        }
    }

    protected open fun initFacebook() {

        if (facebookEnabled) {

            try {

                FacebookSdk.sdkInitialize(applicationContext)
                val facebookLogger = AppEventsLogger.newLogger(this);
                facebookLogger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP);

            } catch (e: Exception) {

                recordException(e)
            }
        }
    }

    protected open fun onPostCreate() = Unit

    protected open fun onScreenOn() {

        Console.log("Screen is ON")
    }

    protected open fun onScreenOff() {

        Console.log("Screen is OFF")

        val intent = Intent(BROADCAST_ACTION_APPLICATION_SCREEN_OFF)
        sendBroadcast(intent)
    }

    protected open fun onFcmToken(token: String) {

        Console.log("FCM: Token => $token")
    }

    protected open fun onFcmEvent(intent: Intent) {

        Console.log("FCM: Event => $intent")
    }

    protected open fun onManagersReady() {

        Console.info("Managers: Ready")
    }

    protected open fun getIndependentManagers(): MutableList<DataManagement<*>> {

        val managers = mutableListOf<DataManagement<*>>()

        managers.add(SettingsManager.obtain())

        return managers
    }

    protected open fun getToLoad(): MutableList<Loadable> {

        val toLoad = mutableListOf<Loadable>()

        // TODO: Add install referrers
        // toLoad.add(SettingsManager.obtain())

        return toLoad
    }

    protected open fun getManagersToLoad(): MutableList<Loadable> {

        val managers = mutableListOf<Loadable>()

        managers.add(SettingsManager.obtain())

        return managers
    }

    protected open fun onLoaded() {

        Console.log("Loadable are loaded")
    }

    protected open fun onManagersLoaded() {

        Console.log("Managers are loaded")
    }

    private fun initializeManagers(): Boolean {

        var success = true

        managers.forEach {

            val disabled = getDisabledManagers()

            if (disabled.contains(it)) {

                Console.debug("${it::class.simpleName} is disabled")

            } else {

                val result = ManagersInitializer().initializeManagers(

                    managers = it,
                    context = this,
                    defaultResources = defaultManagerResources
                )

                if (!result) {

                    success = false
                }
            }
        }

        managersReady.set(true)

        return success
    }

    private fun load() {

        loadManagers()

        getToLoad().forEach {

            it.load()
        }

        onDidLoaded()
    }

    private fun loadManagers() {

        getManagersToLoad().forEach {

            it.load()
        }

        onManagersDidLoaded()
    }

    private fun onDidLoaded() {

        onLoaded()
    }

    private fun onManagersDidLoaded() {

        // TODO: Initialize installation referrers or other Loadable(s)

        onManagersLoaded()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initializeFcm() {

        if (!firebaseEnabled) {

            return
        }

        Console.info("FCM: Initializing")

        val tokenFilter = IntentFilter(FcmService.BROADCAST_ACTION_TOKEN)
        val eventFilter = IntentFilter(FcmService.BROADCAST_ACTION_EVENT)

        doRegisterReceiver(fcmTokenReceiver, tokenFilter)
        doRegisterReceiver(fcmEventReceiver, eventFilter)

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener(

                OnCompleteListener { task ->

                    if (!task.isSuccessful) {

                        Console.warning("FCM: Fetching registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    val token = task.result

                    if (isNotEmpty(token)) {

                        Console.info("FCM: Initialized, token => $token")

                        onFcmToken(token)

                    } else {

                        Console.info("FCM: Initialized with no token")
                    }
                }
            )
    }

    override fun getActivityCount(): Int {

        return TOP_ACTIVITY.size
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

        Executor.UI.execute {

            try {

                TOP_ACTIVITY.add(clazz)
                TOP_ACTIVITIES.add(clazz)

                Console.log("$ACTIVITY_LIFECYCLE_TAG PRE-RESUMED :: ${clazz.simpleName}")

                Console.debug("$ACTIVITY_LIFECYCLE_TAG Top activity: ${clazz.simpleName}")

            } catch (e: Exception) {

                recordException(e)
            }
        }

        super.onActivityPreResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG PAUSED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG RESUMED :: ${activity.javaClass.simpleName}")

        if (isAppInBackground.get()) {

            val intent = Intent(BROADCAST_ACTION_APPLICATION_STATE_FOREGROUND)
            sendBroadcast(intent)

            Console.debug("$ACTIVITY_LIFECYCLE_TAG Foreground")
        }

        isAppInBackground.set(false)
    }

    override fun onActivityPostResumed(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG POST-RESUMED :: ${activity.javaClass.simpleName}")

        super.onActivityPostResumed(activity)
    }

    override fun onActivityPrePaused(activity: Activity) {

        try {

            val clazz = activity::class.java

            if (TOP_ACTIVITIES.contains(clazz)) {

                TOP_ACTIVITIES.remove(clazz)
            }

            Console.log("$ACTIVITY_LIFECYCLE_TAG PRE-PAUSED :: ${activity.javaClass.simpleName}")

            Console.debug("$ACTIVITY_LIFECYCLE_TAG Top activity: ${clazz.simpleName}")

        } catch (e: Exception) {

            recordException(e)
        }

        super.onActivityPrePaused(activity)
    }

    override fun onActivityPostPaused(activity: Activity) {

        Console.log(

            "$ACTIVITY_LIFECYCLE_TAG POST-PAUSED :: ${activity.javaClass.simpleName}, " +
                    "Active: ${TOP_ACTIVITY.size}"
        )

        if (TOP_ACTIVITIES.size <= 1) {

            onAppBackgroundState()
        }

        super.onActivityPostPaused(activity)
    }

    override fun onActivityPreStopped(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG PRE-STOPPED :: ${activity.javaClass.simpleName}")

        super.onActivityPreStopped(activity)
    }

    override fun onActivityStopped(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG STOPPED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPostStopped(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG POST-STOPPED :: ${activity.javaClass.simpleName}")

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

        Console.log("$ACTIVITY_LIFECYCLE_TAG PRE-DESTROYED :: ${activity.javaClass.simpleName}")

        if (TOP_ACTIVITIES.isEmpty()) {

            Console.debug("$ACTIVITY_LIFECYCLE_TAG No top activity")

            onAppBackgroundState()

        } else {

            if (TOP_ACTIVITY.isNotEmpty()) {

                val clazz = TOP_ACTIVITY.last()

                Console.debug("$ACTIVITY_LIFECYCLE_TAG Top activity: ${clazz.simpleName}")
            }
        }

        super.onActivityPreDestroyed(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {

        Console.log("$ACTIVITY_LIFECYCLE_TAG DESTROYED :: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPostDestroyed(activity: Activity) {

        // Ignore

        super.onActivityPostDestroyed(activity)
    }

    private fun beforeManagers() {

        update()
    }

    private fun onManagers() {

        initializeFcm()
        load()
        onManagersReady()
        update()
    }

    private fun enableStrictMode() {

        Console.log("Enable Strict Mode, disabled=$STRICT_MODE_DISABLED")

        if (STRICT_MODE_DISABLED.get()) {

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

            Console.error(e)
        }
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        return doRegisterReceiver(receiver, filter)
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {

        receiver?.let {

            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(it)
        }
    }

    fun sendBroadcastIPC(

        action: String,
        receiver: String,
        receiverClass: KClass<*>,
        function: String,
        content: String? = null,
        tag: String = "IPC :: Send ::"

    ): Boolean {

        val cName = receiverClass.qualifiedName ?: ""

        return sendBroadcastIPC(action, receiver, cName, function, content, tag)
    }

    fun sendBroadcastIPC(

        action: String,
        receiver: String,
        cName: String,
        function: String,
        content: String?,
        tag: String

    ): Boolean {

        Console.log("$tag Sending intent :: START")

        val intent = Intent()
        val data = InterprocessData(function, content)
        // FIXME: Shall use GsonParser with custom serialization support
        val json = Gson().toJson(data)

        intent.setAction(action)

        Console.log("$tag Sending intent :: Action = ${intent.action}")
        Console.log("$tag Sending intent :: Data = $data")
        Console.log("$tag Sending intent :: JSON = $json")

        intent.putExtra(InterprocessData.BUNDLE_KEY, json)

        if (isEmpty(cName)) {

            Console.error("$tag Sending intent :: Failed :: Class name is empty")

            return false
        }

        if (isEmpty(receiver)) {

            Console.error("$tag Sending intent :: Failed :: Package name is empty")

            return false
        }

        Console.log("$tag Sending intent :: Class = $cName")
        Console.log("$tag Sending intent :: Target receiver = $receiver")

        intent.setClassName(receiver, cName)

        if (takeContext().sendBroadcastWithResult(intent, local = false)) {

            Console.log("$tag Sending intent :: END")

            return true

        } else {

            Console.error("$tag Sending intent :: Failed")
        }

        return false
    }

    fun sendBroadcastWithResult(intent: Intent?, local: Boolean = true): Boolean {

        intent?.let {

            if (local) {

                return LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(it)

            } else {

                super.sendBroadcast(it)

                return true
            }
        }

        return false
    }

    override fun sendBroadcast(intent: Intent?) {

        intent?.let {

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(it)
        }
    }

    protected open fun getUpdatesCodes() = setOf<Long>()

    override fun isUpdating(): Boolean {

        val updating = updating.get()

        Console.log("$updatingTag GET :: Updating = $updating")

        return updating
    }

    override fun update() {

        /*
            TODO: Integrate DataMigration recipes with the updates
         */

        var versionCode = 0

        val tag = "Update ::"

        try {

            versionCode = getVersionCode().toInt()

        } catch (e: NumberFormatException) {

            onUpdatedFailed(0)

            Console.error(e)
        }

        getUpdatesCodes().forEach { code ->

            /*
                TODO: Incorporate until which version code is the update applicable (if needed)
            */
            if (versionCode >= code && isUpdateAvailable(code)) {

                if (!isUpdating()) {

                    setUpdating(true)
                }

                Console.log("$tag Code :: $versionCode :: START")

                try {

                    val success = update(code)

                    if (success) {

                        onUpdated(code)

                        Console.log("$tag Code :: $versionCode :: END")

                    } else {

                        onUpdatedFailed(code)
                    }

                } catch (e: MigrationNotReadyException) {

                    Console.warning("${e.message}, Code = $versionCode")
                }
            }
        }

        setUpdating(false)
    }

    override fun update(identifier: Long) = false

    override fun onUpdatedFailed(identifier: Long) {

        val msg = "Failed to update, versionCode = ${getVersionCode()}, " +
                "identifier = $identifier"

        val error = IllegalStateException(msg)
        recordException(error)
    }

    override fun onUpdated(identifier: Long) {

        val tag = "Update ::"
        val key = "$prefsKeyUpdate.$identifier"
        val result = prefs.put(key, "$identifier")

        val msg = "$tag Success: versionCode = ${getVersionCode()}, " +
                "identifier = $identifier"

        Console.debug(msg)

        if (!result) {

            Console.error("$tag Failed to update preferences :: key = '$key'")
        }
    }

    override fun isUpdateApplied(identifier: Long) = !isUpdateAvailable(identifier)

    protected open fun setupObfuscator(): Boolean {

        val tag = "Obfuscator ::"

        Console.log("$tag Setting up :: START")

        val endpoint = getObfuscatorEndpoint()
        val ghToken = getObfuscatorEndpointToken()
        val saltObtain = RemoteObfuscatorSaltProvider(endpoint, ghToken)
        val obfuscation = Obfuscator(saltObtain)

        DefaultObfuscator.setStrategy(obfuscation)

        Console.log("$tag Setting up :: END")

        return true
    }

    protected open fun getObfuscatorEndpoint() = ""

    protected open fun getObfuscatorEndpointToken() = ""

    protected fun isUpdateAvailable(identifier: Long): Boolean {

        val key = "$prefsKeyUpdate.$identifier"
        val value = prefs.get(key)
        val updateAvailable = isEmpty(value)

        if (updateAvailable) {

            Console.log("Update :: Available :: identifier = '$identifier'")

        } else {

            Console.log("Update :: Already applied :: identifier = '$identifier'")
        }

        return updateAvailable
    }

    private fun onAppBackgroundState() {

        isAppInBackground.set(true)

        val intent = Intent(BROADCAST_ACTION_APPLICATION_STATE_BACKGROUND)
        sendBroadcast(intent)

        Console.debug("$ACTIVITY_LIFECYCLE_TAG Background")
    }

    private fun onPreCreate() {

        val success = setupObfuscator()
        DefaultObfuscator.setReady(success)

        if (!success) {

            val e = IllegalStateException("Failed to setup obfuscator")
            recordException(e)
        }
    }

    private fun setUpdating(value: Boolean) {

        Console.log("$updatingTag SET :: Updating = $value")

        updating.set(value)
    }

    private fun doRegisterReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        receiver?.let { r ->
            filter?.let { f ->

                LocalBroadcastManager.getInstance(applicationContext).registerReceiver(r, f)
            }
        }

        return null
    }
}