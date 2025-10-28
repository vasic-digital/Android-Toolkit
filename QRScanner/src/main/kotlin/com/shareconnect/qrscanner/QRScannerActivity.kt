package com.shareconnect.qrscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

class QRScannerActivity : ComponentActivity() {

    internal lateinit var barcodeScanner: BarcodeScanner

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start scanning
            setContent {
                QRScannerScreen(onResult = { result ->
                    finishWithResult(result)
                })
            }
        } else {
            // Permission denied
            finishWithResult(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Check camera permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                setContent {
                    QRScannerScreen(onResult = { result ->
                        finishWithResult(result)
                    })
                }
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun finishWithResult(result: String?) {
        val intent = Intent().apply {
            putExtra(EXTRA_QR_RESULT, result)
        }
        setResult(if (result != null) RESULT_OK else RESULT_CANCELED, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeScanner.close()
    }

    companion object {
        const val EXTRA_QR_RESULT = "qr_result"
    }
}