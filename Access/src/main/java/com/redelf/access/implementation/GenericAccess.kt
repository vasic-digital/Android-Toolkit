/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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
import com.redelf.commons.extensions.exec

class GenericAccess(priority: Int, ctx: PinAccessActivity) : BiometricAccessMethod(priority, ctx) {

    private val pinAccess = PinAccess(priority, ctx)

    override fun install() {

        exec {

            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            ctx.startActivity(intent)
        }
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