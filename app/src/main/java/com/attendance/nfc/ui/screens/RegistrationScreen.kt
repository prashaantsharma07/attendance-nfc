package com.attendance.nfc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.ui.components.GlassBackground
import com.attendance.nfc.ui.components.GlassCard
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.AccentBlueBright
import com.attendance.nfc.ui.theme.TextPrimary
import com.attendance.nfc.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    classId: Long,
    rfid: String,
    repository: AttendanceRepository,
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("New Student", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = AccentBlueBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            " Register Scanned Card",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        "ID: $rfid",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = rollNo,
                        onValueChange = { rollNo = it },
                        label = { Text("Roll Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank() && rollNo.isNotBlank()) {
                                isSubmitting = true
                                scope.launch {
                                    repository.registerAndMark(classId, rfid, name, rollNo)
                                    onRegistrationComplete()
                                }
                            }
                        },
                        enabled = name.isNotBlank() && rollNo.isNotBlank() && !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.Black,
                            disabledContainerColor = AccentBlue.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            if (isSubmitting) "Registering..." else "Register & Mark Present",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary
)
