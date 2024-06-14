package com.redelf.access

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import com.redelf.access.installation.Installation
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.capability.CapabilityCheck
import com.redelf.commons.capability.CapabilityCheckCallback
import com.redelf.commons.execution.Cancellation
import com.redelf.commons.execution.CommonExecution
import com.redelf.commons.execution.CommonExecutionCallback
import com.redelf.commons.execution.Executor
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

abstract class AccessMethod(private val priority: Int, protected val ctx: AppCompatActivity) :

        Comparable<AccessMethod>,
    CapabilityCheck,
    Installation,
    Cancellation,
    CommonExecution {

    private val executing = AtomicBoolean()

    private val executionCallbacks =
        Callbacks<CommonExecutionCallback>(identifier = "Execution")

    private val installationCallbacks =
        Callbacks<InstallationCheckCallback>(identifier = "Installation")

    private val capabilityCheckCallbacks =
        Callbacks<CapabilityCheckCallback>(identifier = "Capability check")

    protected val executor = Executor.MAIN

    protected val authCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)

            Console.error("Authentication error, %d, %s", errorCode, errString)
            executionCallback.onExecution(false, "onAuthenticationError")
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()

            executionCallback.onExecution(false, "onAuthenticationFailed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            executionCallback.onExecution(true, "onAuthenticationSucceeded")
        }
    }

    protected val executionCallback = object : CommonExecutionCallback {

        override fun onExecution(success: Boolean, calledFrom: String) {

            executing.set(false)
            executionCallbacks.doOnAll(object : CallbackOperation<CommonExecutionCallback> {

                override fun perform(callback: CommonExecutionCallback) {

                    callback.onExecution(success, "executionCallback :: $calledFrom")
                    executionCallbacks.unregister(callback)
                }
            }, operationName = "Execution operation")
        }
    }

    protected val installationCallback = object : InstallationCheckCallback {

        override fun onInstallationChecked(installed: Boolean) {

            installationCallbacks.doOnAll(object : CallbackOperation<InstallationCheckCallback> {

                override fun perform(callback: InstallationCheckCallback) {

                    callback.onInstallationChecked(installed)
                    installationCallbacks.unregister(callback)
                }
            }, operationName = "Installation operation")
        }
    }

    protected val capabilityCheckCallback = object : CapabilityCheckCallback {

        override fun onCapabilityChecked(capable: Boolean) {

            capabilityCheckCallbacks.doOnAll(object : CallbackOperation<CapabilityCheckCallback> {

                override fun perform(callback: CapabilityCheckCallback) {

                    callback.onCapabilityChecked(capable)
                    capabilityCheckCallbacks.unregister(callback)
                }
            }, operationName = "Capability check operation")
        }
    }

    override fun checkCapability(callback: CapabilityCheckCallback) {

        capabilityCheckCallbacks.register(callback)
    }

    override fun checkInstalled(callback: InstallationCheckCallback) {

        installationCallbacks.register(callback)
    }

    final override fun execute(callback: CommonExecutionCallback) {

        executionCallbacks.register(callback)
        if (executing.get()) {

            Console.warning("Already executing: $this")
            return
        }

        executing.set(true)

        val checkCallback = object : InstallationCheckCallback {

            override fun onInstallationChecked(installed: Boolean) {

                if (installed) {

                    execute()

                } else {

                    val msg = "onInstallationChecked, not installed"
                    executionCallback.onExecution(false, msg)
                }
            }
        }

        checkInstalled(checkCallback)
    }

    fun isExecuting() = executing.get()

    abstract fun execute()

    override fun compareTo(other: AccessMethod) = priority.compareTo(other.priority)

    override fun toString(): String {

        return "AccessMethod(name=${this::class.simpleName}, priority=$priority)"
    }
}