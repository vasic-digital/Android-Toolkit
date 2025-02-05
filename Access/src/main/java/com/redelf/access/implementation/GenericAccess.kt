package com.redelf.access.implementation

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.redelf.access.BiometricAccessMethod
import com.redelf.access.implementation.pin.PinAccess
import com.redelf.access.implementation.pin.PinAccessActivity
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.capability.CapabilityCheckCallback

class GenericAccess(priority: Int, ctx: PinAccessActivity) : BiometricAccessMethod(priority, ctx) {

    private val pinAccess = PinAccess(priority, ctx)

    override fun install() = executor.execute {

        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        ctx.startActivity(intent)
    }

    override fun execute() {

        if (hasFingerprint()) {

            super.execute()

        } else {
            pinAccess.execute(executionCallback)
        }
    }

    override fun cancel() {

        if (hasFingerprint()) {
            super.cancel()
        }
    }

    override fun checkInstalled(callback: InstallationCheckCallback) {

        if (hasFingerprint()) {

            super.checkInstalled(callback)
        } else {
            pinAccess.checkInstalled(callback)
        }
    }

    override fun checkCapability(callback: CapabilityCheckCallback) {

        if (hasFingerprint()) {

            super.checkCapability(callback)

        } else {
            pinAccess.checkCapability(callback)
        }
    }

    override fun isAvailable() = if (hasFingerprint()) {

        true
    } else {

        val manager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        manager.isKeyguardSecure
    }
}