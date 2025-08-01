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