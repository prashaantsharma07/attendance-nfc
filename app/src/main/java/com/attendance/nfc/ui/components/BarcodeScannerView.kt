package com.attendance.nfc.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.AccentBlueBright
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    modifier: Modifier = Modifier,
    debounceMs: Long = 1800L,
    onBarcodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull()
    }

    var lastScannedValue by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            toneGenerator?.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        @OptIn(ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val raw = barcode.rawValue?.trim()
                                        if (!raw.isNullOrBlank()) {
                                            val now = System.currentTimeMillis()
                                            if (raw != lastScannedValue || (now - lastScannedTime) > debounceMs) {
                                                lastScannedValue = raw
                                                lastScannedTime = now

                                                // Haptic + Sound feedback
                                                triggerHapticAndBeep(ctx, toneGenerator)
                                                onBarcodeScanned(raw)
                                                break
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Viewfinder reticle overlay
        ViewfinderOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Flashlight toggle button
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            IconButton(
                onClick = {
                    val nextState = !isTorchOn
                    camera?.cameraControl?.enableTorch(nextState)
                    isTorchOn = nextState
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) AccentBlueBright else Color.White
                )
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanner-laser")
    val laserYFraction by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser-pos"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Target box in the center (approx 76% width, 65% height)
        val boxWidth = width * 0.76f
        val boxHeight = height * 0.65f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerLength = 32.dp.toPx()
        val cornerRadius = 14.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()

        // Semi-transparent dark vignette outside target frame
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path, color = Color.Black.copy(alpha = 0.35f))

        // Cutout target area border
        drawRoundRect(
            color = AccentBlue.copy(alpha = 0.3f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw 4 stylish corner brackets
        val cornerColor = AccentBlueBright

        // Top-Left
        drawLine(
            color = cornerColor,
            start = Offset(left, top + cornerLength),
            end = Offset(left, top + cornerRadius),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth
        )

        // Top-Right
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerLength, top),
            end = Offset(right - cornerRadius, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, top + cornerRadius),
            end = Offset(right, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-Left
        drawLine(
            color = cornerColor,
            start = Offset(left, bottom - cornerLength),
            end = Offset(left, bottom - cornerRadius),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, bottom),
            end = Offset(left + cornerLength, bottom),
            strokeWidth = strokeWidth
        )

        // Bottom-Right
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerLength, bottom),
            end = Offset(right - cornerRadius, bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, bottom - cornerRadius),
            end = Offset(right, bottom - cornerLength),
            strokeWidth = strokeWidth
        )

        // Animated laser scanning line
        val laserY = top + (boxHeight * laserYFraction)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    AccentBlueBright.copy(alpha = 0.9f),
                    Color.Transparent
                ),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

private fun triggerHapticAndBeep(context: Context, toneGenerator: ToneGenerator?) {
    try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    } catch (_: Exception) {}

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    } catch (_: Exception) {}
}
