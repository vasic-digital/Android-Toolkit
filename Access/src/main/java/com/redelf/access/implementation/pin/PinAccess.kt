package com.redelf.access.implementation.pin

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import com.redelf.access.AccessMethod
import com.redelf.access.R
import com.redelf.access.implementation.AccessActivity
import com.redelf.access.installation.InstallationCheckCallback
import com.redelf.commons.capability.CapabilityCheckCallback
import com.redelf.commons.logging.Console
import kotlin.random.Random


class PinAccess(priority: Int, ctx: AccessActivity) : AccessMethod(priority, ctx) {

    private val activityRequestCode = Random.nextInt(1001, 2001)

    override fun checkCapability(callback: CapabilityCheckCallback) {

        super.checkCapability(callback)
        capabilityCheckCallback.onCapabilityChecked(true)
    }

    override fun checkInstalled(callback: InstallationCheckCallback) {
        super.checkInstalled(callback)
        executor.execute {

            val manager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            installationCallback.onInstallationChecked(manager.isKeyguardSecure)
        }
    }

    override fun install() = executor.execute {

        val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
        ctx.startActivity(intent)
    }

    
    @Suppress("DEPRECATION")
    override fun execute() {

        val context = getContext()

        if (context.isFinishing) {

            Console.warning("Activity is finishing")
            return
        }

        val title = ctx.getString(R.string.pin_login_title)
        val subtitle = ctx.getString(R.string.pin_login_subtitle)
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val authIntent: Intent = km.createConfirmDeviceCredentialIntent(title, subtitle)

        context.register(executionCallback)
        context.activityRequestCode = activityRequestCode
        context.startActivityForResult(authIntent, activityRequestCode)
    }

    override fun cancel() {

        // TODO: Implement cancellation.
    }

    private fun getContext() = ctx as PinAccessActivity
}