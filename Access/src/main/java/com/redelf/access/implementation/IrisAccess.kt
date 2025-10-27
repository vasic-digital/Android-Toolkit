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

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.redelf.access.BiometricAccessMethod
import com.redelf.commons.extensions.exec

class IrisAccess(priority: Int, ctx: AppCompatActivity) : BiometricAccessMethod(priority, ctx) {

    override val authenticators = listOf(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    override fun install() {

        exec {

            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            ctx.startActivity(intent)
        }
    }

    override fun isAvailable(): Boolean {

        /*
            PackageManager.FEATURE_FACE or IRIS is always false on devices that actually
                support biometry! Let's wait for Google to fix the API.
        */
        return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }
}