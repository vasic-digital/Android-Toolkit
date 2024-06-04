package com.redelf.access.implementation

import android.os.Bundle
import com.redelf.access.Access
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Timber

abstract class AccessActivity : BaseActivity() {

    protected var authenticated = false

    private var accessFailed = false
    private lateinit var access: Access

    private val accessInitCallback: LifecycleCallback<Unit> = object : LifecycleCallback<Unit> {

        override fun onInitialization(success: Boolean, vararg args: Unit) {

            if (success) {

                Timber.i("Access has been initialized")

                try {

                    access.checkInstalled(accessCheckCallback)

                } catch (e: IllegalStateException) {

                    accessFailed = true
                    Timber.e(e)
                    onAccessInitFailed()
                }
            } else {

                accessFailed = true
                onAccessInitFailed()
            }
        }

        override fun onShutdown(success: Boolean, vararg args: Unit) {

            // Ignore.
        }
    }

    private val accessCheckCallback: InstallationCheckCallback =

        object : InstallationCheckCallback {

            override fun onInstallationChecked(installed: Boolean) {

                if (installed) {

                    Timber.v("onInstallationChecked: %s", hashCode())
                    executeAccess()

                } else {

                    try {

                        onAccessInstall()

                    } catch (e: IllegalStateException) {

                        Timber.e(e)
                        onAccessInstallFailed()
                    }
                }
            }
        }

    private val accessExecCallback: CommonExecutionCallback = object : CommonExecutionCallback {

        override fun onExecution(success: Boolean, calledFrom: String) {

            Timber.v(

                "onAccessResult from onExecution: %s, hash=%s, called from: %s",
                success, hashCode(), calledFrom
            )

            authenticated = success
            onAccessResult(success)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        access = getAccess()
    }

    abstract fun getAccess(): Access

    @Synchronized
    protected open fun checkAccess() {

        try {

            if (access.isInitialized()) {

                access.checkInstalled(accessCheckCallback)

            } else {

                if (accessFailed) {

                    onAccessFailed()

                } else {

                    access.initialize(accessInitCallback)
                }
            }
        } catch (e: IllegalStateException) {

            Timber.e(e)
            onAccessInitFailed()
        }
    }

    protected open fun isAuthenticated() = authenticated

    protected open fun onAccessFailed() {

        Timber.w("We have faulty access")
    }

    protected open fun onAccessInitFailed() {

        Timber.e("Access has not been initialized")
    }

    protected open fun onAccessResult(success: Boolean) {

        if (success) {
            Timber.i("Access has authenticated user with success")
        } else {
            Timber.e("Access has not authenticated user with success")
        }
    }

    protected open fun onAccessInstall() {

        access.install()
    }

    @Synchronized
    protected open fun executeAccess() {

        if (access.isExecuting()) {

            Timber.w("Already executing access: $access")
            return
        }

        Timber.i("Executing access: $access")

        if (authenticated) {

            Timber.v("onAccessResult from already authenticated hash=%s", hashCode())
            Timber.d("Already authenticated: $access")
            onAccessResult(authenticated)

        } else {

            Timber.d("Access is ready: ${access.hashCode()}")

            try {

                access.execute(accessExecCallback)

            } catch (e: java.lang.IllegalStateException) {

                Timber.e(e)
            }
        }
    }

    protected open fun onAccessInstallFailed() {

        Timber.w("Access installation failed")
    }
}