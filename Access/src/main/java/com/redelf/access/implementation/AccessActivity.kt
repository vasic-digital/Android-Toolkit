package com.redelf.access.implementation

import android.os.Bundle
import com.redelf.access.Access
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.logging.Console

abstract class AccessActivity : BaseActivity() {

    protected var authenticated = false

    private var accessFailed = false
    private lateinit var access: Access

    private val accessInitCallback: LifecycleCallback<Unit> = object : LifecycleCallback<Unit> {

        override fun onInitialization(success: Boolean, vararg args: Unit) {

            if (success) {

                Console.info("Access has been initialized")

                try {

                    access.checkInstalled(accessCheckCallback)

                } catch (e: IllegalStateException) {

                    accessFailed = true
                    Console.error(e)
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

                    Console.log("onInstallationChecked: %s", hashCode())
                    executeAccess()

                } else {

                    try {

                        onAccessInstall()

                    } catch (e: IllegalStateException) {

                        Console.error(e)
                        onAccessInstallFailed()
                    }
                }
            }
        }

    private val accessExecCallback: CommonExecutionCallback = object : CommonExecutionCallback {

        override fun onExecution(success: Boolean, calledFrom: String) {

            Console.log(

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

            Console.error(e)
            onAccessInitFailed()
        }
    }

    protected open fun isAuthenticated() = authenticated

    protected open fun onAccessFailed() {

        Console.warning("We have faulty access")
    }

    protected open fun onAccessInitFailed() {

        Console.error("Access has not been initialized")
    }

    protected open fun onAccessResult(success: Boolean) {

        if (success) {
            Console.info("Access has authenticated user with success")
        } else {
            Console.error("Access has not authenticated user with success")
        }
    }

    protected open fun onAccessInstall() {

        access.install()
    }

    
    protected open fun executeAccess() {

        if (access.isExecuting()) {

            Console.warning("Already executing access: $access")
            return
        }

        Console.info("Executing access: $access")

        if (authenticated) {

            Console.log("onAccessResult from already authenticated hash=%s", hashCode())
            Console.debug("Already authenticated: $access")
            onAccessResult(authenticated)

        } else {

            Console.debug("Access is ready: ${access.hashCode()}")

            try {

                access.execute(accessExecCallback)

            } catch (e: java.lang.IllegalStateException) {

                Console.error(e)
            }
        }
    }

    protected open fun onAccessInstallFailed() {

        Console.warning("Access installation failed")
    }
}