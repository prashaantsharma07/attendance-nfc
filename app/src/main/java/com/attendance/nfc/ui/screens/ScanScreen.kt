package com.attendance.nfc.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.data.MarkResult
import com.attendance.nfc.nfc.NfcScanBus
import com.attendance.nfc.ui.components.BarcodeScannerView
import com.attendance.nfc.ui.components.GlassBackground
import com.attendance.nfc.ui.components.GlassCard
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.AccentBlueBright
import com.attendance.nfc.ui.theme.SuccessGreen
import com.attendance.nfc.ui.theme.TextPrimary
import com.attendance.nfc.ui.theme.TextSecondary
import com.attendance.nfc.ui.theme.WarnAmber
import kotlinx.coroutines.launch

data class ScanLogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType { SUCCESS, INFO, WARNING }

enum class ScanMode { NFC, BARCODE }

@Composable
fun ScanScreen(
    classId: Long,
    repository: AttendanceRepository,
    nfcAvailable: Boolean,
    isNfcEnabled: () -> Boolean = { true },
    onOpenNfcSettings: () -> Unit = {},
    onBack: () -> Unit,
    onUnknownCard: (String) -> Unit,
    onUnknownBarcode: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var className by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(ScanMode.NFC) }
    val scanLogs = remember { mutableStateListOf<ScanLogEntry>() }
    val scannedUid by NfcScanBus.scannedUid.collectAsState()

    var showManualNfcDialog by remember { mutableStateOf(false) }
    var showManualBarcodeDialog by remember { mutableStateOf(false) }

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

    DisposableEffect(Unit) {
        NfcScanBus.clear()
        onDispose {
            NfcScanBus.clear()
        }
    }

    LaunchedEffect(classId) {
        className = repository.getClassById(classId)?.name.orEmpty()
    }

    // Process NFC scans whenever a tag is detected
    LaunchedEffect(scannedUid) {
        val uid = scannedUid ?: return@LaunchedEffect
        NfcScanBus.clear()

        val result = repository.scanAndMark(classId, uid)
        when (result) {
            is MarkResult.Success -> {
                val msg = if (result.alreadyMarkedToday) {
                    "${result.student.name} already marked."
                } else {
                    "Marked ${result.student.name} present (via NFC)"
                }
                scanLogs.add(0, ScanLogEntry(
                    message = msg,
                    type = if (result.alreadyMarkedToday) LogType.INFO else LogType.SUCCESS
                ))
            }
            is MarkResult.UnknownCard -> {
                onUnknownCard(uid)
            }
            is MarkResult.UnknownBarcode -> {
                onUnknownBarcode(result.barcode)
            }
        }
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(className.ifBlank { "Class" }, style = MaterialTheme.typography.titleLarge)
                    Text("Attendance Marking", style = MaterialTheme.typography.bodyMedium, color = AccentBlue)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: NFC Tap vs Barcode Scanner
            ModeSelector(
                currentMode = currentMode,
                onModeSelected = { mode ->
                    currentMode = mode
                    if (mode == ScanMode.BARCODE && !hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Content
            when (currentMode) {
                ScanMode.NFC -> {
                    NfcModeContent(
                        nfcAvailable = nfcAvailable,
                        isNfcEnabled = isNfcEnabled,
                        onOpenNfcSettings = onOpenNfcSettings,
                        onManualScanClick = { showManualNfcDialog = true }
                    )
                }
                ScanMode.BARCODE -> {
                    BarcodeModeContent(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onBarcodeScanned = { barcode ->
                            scope.launch {
                                val result = repository.scanAndMarkBarcode(classId, barcode)
                                when (result) {
                                    is MarkResult.Success -> {
                                        val msg = if (result.alreadyMarkedToday) {
                                            "${result.student.name} already marked."
                                        } else {
                                            "Marked ${result.student.name} present (via Barcode)"
                                        }
                                        scanLogs.add(0, ScanLogEntry(
                                            message = msg,
                                            type = if (result.alreadyMarkedToday) LogType.INFO else LogType.SUCCESS
                                        ))
                                    }
                                    is MarkResult.UnknownBarcode -> {
                                        onUnknownBarcode(result.barcode)
                                    }
                                    is MarkResult.UnknownCard -> {
                                        // fallback
                                        onUnknownCard(barcode)
                                    }
                                }
                            }
                        },
                        onManualBarcodeClick = { showManualBarcodeDialog = true }
                    )
                }
            }

            // Scan Log Section
            Text(
                "Scan Log",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(scanLogs) { entry ->
                    LogItem(entry)
                }
            }
        }
    }

    // Dialog for manual NFC simulation
    if (showManualNfcDialog) {
        ManualScanDialog(
            title = "Manual / Test Card Scan",
            label = "Card UID (e.g. 04A1B2C3)",
            prompt = "Enter a card UID to simulate an NFC scan:",
            onDismiss = { showManualNfcDialog = false },
            onSubmit = { uid ->
                showManualNfcDialog = false
                NfcScanBus.emit(uid.trim().uppercase())
            }
        )
    }

    // Dialog for manual Barcode simulation
    if (showManualBarcodeDialog) {
        ManualScanDialog(
            title = "Manual Barcode Entry",
            label = "Barcode / QR Code value",
            prompt = "Type or paste a barcode value to simulate scanning:",
            onDismiss = { showManualBarcodeDialog = false },
            onSubmit = { barcode ->
                showManualBarcodeDialog = false
                scope.launch {
                    val result = repository.scanAndMarkBarcode(classId, barcode)
                    when (result) {
                        is MarkResult.Success -> {
                            val msg = if (result.alreadyMarkedToday) {
                                "${result.student.name} already marked."
                            } else {
                                "Marked ${result.student.name} present (via Barcode)"
                            }
                            scanLogs.add(0, ScanLogEntry(
                                message = msg,
                                type = if (result.alreadyMarkedToday) LogType.INFO else LogType.SUCCESS
                            ))
                        }
                        is MarkResult.UnknownBarcode -> {
                            onUnknownBarcode(result.barcode)
                        }
                        is MarkResult.UnknownCard -> {
                            onUnknownCard(barcode)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ModeSelector(
    currentMode: ScanMode,
    onModeSelected: (ScanMode) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeTab(
                title = "NFC Tap",
                icon = Icons.Filled.Nfc,
                isSelected = currentMode == ScanMode.NFC,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScanMode.NFC) }
            )
            ModeTab(
                title = "Barcode / QR",
                icon = Icons.Filled.QrCodeScanner,
                isSelected = currentMode == ScanMode.BARCODE,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScanMode.BARCODE) }
            )
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) AccentBlue.copy(alpha = 0.25f) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentBlueBright else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = " $title",
                color = if (isSelected) TextPrimary else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NfcModeContent(
    nfcAvailable: Boolean,
    isNfcEnabled: () -> Boolean,
    onOpenNfcSettings: () -> Unit,
    onManualScanClick: () -> Unit
) {
    if (!nfcAvailable) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = WarnAmber)
                Text(
                    "No NFC hardware detected. Use 'Manual / Test Scan' below.",
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    } else if (!isNfcEnabled()) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = WarnAmber)
                    Text(
                        "NFC is turned OFF in device settings.",
                        color = WarnAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Button(
                    onClick = onOpenNfcSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                ) {
                    Text("Turn On NFC in Settings")
                }
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            PulsingNfcIcon()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Ready to Scan NFC",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                "Hold a student ID card against the back of the phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
            OutlinedButton(
                onClick = onManualScanClick,
                modifier = Modifier.padding(top = 14.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Manual / Test Scan", color = AccentBlue)
            }
        }
    }
}

@Composable
private fun BarcodeModeContent(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onManualBarcodeClick: () -> Unit
) {
    if (!hasCameraPermission) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Camera Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    "Grant camera permission to read student barcodes or QR codes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                )
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Camera Scanner")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onManualBarcodeClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Enter Barcode Manually", color = AccentBlue)
                }
            }
        }
    } else {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                // Live camera viewfinder
                BarcodeScannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    onBarcodeScanned = onBarcodeScanned
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Align barcode inside frame",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = onManualBarcodeClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("  Manual Entry", color = AccentBlue, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingNfcIcon() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "nfc-scale"
    )
    Box(
        modifier = Modifier
            .size(76.dp * scale)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent)
                ),
                CircleShape
            )
            .border(1.5.dp, AccentBlue.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Nfc,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = AccentBlue
        )
    }
}

@Composable
private fun ManualScanDialog(
    title: String,
    label: String,
    prompt: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSubmit(text) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LogItem(entry: ScanLogEntry) {
    GlassCard(cornerRadius = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (entry.type) {
                    LogType.SUCCESS -> Icons.Filled.CheckCircle
                    LogType.INFO -> Icons.Filled.Info
                    LogType.WARNING -> Icons.Filled.Warning
                },
                contentDescription = null,
                tint = when (entry.type) {
                    LogType.SUCCESS -> SuccessGreen
                    LogType.INFO -> AccentBlue
                    LogType.WARNING -> WarnAmber
                },
                modifier = Modifier.size(24.dp)
            )
            Text(
                entry.message,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
