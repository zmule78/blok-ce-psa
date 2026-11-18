package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.scanner.ParsedReceiptInfo
import com.example.scanner.ReceiptOcrParser
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950
import com.example.ui.theme.adaptiveColors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedReceiptResult(
    val receiptImageUrl: String,
    val parsed: ParsedReceiptInfo
)

@Composable
fun ReceiptScannerDialog(
    onReceiptScanned: (ScannedReceiptResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ac = adaptiveColors

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedPermission by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val hasCameraHardware = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    DisposableEffect(textRecognizer) {
        onDispose { textRecognizer.close() }
    }

    fun processBitmapAndFinish(bitmap: Bitmap) {
        isProcessing = true
        errorMsg = null
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                scope.launch {
                    val dataUri = withContext(Dispatchers.Default) {
                        ReceiptOcrParser.compressBitmapToDataUri(bitmap)
                    }
                    val parsed = ReceiptOcrParser.parse(visionText.text)
                    isProcessing = false
                    onReceiptScanned(ScannedReceiptResult(dataUri, parsed))
                }
            }
            .addOnFailureListener {
                scope.launch {
                    val dataUri = withContext(Dispatchers.Default) {
                        ReceiptOcrParser.compressBitmapToDataUri(bitmap)
                    }
                    isProcessing = false
                    onReceiptScanned(
                        ScannedReceiptResult(
                            dataUri,
                            ParsedReceiptInfo(rawText = "", detectedAmount = null, detectedMethod = null, summaryText = "Struk terlampir (foto)")
                        )
                    )
                }
            }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val bmp = withContext(Dispatchers.IO) {
                    ReceiptOcrParser.decodeUriBitmap(context, uri)
                }
                if (bmp != null) {
                    processBitmapAndFinish(bmp)
                } else {
                    isProcessing = false
                    errorMsg = "Gagal membaca gambar struk dari galeri."
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedPermission = true
        hasCameraPermission = granted
        if (!granted) {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        } else {
            permanentlyDenied = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !hasRequestedPermission && hasCameraHardware) {
            hasRequestedPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Emerald950)
        ) {
            when {
                !hasCameraHardware || !hasCameraPermission -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                            border = BorderStroke(1.dp, ac.borderSubtle),
                            modifier = Modifier.widthIn(max = 420.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = ac.emeraldSurface,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.DocumentScanner,
                                            contentDescription = null,
                                            tint = ac.emeraldText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.scan_receipt_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ac.textPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (!hasCameraHardware) {
                                        "Kamera tidak terdeteksi. Anda dapat memilih foto struk/bukti transfer langsung dari galeri perangkat."
                                    } else if (permanentlyDenied) {
                                        "Izin kamera ditolak permanen. Buka Pengaturan atau pilih foto bukti transfer dari galeri."
                                    } else {
                                        "Izinkan kamera untuk memindai struk dengan ML Kit OCR atau pilih gambar struk dari galeri."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ac.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (errorMsg != null) {
                                    Text(
                                        text = errorMsg.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ac.roseText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Button(
                                    onClick = {
                                        galleryPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald700,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("pick_receipt_gallery_fallback_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pilih Bukti dari Galeri", fontWeight = FontWeight.Bold)
                                }
                                if (hasCameraHardware) {
                                    OutlinedButton(
                                        onClick = {
                                            if (permanentlyDenied) {
                                                val intent = Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.fromParts("package", context.packageName, null)
                                                )
                                                context.startActivity(intent)
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (permanentlyDenied) Icons.Filled.Settings else Icons.Filled.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (permanentlyDenied) "Buka Pengaturan Kamera" else "Izinkan Kamera"
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("close_receipt_scanner_fallback_button")
                                ) {
                                    Text("Tutup")
                                }
                            }
                        }
                    }
                }
                else -> {
                    LiveReceiptCameraContent(
                        textRecognizer = textRecognizer,
                        isProcessing = isProcessing,
                        errorMsg = errorMsg,
                        onCaptureBitmap = ::processBitmapAndFinish,
                        onPickFromGallery = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCaptureError = { msg -> errorMsg = msg },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveReceiptCameraContent(
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    isProcessing: Boolean,
    errorMsg: String?,
    onCaptureBitmap: (Bitmap) -> Unit,
    onPickFromGallery: () -> Unit,
    onCaptureError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var liveOcrPreview by remember { mutableStateOf("Arahkan ke struk / bukti transfer...") }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(cameraController, lifecycleOwner, textRecognizer) {
        cameraController.setImageAnalysisAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOf(textRecognizer),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor
            ) { result: MlKitAnalyzer.Result? ->
                val detected = result?.getValue(textRecognizer)?.text?.trim().orEmpty()
                if (detected.isNotBlank()) {
                    val info = ReceiptOcrParser.parse(detected)
                    liveOcrPreview = info.summaryText.ifBlank { "Teks struk terdeteksi" }
                }
            }
        )
        try {
            cameraController.bindToLifecycle(lifecycleOwner)
        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Failed to bind CameraX", e)
        }
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("receipt_scanner_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.receipt_scanner_close_cd),
                        tint = Color.White
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        tint = Emerald100,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.scan_receipt_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        // Bottom live OCR + capture bar (elevated clearly above device home/navigation bar)
        Surface(
            color = Color.Black.copy(alpha = 0.80f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.45f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .union(WindowInsets(bottom = 56.dp))
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "OCR Live ML Kit: $liveOcrPreview",
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald100,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFDA4AF)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("pick_receipt_gallery_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri")
                    }

                    Button(
                        onClick = {
                            cameraController.takePicture(
                                mainExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = imageProxyToBitmap(image)
                                        image.close()
                                        if (bmp != null) {
                                            onCaptureBitmap(bmp)
                                        } else {
                                            onCaptureError("Gagal memproses hasil tangkapan kamera.")
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        onCaptureError("Kamera gagal memotret: ${exception.message}")
                                    }
                                }
                            )
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 48.dp)
                            .testTag("capture_receipt_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Potret & Scan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val raw = image.toBitmap()
        val rot = image.imageInfo.rotationDegrees.toFloat()
        if (rot == 0f) raw else {
            val matrix = Matrix().apply { postRotate(rot) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        }
    } catch (_: Exception) {
        null
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
