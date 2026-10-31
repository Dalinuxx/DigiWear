# DigiWear v0.2.0 Official Release Notes

### Complete Overview
DigiWear v0.2.0 represents a complete, exhaustive overhaul across both the standalone smartwatch application and the Android phone companion application, achieving 100% parity with historical Bandai virtual pets while leveraging modern wearable hardware, NFC, optical QR matchmaking, and wireless protocols.

---

### Highlights & New Features

#### 1. 500-Species Complete Canonical Roster
- 500 authentic Digimon from Baby I to Super Ultimate (16 Baby I, 34 Baby II, 85 Child, 117 Adult, 111 Perfect, 137 Ultimate/Super Ultimate).
- 500 bespoke 16×16 multi-tone pixel matrices and 500 unique canonical color palettes with 12 distinct animation frames per monster (idle breathing, LCD roaming walk, multi-bite feeding, chewing/joy jump, sleeping with anatomy-aware eye closure, refusal, attack charge, attack release, and injured/recoil).
- Rendered on a 64×64 matrix at `FilterQuality.None` for pristine pixel sharpness across round and square watch displays and the home screen launcher widget.
- 16 Starter Digi-Eggs covering every historical generation (1997 V1–V6, Pendulum, D-3, Digimon X, and Digimon Color).

#### 2. 100% Evolutionary Reachability & 85 DNA Jogress / DigiXros Recipes
- Every single one of the 500 species can be naturally reached from the 16 starter eggs.
- Zero orphaned species and zero dead ends.
- 85 canonical DNA Jogress, DigiXros, and Mode Change fusions with dedicated 1-tap chambers on both watch and companion app.
- Full visual sprite sheet evolution poster (`docs/digiwear_evolution_guide_chart.png`) and 500-species master sprite sheet (`docs/digiwear_roster_spritesheet.png`).

#### 3. 4-Mode Battle System with Face-to-Face Animated Arena & Optical QR Scanner
- **Adventure Colosseum (5 Grand Sagas, 114 Sectors)**: Physical-step exploration and turn-by-turn boss combat with environmental weather hazards and 3-phase combat sprite choreography (`ATTACK_CHARGE` → `ATTACK_RELEASE` → `INJURED` recoil).
- **Wild NFC & Barcode Scanner**: Scan physical NFC cards or everyday retail UPC/EAN product barcodes with your phone camera to spawn unique wild Digimon encounters with an 18% rare boss spawn rate.
- **NFC Tap-to-Duel & Optical QR Matchmaking**: Direct watch-to-watch, watch-to-phone, or phone-to-phone peer-to-peer duels via NFC or on-wrist 128-bit QR Code matrices scanned by the Companion CameraX/MLKit scanner.
- **A-Com / D-Com / WiFiCom Online Battles**: Fully compatible with the 128-bit Crockford Base32 CommCode standard (`DW2-...`) and original 16/26-character DigiROM protocols (`V1-XXXX-XXXX-XXXX`). Includes 1-tap NFC & DataLayer relay from watch to phone.

#### 4. Broad Hardware Support: Wear OS & Standalone AOSP Standard Watches
- Seamless support for Wear OS 3.0, 4.0, and 5.0+ (Galaxy Watch 4/5/6/7/Ultra, Pixel Watch 1/2/3, TicWatch, OnePlus Watch 2).
- First-class support for standalone AOSP / Full Android Watches (Kospet, Lokmat, Lemfo, Zeblaze, Rogbid) without Google Play Services.
- **AOSP Wi-Fi & Hotspot LAN Bridge**: Built-in TCP sync server (port 45820) and parallel UDP/subnet discovery responder (port 45821) for zero-GMS wireless pairing.
- **Display Shape Override (`AUTO / ROUND / SQUARE`)**: Resolves circular bezel clipping on round AOSP watches running square phone ROMs.
- **3-Tier Universal Pedometer**: Hardware step counter, hardware step detector, and raw 3-axis accelerometer peak detector fallback.
- **Expanded Hardware Key Mappings & Native BackHandler**: Full support for physical stem buttons (`[A]`, `[B]`, `[C]`), volume keys, rotating crowns, and Android back gestures across every screen.

#### 5. Complete Android Companion App & Home Screen Widget
- **DigiVault Cryo-Storage**: Zero-decay cold storage for raised Digimon with instant deployment and dual CommCode/DigiROM export.
- **500-Species Sprite Studio & DigiDex**: 12-frame animation viewer with authentic Bandai LCD screen overlays and evolution lineage inspector.
- **Story Campaign Architect**: Custom campaign editor with 5 Grand Saga presets and direct wireless push to watch.
- **Android Home Screen AppWidget**: Crisp nearest-neighbor scaled pixel art sprite, stage, weight, hunger hearts, and strength stars with `goAsync()` quick-care actions.
- **Cloud JSON Backup & Restore**: Full archive import/export.

---

### Release Binaries (`release_bin/`)
- `DigiWear-WearOS-release.apk` (Wear OS 3.0+ Signed Release)
- `DigiWear-Companion-release.apk` (Android Phone Companion Signed Release)
- `digiwear-aosp-standard-watch-release.apk` (Standalone AOSP / Full Android Watch Signed Release)
- `wear-os-app-release.apk` & `phone-companion-app-release.apk` (Standard Gradle module names)
- `wear-os-app-debug.apk` & `phone-companion-app-debug.apk` (Development Debug builds)
