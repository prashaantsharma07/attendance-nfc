package com.attendance.nfc.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.data.MarkResult
import com.attendance.nfc.nfc.NfcScanBus
import com.attendance.nfc.ui.components.GlassBackground
import com.attendance.nfc.ui.components.GlassCard
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.SuccessGreen
import com.attendance.nfc.ui.theme.TextPrimary
import com.attendance.nfc.ui.theme.TextSecondary
import com.attendance.nfc.ui.theme.WarnAmber

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import com.attendance.nfc.ui.theme.AccentBlueBright

data class ScanLogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType { SUCCESS, INFO, WARNING }

@Composable
fun ScanScreen(
    classId: Long,
    repository: AttendanceRepository,
    nfcAvailable: Boolean,
    isNfcEnabled: () -> Boolean = { true },
    onOpenNfcSettings: () -> Unit = {},
    onBack: () -> Unit,
    onUnknownCard: (String) -> Unit
) {
    var className by remember { mutableStateOf("") }
    val scanLogs = remember { mutableStateListOf<ScanLogEntry>() }
    val scannedUid by NfcScanBus.scannedUid.collectAsState()
    var showManualScanDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        NfcScanBus.clear()
        onDispose {
            NfcScanBus.clear()
        }
    }

    LaunchedEffect(classId) {
        className = repository.getClassById(classId)?.name.orEmpty()
    }

    LaunchedEffect(scannedUid) {
        val uid = scannedUid ?: return@LaunchedEffect
        NfcScanBus.clear()

        val result = repository.scanAndMark(classId, uid)
        when (result) {
            is MarkResult.Success -> {
                val msg = if (result.alreadyMarkedToday) {
                    "${result.student.name} already marked."
                } else {
                    "Marked ${result.student.name} present."
                }
                scanLogs.add(0, ScanLogEntry(
                    message = msg,
                    type = if (result.alreadyMarkedToday) LogType.INFO else LogType.SUCCESS
                ))
            }
            MarkResult.UnknownCard -> {
                onUnknownCard(uid)
            }
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(className.ifBlank { "Class" }, style = MaterialTheme.typography.titleLarge)
                    Text("Scanning Mode", style = MaterialTheme.typography.bodyMedium, color = AccentBlue)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ready to Scan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        "Hold a student ID card against the back of the phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { showManualScanDialog = true },
                        modifier = Modifier.padding(top = 14.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Manual / Test Scan", color = AccentBlue)
                    }
                }
            }

            Text(
                "Scan Log",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(scanLogs) { entry ->
                    LogItem(entry)
                }
            }
        }
    }

    if (showManualScanDialog) {
        ManualScanDialog(
            onDismiss = { showManualScanDialog = false },
            onSubmit = { uid ->
                showManualScanDialog = false
                NfcScanBus.emit(uid.trim().uppercase())
            }
        )
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
            .size(80.dp * scale)
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
            modifier = Modifier.size(48.dp),
            tint = AccentBlue
        )
    }
}

@Composable
private fun ManualScanDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var uidText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual / Test Card Scan") },
        text = {
            Column {
                Text(
                    "Enter a card UID to simulate a scan (e.g. 04A1B2C3), or use this if cards are 125kHz RFID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = uidText,
                    onValueChange = { uidText = it },
                    label = { Text("Card UID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (uidText.isNotBlank()) onSubmit(uidText) }) {
                Text("Simulate Scan")
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
