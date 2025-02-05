package com.redelf.access.implementation

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.redelf.access.BiometricAccessMethod

class FaceRecognitionAccess(priority: Int, ctx: AppCompatActivity) : BiometricAccessMethod(priority, ctx) {

    override val authenticators = listOf(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    override fun install() = executor.execute {

        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        ctx.startActivity(intent)
    }

    override fun isAvailable(): Boolean {

        /*
            PackageManager.FEATURE_FACE or IRIS is always false on devcies that actually
                support biometry! Let's wait for Google to fix the API.
        */
        return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }
}