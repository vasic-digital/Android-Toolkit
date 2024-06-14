package com.redelf.access

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.capability.CapabilityCheckCallback
import com.redelf.commons.logging.Console

abstract class BiometricAccessMethod(priority: Int, ctx: AppCompatActivity) :
    AccessMethod(priority, ctx) {

    private val biometricManager = BiometricManager.from(ctx)
    private val mainExecutor = ContextCompat.getMainExecutor(ctx)
    private val weak = BiometricManager.Authenticators.BIOMETRIC_WEAK
    private val prompt = BiometricPrompt(ctx, mainExecutor, authCallback)

    protected open val authenticators: List<Int> = listOf()
    protected val packageManager: PackageManager = ctx.packageManager

    override fun checkCapability(callback: CapabilityCheckCallback) {
        super.checkCapability(callback)

        val capable = isCapable()
        val available = isAvailable()
        val success = capable && available
        if (!success) {

            Console.warning("Not capable (capable=$capable, available=$available): $this")
        }
        capabilityCheckCallback.onCapabilityChecked(success)
    }

    override fun checkInstalled(callback: InstallationCheckCallback) {
        super.checkInstalled(callback)
        executor.execute {

            val available = isAvailable()
            val success = BiometricManager.BIOMETRIC_SUCCESS
            val result = biometricManager.canAuthenticate(weak)
            val installed = result == success && available
            if (!installed) {

                val msg =
                    "Not installed (success=${result == success}, available=$available): $this"
                Console.warning(msg)
            }
            installationCallback.onInstallationChecked(installed)
        }
    }

    override fun execute() {

        execute(authenticators, "$this")
    }


    private fun execute(authenticators: List<Int>, from: String) {

        try {

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(ctx.getString(R.string.biometric_login_title))
                .setSubtitle(ctx.getString(R.string.biometric_login_subtitle))
                .setNegativeButtonText(ctx.getString(android.R.string.cancel))
                .setConfirmationRequired(false)

            val noAuth = -1
            var auth = noAuth
            Console.log("Authenticators: $authenticators")
            authenticators.forEach {

                auth = if (auth == noAuth) {

                    it
                } else {
                    auth or it
                }
            }
            if (auth > noAuth) {
                promptInfoBuilder.setAllowedAuthenticators(auth)
            }

            val info = promptInfoBuilder.build()
            ctx.runOnUiThread {

                Console.log("Authenticate: $prompt, from: $from")
                prompt.authenticate(info)
            }
        } catch (e: IllegalArgumentException) {

            Console.error(e)
            executionCallback.onExecution(false, "IllegalArgumentException")
        }
    }

    override fun cancel() {

        prompt.cancelAuthentication()
    }


    protected fun hasFingerprint(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val fingerprint = PackageManager.FEATURE_FINGERPRINT
            packageManager.hasSystemFeature(fingerprint)
        } else {

            return false
        }
    }

    protected abstract fun isAvailable(): Boolean

    private fun isCapable(): Boolean {

        val result = biometricManager.canAuthenticate(weak)
        val errNoHardware = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        val errHwUnavailable = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        return result != errHwUnavailable && result != errNoHardware
    }
}