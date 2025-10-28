package com.shareconnect.qrscanner

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun QRScannerScreen(
    onResult: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalyzer by remember { mutableStateOf<ImageAnalysis?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isScanning = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isScanning = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Clean up camera executor
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = getCameraProvider(context)
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Create preview
            preview = Preview.Builder().build()

            // Create image analysis
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isScanning) {
                            processImageProxy(
                                barcodeScanner = (context as QRScannerActivity).barcodeScanner,
                                imageProxy = imageProxy,
                                onResult = { result ->
                                    if (result != null) {
                                        isScanning = false
                                        onResult(result)
                                    }
                                }
                            )
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        } catch (exc: Exception) {
            Log.e("QRScanner", "Use case binding failed", exc)
            onResult(null)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    preview?.setSurfaceProvider(surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scanning overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // Scanning frame
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .border(2.dp, Color.White, MaterialTheme.shapes.medium)
            ) {
                // Corner indicators
                val cornerSize = 20.dp
                val cornerThickness = 4.dp

                // Top-left corner
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.TopStart)
                        .border(
                            width = cornerThickness,
                            color = Color.Green,
                            shape = MaterialTheme.shapes.small
                        )
                )

                // Top-right corner
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.TopEnd)
                        .border(
                            width = cornerThickness,
                            color = Color.Green,
                            shape = MaterialTheme.shapes.small
                        )
                )

                // Bottom-left corner
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.BottomStart)
                        .border(
                            width = cornerThickness,
                            color = Color.Green,
                            shape = MaterialTheme.shapes.small
                        )
                )

                // Bottom-right corner
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.BottomEnd)
                        .border(
                            width = cornerThickness,
                            color = Color.Green,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }

            // Instructions
            Text(
                text = "Position QR code within the frame",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )

            // Cancel button
            OutlinedButton(
                onClick = { onResult(null) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }

private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onResult: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { value ->
                    Log.d("QRScanner", "QR Code detected: $value")
                    onResult(value)
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}