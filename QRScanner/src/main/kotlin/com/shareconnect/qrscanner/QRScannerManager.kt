package com.shareconnect.qrscanner

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class QRScannerManager(private val context: Context) {

    /**
     * Launches QR scanner and returns the result as a String, or null if cancelled/failed
     */
    suspend fun scanQRCode(): String? = suspendCancellableCoroutine { continuation ->
        val intent = Intent(context, QRScannerActivity::class.java)

        // For activities
        if (context is ComponentActivity) {
            val launcher = context.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val qrResult = result.data?.getStringExtra(QRScannerActivity.EXTRA_QR_RESULT)
                continuation.resume(qrResult)
            }
            launcher.launch(intent)
        }
        // For fragments - this would need to be handled differently
        else {
            // For now, return null for fragments (can be extended later)
            continuation.resume(null)
        }
    }

    companion object {
        /**
         * Creates a QR scanner launcher for use with Activity Result API
         */
        fun createLauncher(
            activity: ComponentActivity,
            onResult: (String?) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val qrResult = result.data?.getStringExtra(QRScannerActivity.EXTRA_QR_RESULT)
                onResult(qrResult)
            }
        }

        /**
         * Launches QR scanner using the provided launcher
         */
        fun launchScanner(context: Context, launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(context, QRScannerActivity::class.java)
            launcher.launch(intent)
        }
    }
}