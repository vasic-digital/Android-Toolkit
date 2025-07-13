package com.redelf.access.implementation

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.redelf.access.BiometricAccessMethod
import com.redelf.commons.extensions.exec

class FingerprintAccess(priority: Int, ctx: AppCompatActivity) : BiometricAccessMethod(priority, ctx) {

    override val authenticators = listOf(BiometricManager.Authenticators.BIOMETRIC_WEAK)

    @Suppress("DEPRECATION")
    override fun install() {

        exec {

            val intent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            ctx.startActivity(intent)
        }
    }

    override fun isAvailable() = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
}