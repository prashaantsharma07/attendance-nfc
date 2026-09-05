package com.attendance.nfc.nfc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MainActivity.onNewIntent() runs outside the Compose tree, so there's no
 * direct way to hand a scanned UID to whichever screen is currently
 * listening. This bus bridges the two: the activity emits, the active
 * scan screen collects and clears.
 */
object NfcScanBus {
    private val _scannedUid = MutableStateFlow<String?>(null)
    val scannedUid: StateFlow<String?> = _scannedUid

    fun emit(uid: String) {
        _scannedUid.value = uid
    }

    fun clear() {
        _scannedUid.value = null
    }
}
