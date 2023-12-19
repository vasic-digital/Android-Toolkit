package com.redelf.commons.application

import android.app.Application
import com.google.gson.GsonBuilder
import com.redelf.commons.BuildConfig
import com.redelf.commons.R
import com.redelf.commons.execution.Executor
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.management.DataManagement
import com.redelf.commons.management.Management
import com.redelf.commons.persistance.Data
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.persistance.Salter

import timber.log.Timber

abstract class BaseApplication : Application() {

    protected abstract fun getSalt(): String
    protected abstract fun populateManagers(): List<Management>

    protected abstract fun onDoCreate()

    private val managers = mutableListOf<Management>()

    override fun onCreate() {
        super.onCreate()

        managers.addAll(populateManagers())

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

                var salt = getSalt()

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

        val action = {

            onDoCreate()

            Timber.i("FCM: Initializing")

            // TODO: FCM
//            FirebaseMessaging.getInstance()
//                .token
//                .addOnCompleteListener(
//
//                    OnCompleteListener { task ->
//
//                        if (!task.isSuccessful) {
//
//                            Timber.w("FCM: Fetching registration token failed", task.exception)
//                            return@OnCompleteListener
//                        }
//
//                        val token = task.result
//
//                        Timber.i("FCM: Initialized, token => $token")
//                    }
//                )

            // TODO: Screen receiver
//            val intentFilter = IntentFilter()
//            intentFilter.addAction(Intent.ACTION_SCREEN_ON)
//            intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
//            registerReceiver(screenReceiver, intentFilter)

            initializeManagers()

            Timber.i("Application: Initialized")
        }

        Executor.MAIN.execute(action)
    }

    private fun initializeManagers() {

        managers.forEach { manager ->

            if (manager is DataManagement) {

                manager.initialize(object : LifecycleCallback<EncryptedPersistence> {

                    override fun onInitialization(

                        success: Boolean,
                        vararg args: EncryptedPersistence

                    ) {

                        if (success) {

                            Timber.v(

                                "Manager: ${manager.javaClass.simpleName} " +
                                        "initialization completed with success"
                            )

                        } else throw IllegalStateException(

                            "Manager: ${manager.javaClass.simpleName} " +
                                    "initialization completed with failure"
                        )
                    }

                    override fun onShutdown(

                        success: Boolean,
                        vararg args: EncryptedPersistence

                    ) {

                        // Ignore - not used
                    }
                })
            }
        }
    }
}