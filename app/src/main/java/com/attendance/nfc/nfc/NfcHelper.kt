package com.attendance.nfc.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

/**
 * Wraps Android's NFC foreground dispatch so any tag tapped while this
 * activity is on top is routed straight to onNewIntent — no manual
 * tech-list filtering needed since we only care about the tag's UID,
 * not its payload.
 *
 * NOTE: this reads 13.56 MHz NFC tags (ISO14443A/B, MIFARE, NDEF, etc.).
 * It CANNOT read 125 kHz proximity RFID cards (EM4100/HID Prox) — those
 * use a different radio frequency phones don't have hardware for.
 * Verify your ID cards are NFC, not 125kHz RFID, before relying on this.
 */
class NfcHelper(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun isNfcAvailable(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        if (!isNfcAvailable() || !isNfcEnabled()) return
        try {
            val intent = Intent(activity, activity.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

            val filters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            )
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
        } catch (_: Exception) {
            // Guard against state errors when activity lifecycle changes
        }
    }

    fun disableForegroundDispatch() {
        if (!isNfcAvailable()) return
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {
            // Guard against state errors
        }
    }

    fun openNfcSettings() {
        try {
            activity.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
        } catch (_: Exception) {
            try {
                activity.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        /** Extracts the tag UID as an uppercase hex string, e.g. "04A3B2C1". */
        fun extractUid(intent: Intent): String? {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            val uidBytes = tag?.id ?: return null
            if (uidBytes.isEmpty()) return null
            return uidBytes.joinToString("") { "%02X".format(it) }
        }
    }
}
