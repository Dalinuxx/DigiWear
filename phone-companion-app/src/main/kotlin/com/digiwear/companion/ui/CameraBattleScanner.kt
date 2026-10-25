package com.digiwear.companion.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.digiwear.core.codec.CommCodeCodec
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Cyber Targeting Reticle HUD & Real-Time MLKit/CameraX Barcode Scanner for D-Com PvP Matchmaking.
 * Captures 128-bit Comm Codes, DigiROMs, and Wild Barcodes directly from smartwatches and companion screens.
 */
@Composable
fun CameraBattleScanner(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreviewAndScanner(
                onCodeScanned = onCodeScanned,
                onDismiss = onDismiss,
                statusMessage = statusMessage
            )
        } else {
            CameraPermissionFallback(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CameraPreviewAndScanner(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    statusMessage: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8
                )
                .build()
            )
    }
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    // Throttle / debounce scanner callbacks (thread-safe for background analyzer thread)
    val lastScannedCode = remember { AtomicReference<String?>(null) }
    val lastScanTimestamp = remember { AtomicLong(0L) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }

    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val isDisposed = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            try {
                cameraProviderRef.get()?.unbindAll()
                cameraExecutor.shutdown()
                barcodeScanner.close()
                textRecognizer.close()
            } catch (e: Exception) {
                Log.w("CameraBattleScanner", "Error releasing scanner resources", e)
            }
        }
    }

    // Reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "ReticleScanAnim")
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanlineY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. CameraX PreviewView via AndroidView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    if (isDisposed.get()) return@addListener
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef.set(cameraProvider)
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(barcodeScanner, textRecognizer, imageProxy) { scannedRaw ->
                                val now = System.currentTimeMillis()
                                val lastCode = lastScannedCode.get()
                                val lastTimestamp = lastScanTimestamp.get()
                                if (scannedRaw != lastCode || (now - lastTimestamp > 1500L)) {
                                    lastScannedCode.set(scannedRaw)
                                    lastScanTimestamp.set(now)
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        currentOnCodeScanned(scannedRaw)
                                    }
                                }
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraBattleScanner", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. Cyber Reticle HUD Overlay
        CyberReticleCanvas(
            scanlineProgress = scanlineY,
            isError = statusMessage != null,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top Controls HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cyber Status Pill
            Box(
                modifier = Modifier
                    .background(Color(0xCC0A121A), RoundedCornerShape(8.dp))
                    .border(1.dp, if (statusMessage != null) Color(0xFFFF5252) else Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (statusMessage != null) Color(0xFFFF5252) else Color(0xFF00E676), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "D-COM // OPTICAL SENSOR LOCK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Top Right Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flashlight / Torch Toggle (if available)
                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC101C26))
                            .border(1.dp, Color(0xFF4DD0E1), CircleShape)
                            .clickable {
                                isTorchOn = !isTorchOn
                                camera?.cameraControl?.enableTorch(isTorchOn)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        DigiVectorIcon(
                            type = DigiIconType.BOLT,
                            size = 18.dp,
                            tint = if (isTorchOn) Color(0xFFFFD54F) else Color.White
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC2A1010))
                        .border(1.dp, Color(0xFFFF5252), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    DigiVectorIcon(
                        type = DigiIconType.CLOSE,
                        size = 20.dp,
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }

        // 4. Bottom Instruction & Validation Status Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(Color(0xEE330C10), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DigiVectorIcon(type = DigiIconType.ATTENTION, tint = Color(0xFFFF5252), size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color(0xCC09121D), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E3A4D), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ALIGN QR MATRIX, BARCODE, OR TEXT COMM CODE IN RETICLE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Point camera at Smartwatch LCD, screen text, or physical barcode/QR",
                        fontSize = 9.sp,
                        color = Color(0xFFB0BEC5),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    textRecognizer: TextRecognizer,
    imageProxy: ImageProxy,
    onCodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        try {
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    var barcodeFound = false
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            barcodeFound = true
                            onCodeDetected(rawValue)
                            break
                        }
                    }
                    if (!barcodeFound) {
                        // Optical OCR recognition for printed or screen-displayed CommCodes & DigiROMs
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val extracted = extractCommCodeFromText(visionText.text)
                                if (!extracted.isNullOrBlank()) {
                                    onCodeDetected(extracted)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("CameraBattleScanner", "Text recognition failure", e)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("CameraBattleScanner", "Barcode analysis failure", e)
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val extracted = extractCommCodeFromText(visionText.text)
                            if (!extracted.isNullOrBlank()) {
                                onCodeDetected(extracted)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
        } catch (e: Exception) {
            Log.w("CameraBattleScanner", "Scanner submission failure", e)
            imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}

/**
 * Extracts a valid 128-bit CommCode or DigiROM packet from OCR-recognized text.
 */
fun extractCommCodeFromText(rawText: String): String? {
    if (rawText.isBlank()) return null
    val clean = rawText.trim()
    try {
        CommCodeCodec.decodeAuto(clean)
        return clean
    } catch (_: Exception) {}

    // Check for DigiROM regex: V1-XXXX-XXXX-XXXX or similar
    val digiRomRegex = Regex("""(?:V[0-9]|D[0-9]|A[0-9]|[0-9A-Fa-f]{2})-[0-9A-Fa-f]{3,4}-[0-9A-Fa-f]{3,4}(?:-[0-9A-Fa-f]{3,4})*""", RegexOption.IGNORE_CASE)
    digiRomRegex.find(clean)?.let { match ->
        try {
            CommCodeCodec.decodeAuto(match.value)
            return match.value
        } catch (_: Exception) {}
    }

    // Check for 26-character Base32 Comm Code
    val base32Regex = Regex("""[A-Za-z2-7]{4,6}(?:-[A-Za-z2-7]{4,6}){3,6}|[A-Za-z2-7]{26}""")
    base32Regex.find(clean)?.let { match ->
        val candidate = match.value.replace("-", "").uppercase()
        if (candidate.length == 26) {
            try {
                CommCodeCodec.decode128(candidate)
                return candidate
            } catch (_: Exception) {}
        }
    }

    // Check individual tokens
    for (line in clean.lines()) {
        for (token in line.split(" ", "\t", ",", ";", "|")) {
            val candidate = token.trim().trim('"', '\'', '(', ')', '[', ']', '<', '>')
            if (candidate.length >= 10) {
                try {
                    CommCodeCodec.decodeAuto(candidate)
                    return candidate
                } catch (_: Exception) {}
            }
        }
    }
    return null
}

@Composable
private fun CyberReticleCanvas(
    scanlineProgress: Float,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isError) Color(0xFFFF5252) else Color(0xFF00E5FF)
    val laserColor = if (isError) Color(0xFFFF1744) else Color(0xFF00E676)

    Canvas(modifier = modifier) {
        val boxSize = 250.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f - 20.dp.toPx())
        val half = boxSize / 2f

        val left = center.x - half
        val right = center.x + half
        val top = center.y - half
        val bottom = center.y + half

        // Dim background outside reticle (scrim)
        drawRect(Color(0x99000000), Offset(0f, 0f), Size(size.width, top))
        drawRect(Color(0x99000000), Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(Color(0x99000000), Offset(0f, top), Size(left, boxSize))
        drawRect(Color(0x99000000), Offset(right, top), Size(size.width - right, boxSize))

        // Reticle Corner Brackets
        val cornerLen = 28.dp.toPx()
        val stroke = 3.dp.toPx()

        val path = Path().apply {
            // Top-Left
            moveTo(left, top + cornerLen)
            lineTo(left, top)
            lineTo(left + cornerLen, top)

            // Top-Right
            moveTo(right - cornerLen, top)
            lineTo(right, top)
            lineTo(right, top + cornerLen)

            // Bottom-Left
            moveTo(left, bottom - cornerLen)
            lineTo(left, bottom)
            lineTo(left + cornerLen, bottom)

            // Bottom-Right
            moveTo(right - cornerLen, bottom)
            lineTo(right, bottom)
            lineTo(right, bottom - cornerLen)
        }

        drawPath(path = path, color = primaryColor, style = Stroke(width = stroke))

        // Center Crosshair Tick Marks
        val tickLen = 10.dp.toPx()
        drawLine(primaryColor.copy(alpha = 0.6f), Offset(center.x - tickLen, center.y), Offset(center.x + tickLen, center.y), 1.5f)
        drawLine(primaryColor.copy(alpha = 0.6f), Offset(center.x, center.y - tickLen), Offset(center.x, center.y + tickLen), 1.5f)

        // Animated Laser Scanline with Gradient Glow
        val scanlineY = top + (boxSize * scanlineProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    laserColor.copy(alpha = 0f),
                    laserColor.copy(alpha = 0.95f),
                    Color.White,
                    laserColor.copy(alpha = 0.95f),
                    laserColor.copy(alpha = 0f)
                ),
                startX = left,
                endX = right
            ),
            start = Offset(left, scanlineY),
            end = Offset(right, scanlineY),
            strokeWidth = 2.5f
        )
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F17))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2735))
                .border(2.dp, Color(0xFF00E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            DigiVectorIcon(
                type = DigiIconType.QR,
                tint = Color(0xFF00E5FF),
                size = 36.dp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "OPTICAL COMBAT SCANNER OFFLINE",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Camera access is required for real-time QR matrix capture and instant point-and-shoot PvP matchmaking.",
            fontSize = 11.5.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("GRANT CAMERA PERMISSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("RETURN TO D-COM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
