package com.redelf.commons.application

import android.app.Application
import com.google.gson.GsonBuilder
import com.redelf.commons.BuildConfig
import com.redelf.commons.R
import com.redelf.commons.execution.Executor
import com.redelf.commons.management.Management
import com.redelf.commons.persistance.Data
import com.redelf.commons.persistance.GsonParser
import com.redelf.commons.persistance.Salter
import com.redelf.commons.recordException

import timber.log.Timber

abstract class BaseApplication : Application() {

    protected abstract fun onDoCreate()
    protected abstract fun takeSalt(): String

    protected open fun populateManagers() = listOf<Management>()

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

            val managersInitializerCallback = object : ManagersInitializer.InitializationCallback {

                override fun onInitialization(success: Boolean, error: Throwable?) {

                    if (success) {

                        Timber.i("Application: Initialized")

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

        Executor.MAIN.execute(action)
    }

    private fun initializeManagers(callback: ManagersInitializer.InitializationCallback) {

        ManagersInitializer().initializeManagers(managers, callback)
    }
}