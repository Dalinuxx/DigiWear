# DigiWear v0.2.2 Official Release Notes

### Complete Overview
DigiWear v0.2.2 introduces custom sleep schedule time picking with 24-hour canonical rest duration validation, procedural custom LCD matrix themes, and custom photo wallpaper imports for smartwatches directly from your phone's photo gallery.

---

### Highlights & New Features

#### 1. Custom Sleep Schedule Setting & OG V-Pet Rest Defense
- **Interactive Time Picker**: Added a `CUSTOM` schedule chip in the Companion App Settings. Tap to configure exact Bedtime (`HH:mm`) and Wake-up time (`HH:mm`) with 24-hour midnight-crossing calculation.
- **Canonical Rest Duration Rule**: Enforces the 1997 classic virtual pet benchmark of at least 6 hours (360 minutes) of sleep. Custom schedules shorter than 6 hours are flagged with an OG V-Pet alert, disabling the apply action to prevent discipline freeze abuse.
- **One-Tap Auto-Alignment**: An `"AUTO-ALIGN TO 8H SLEEP"` button instantly sets wake time to 8 hours past bedtime.
- **Bi-Directional Sync & Persistence**: Transmits custom schedule windows across Wearable DataLayer and AOSP LAN bridges, formatted cleanly on watch faces (e.g. `Custom (23:15–07:30)`) while surviving reboots and theme cycling.

#### 2. Procedural Custom LCD Color & Pattern Matrix Builder
- **Custom Backlight Palette**: Choose between 9 retro and cyber backlight tints (Olive 1997, Amber, Cyber Cyan, Emerald, Silver GB, Violet, Coral Red, Ice Blue, OLED Black).
- **Custom Shadow & Grid Matrix**: Choose custom grid lines and pixel shadow colors.
- **Matrix Patterns**: Procedurally render `DOT_MATRIX`, `GRID`, `SCANLINES`, or `OLED` textures behind your digital monster.
- **Real-Time Preview**: Live 80x80 canvas renderer displays your custom LCD color and pattern matrix with an authentic V-pet partner sprite.

#### 3. Phone Gallery Wallpaper Photo Import
- **Photo Import from Gallery**: Select any wallpaper or photo directly from your device using the system Storage Access Framework picker.
- **OOM-Safe Image Engine**: Power-of-two sub-sampling and bounds decoding safely processes 12MP to 108MP high-resolution camera photos without OutOfMemory crashes.
- **Authentic LCD Grading**: Automatically center-crops to square 390x390, applies subtle 4-tone dot matrix shading, and tunes contrast so black V-pet sprites remain crisp and legible.
- **Seamless Wearable Asset Transfer**: Beams wallpaper bytes over Google Play Services Wearable `Asset` API and AOSP LAN socket bridge directly into smartwatch internal storage.
- **Smart Watch Ambient Dimming**: Seamlessly handles always-on display (AOD) ambient mode transitions on AMOLED smartwatches.

#### 4. Wear OS 3/4/5 Wireless ADB 6-Digit Pairing & TLS 1.3
- **SPAKE2 Pairing Handshake**: Full support for Android 11+ and Wear OS 3, 4, and 5 Wireless Debugging pairing protocol (`adb pair`) over TLS 1.3 without requiring a PC.
- **Dedicated Pairing UI**: New expandable cyber card in the Wireless ADB Sideload screen allows inputting the dynamic Pairing Port and 6-digit Wi-Fi Pairing Code displayed on your smartwatch screen (`Settings > Developer options > Wireless debugging > Pair new device`).
- **On-Device X.509 Certificate Generation**: Generates and persists a 10-year self-signed RSA-2048 certificate using BouncyCastle and Conscrypt for secure mutual TLS (mTLS) authentication.
- **Robust Sideload Streaming & Staging**: Seamlessly pushes and installs watch packages over TLS streams or staged fallback with zero stdin hang issues, preserving backwards compatibility for legacy AOSP port 5555 smartwatches.

#### 5. Full Production Optimization & Efficiency Engine
- **R8 Code & Resource Minification**: Enabled aggressive R8 code shrinking, dead-code stripping, and resource minification across both applications with strict ProGuard keep rules for Room, Serialization, Cryptography, and Wear OS Tiles. Slashes watch APK from 60.3 MB to **40.5 MB** (-33%) and phone APK from 81.2 MB to **66.6 MB** (-18%).
- **Pedometer Flash Storage Protection**: Eliminates continuous flash disk writes while walking by caching hardware step sensor counts in-memory with batched commit flush points.
- **Zero-Hz Compose Quiescence**: Isolates 2 Hz sprite animation frame tickers into leaf composables (`AnimatedVaultPetSprite`, leaf canvas) across the Vault, Dashboard, and Studio, keeping screen roots completely quiescent.
- **Data-Loss Prevention & Schema Migrations**: Eliminates unconstrained destructive migration fallbacks with automated sequential Room database migrations (v1 through v5 on phone, v1 through v4 on watch) and adds indices to timestamp fields.
- **Socket Lifecycle & Wi-Fi Power Conservation**: Added automatic teardown of active Wireless ADB connections when leaving the sideload screen, guarded chiptune synth lazy allocations, and paused LAN sockets during watch ambient sleep to allow Wi-Fi chipsets to enter low-power DTIM sleep.

---

### Downloads & Assets
- **Phone Companion App (Release)**: `DigiWear-Companion-release.apk`
- **Wear OS Smartwatch App (Release)**: `DigiWear-WearOS-release.apk`
- **AOSP Standard Smartwatch App (Release)**: `digiwear-aosp-standard-watch-release.apk`
