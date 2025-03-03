package com.redelf.access.implementation

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.redelf.access.BiometricAccessMethod

class FingerprintAccess(priority: Int, ctx: AppCompatActivity) : BiometricAccessMethod(priority, ctx) {

    override val authenticators = listOf(BiometricManager.Authenticators.BIOMETRIC_WEAK)

    @Suppress("DEPRECATION")
    override fun install() = executor.execute {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val intent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            ctx.startActivity(intent)

        } else {

            val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
            ctx.startActivity(intent)
        }
    }

    override fun isAvailable() = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
}