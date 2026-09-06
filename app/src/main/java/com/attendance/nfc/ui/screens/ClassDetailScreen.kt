package com.attendance.nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.data.StudentAttendanceSummary
import com.attendance.nfc.ui.components.GlassBackground
import com.attendance.nfc.ui.components.GlassCard
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.AccentBlueBright
import com.attendance.nfc.ui.theme.ErrorRed
import com.attendance.nfc.ui.theme.SuccessGreen
import com.attendance.nfc.ui.theme.TextPrimary
import com.attendance.nfc.ui.theme.TextSecondary
import com.attendance.nfc.ui.theme.WarnAmber

@Composable
fun ClassDetailScreen(
    classId: Long,
    repository: AttendanceRepository,
    nfcAvailable: Boolean,
    isNfcEnabled: () -> Boolean = { true },
    onOpenNfcSettings: () -> Unit = {},
    onMarkAttendance: () -> Unit,
    onBack: () -> Unit
) {
    var className by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf<List<StudentAttendanceSummary>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(classId, refreshKey) {
        className = repository.getClassById(classId)?.name.orEmpty()
        summary = repository.getAttendanceSummary(classId)
    }

    // Returning from the NFC scan screen doesn't recreate this composable
    // (it's the same back-stack entry), so re-pull the summary on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    className.ifBlank { "Class" },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (!nfcAvailable) {
                GlassCard(modifier = Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = WarnAmber)
                        Text(
                            "This device has no NFC hardware. You can still test attendance with manual UID entry.",
                            modifier = Modifier.padding(start = 12.dp),
                            color = TextSecondary
                        )
                    }
                }
            } else if (!isNfcEnabled()) {
                GlassCard(modifier = Modifier.padding(top = 16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarnAmber)
                            Text(
                                "NFC is turned OFF in device settings.",
                                modifier = Modifier.padding(start = 12.dp),
                                color = WarnAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onOpenNfcSettings,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                        ) {
                            Text("Turn On NFC in Settings")
                        }
                    }
                }
            }

            Button(
                onClick = onMarkAttendance,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Nfc, contentDescription = null)
                Text(
                    "  Mark Today's Attendance",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                "Attendance summary",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)
            )

            if (summary.isEmpty()) {
                GlassCard {
                    Text(
                        "No students registered yet. Mark attendance once to add the first one via an NFC card or Barcode scan.",
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(summary) { row -> StudentSummaryRow(row) }
                }
            }
        }
    }
}

@Composable
private fun StudentSummaryRow(row: StudentAttendanceSummary) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.student.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Roll no. ${row.student.rollNo}", style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.student.rfidUid?.let { uid ->
                        Text(
                            text = "NFC: $uid",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    row.student.barcode?.let { bc ->
                        Text(
                            text = "Barcode: $bc",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlueBright,
                            modifier = Modifier
                                .background(AccentBlueBright.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (row.totalSessions == 0) 0f else row.presentCount.toFloat() / row.totalSessions },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                    color = progressColor(row.percentage),
                    trackColor = Color(0x22FFFFFF)
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "${row.percentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = progressColor(row.percentage),
                    fontWeight = FontWeight.Bold
                )
                Text("${row.presentCount}/${row.totalSessions}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun progressColor(percentage: Float): Color = when {
    percentage >= 75f -> SuccessGreen
    percentage >= 50f -> WarnAmber
    else -> ErrorRed
}
