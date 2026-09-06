# Attendance NFC

Kotlin + Jetpack Compose Android app: tap an NFC-enabled student ID card to
mark attendance for a class, with a dark glassmorphism UI (black + light blue).

## ⚠️ Read this before building anything on top of it

**Phone NFC readers operate at 13.56 MHz** (ISO14443A/B — MIFARE, NDEF-capable
cards, most modern access cards). They **cannot** read 125 kHz proximity RFID
cards (EM4100, HID Prox), which is what a lot of Indian college ID cards
actually use. Before relying on this app:

1. Open any NFC-tag-reading app (or Settings → NFC on the phone) and tap a
   real student ID card against the back of the phone.
2. If nothing is detected, your cards are 125 kHz — this approach won't work
   with phone hardware at all, regardless of code. You'd need a dedicated
   125 kHz RFID reader module (e.g. RC522 variant or an EM-18 reader) wired
   to a microcontroller that relays the UID to the phone over BLE/USB, and
   the "scan" screen in this app would read from that instead of `NfcAdapter`.

Everything below assumes your cards passed that check.

## What's implemented

- **Class list** — create classes, glass-card list, tap to open.
- **Class detail** — "Mark Today's Attendance" button + a live attendance
  summary table (present/total sessions, %, color-coded by threshold, and identifier badges).
- **Dual NFC & Barcode/QR scan screen** — continuous scan loop:
  - **NFC Tap**: tap card against phone → looks up by UID → marks present.
  - **Camera Barcode/QR Scanner**: live CameraX + on-device Google ML Kit scanner with animated viewfinder reticle, debounce, torch toggle, haptic vibration, and audio beep confirmation.
  - Unknown card or barcode → routes directly to registration.
  - Manual entry dialogs for both NFC and Barcode testing.
- **Student registration** — captures name + roll no. and links an NFC card UID, a Barcode/QR ID, or both, then immediately marks that student present.
- **Local persistence & Migration** — Room database (`attendance.db`) v2 with automated migration script. A
  student registers once and can be marked across multiple classes (roster
  is global, not per-class — flag it if you need isolated per-class rosters).
- Duplicate-scan protection: a unique DB constraint on
  (class, student, date) means tapping or scanning the same person twice in one session
  can't inflate the count.

## Not implemented (scope cuts made to ship something coherent)

- No backend/server or multi-device sync — everything lives on one phone's
  local SQLite DB. If you need attendance visible across multiple devices
  (e.g. a TA's phone and a professor's), that needs a real backend
  (Firebase Firestore is the least-effort option) — say so and I'll wire it in.
- No auth — anyone with the phone can mark attendance or edit rosters.
- No edit/delete UI for classes or students (only additive — add class, add
  student, mark present). Worth adding before real classroom use.
- True backdrop blur (the glass "frosted" look) is API 31+ only via
  `Modifier.blur`; this build fakes it with layered translucency + border,
  which reads as "glass" on every Android version without extra dependencies.
- No ViewModel/DI layer (no Hilt) — repository is a singleton constructed
  directly in `MainActivity` and passed down. Fine for a solo/student
  project; refactor before a team works on this together.

## Building it

Open the project root in Android Studio (Koala/2024.1 or newer) and let
Gradle sync. Verified compatibility: AGP 8.4.0 requires Gradle 8.6+
(the `gradle-wrapper.properties` included is pinned to exactly 8.6), Kotlin
1.9.24 pairs with Compose compiler extension 1.5.14, JDK 17.

**One thing I couldn't include**: the actual `gradle-wrapper.jar` binary
and `gradlew`/`gradlew.bat` scripts — my sandbox can't reach
`services.gradle.org` to fetch them. `gradle-wrapper.properties` (which
pins the Gradle version) is there. Android Studio will detect the missing
wrapper on open and offer to regenerate it automatically from its bundled
Gradle — accept that prompt, or run `gradle wrapper --gradle-version 8.6`
once you have Gradle installed locally.

Test on a **physical device with NFC hardware** — the emulator cannot
simulate NFC tag scans.

## What I verified vs. couldn't

I don't have a Kotlin/Android compiler in this environment, so I hand-traced
every file for import correctness, and specifically checked two things that
were real (not hypothetical) risks:
- `Icons.Filled.NfcOff` and `Icons.Filled.Nfc` exist in the Material icon
  set (`nfc_off`/`nfc` are real Material Symbols names) — confirmed via search.
- AGP 8.4.0 / Gradle 8.6 / Kotlin 1.9.24 / Compose compiler 1.5.14 is a
  valid, documented compatibility chain — confirmed via Android's official
  release notes.

What I did **not** verify: an actual Gradle build. The first sync in Android
Studio is the real test — if something doesn't resolve, it's most likely a
minor version mismatch in one of the pinned dependency versions, which is a
one-line fix.
