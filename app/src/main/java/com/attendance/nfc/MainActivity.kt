package com.attendance.nfc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.attendance.nfc.data.AppDatabase
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.nfc.NfcHelper
import com.attendance.nfc.nfc.NfcScanBus
import com.attendance.nfc.ui.navigation.AppNavGraph
import com.attendance.nfc.ui.theme.AttendanceNfcTheme

class MainActivity : ComponentActivity() {

    private lateinit var nfcHelper: NfcHelper
    private lateinit var repository: AttendanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcHelper = NfcHelper(this)
        repository = AttendanceRepository(AppDatabase.getInstance(applicationContext))

        // Process tag UID if app was cold-launched by an NFC tap
        NfcHelper.extractUid(intent)?.let { uid -> NfcScanBus.emit(uid) }

        setContent {
            AttendanceNfcTheme {
                AppNavGraph(
                    repository = repository,
                    nfcAvailable = nfcHelper.isNfcAvailable(),
                    isNfcEnabled = { nfcHelper.isNfcEnabled() },
                    onOpenNfcSettings = { nfcHelper.openNfcSettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Foreground dispatch must be re-enabled every time the activity comes
        // to the front, or a background app could otherwise intercept the tag.
        if (nfcHelper.isNfcAvailable()) {
            nfcHelper.enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        if (nfcHelper.isNfcAvailable()) {
            nfcHelper.disableForegroundDispatch()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NfcHelper.extractUid(intent)?.let { uid -> NfcScanBus.emit(uid) }
    }
}
