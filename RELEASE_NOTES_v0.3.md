# DigiWear v0.3.0 Official Release Notes

### Complete Overview
DigiWear v0.3.0 is a milestone release delivering wrist immersion and audio authenticity:
1. A **Standalone Wear OS Interactive Watch Face** (`DigiWatchFaceService`) with authentic 7-segment digital time, wandering partner animations, dynamic touch reactions, and low-power AOD ambient mode.
2. **Interactive ProtoLayout Tiles** on Wear OS allowing instant wrist quick-care actions (`[FEED]`, `[CLEAN]`, `[LIGHTS]`) straight from the tile swipe without opening the full activity.
3. An **Authentic Soundpack Audio Engine** with sub-Hz precision supporting original 1997 Bandai square-wave piezo beeps, 1999 Digivice tri-tone anime chimes, and retro 8-bit chiptune synthesizer.
4. An enhanced **Companion Sideload Engine** with verified direct GitHub release downloads and SPAKE2 TLS 1.3 wireless pairing.

---

### Highlights & New Features

#### 1. Standalone Wear OS Interactive Watch Face (`DigiWatchFaceService`)
- **Authentic 7-Segment LCD Clock**: Procedural vector-based 7-segment digital time renderer featuring classic ghost/inactive shadow segments (`88:88`), colon blinking, and 12h/24h time formatting.
- **Dynamic Partner Wrist Life**: Active partner sprite wander/walk animations (1 Hz) on the LCD canvas beside physiological hunger and strength heart meters, battery level with safe underflow defense, and daily step goal tracker.
- **Dynamic Touch Gestures**:
  - **Single Tap**: Triggers partner dual-hop joy bounce emote with wrist haptic feedback and chime.
  - **Double Tap**: Instantly launches `MainActivity` for full pet management.
- **Always-On Ambient Mode (AOD)**: Low-power monochrome wireframe/dimmed outline matrices with a pure black background (OPR < 15%) to prevent display burn-in on AMOLED screens. Tap events are automatically suppressed during ambient mode to save battery.

#### 2. Interactive Quick-Care ProtoLayout Tiles
- **One-Tap Quick Actions**: Added interactive buttons right on the Wear OS Tile: `[FEED]` (meal or protein), `[CLEAN]` (flush waste), and `[LIGHTS]` (toggle sleep state).
- **Zero-History Translucent Pipeline**: Executed through `TileQuickCareActivity`, updating SQLite in the background, invalidating tile and complication layouts, dismissing attention notifications, and safely releasing audio sinks with zero window flicker.

#### 3. Authentic Soundpack Audio Engine
- **Soundpack Profiles (`SoundpackProfile.kt`)**:
  - **`ORIGINAL_1997`**: Faithful 2.048 kHz square-wave piezo beeps (classic 4-beep call, 18ms button chirp, attack impact, alternating evolution warning, and death siren).
  - **`DIGIVICE_1999`**: Classic tri-tone anime/digivice chimes (encounter chime, evolution fanfare, victory jingle, glass clicks).
  - **`CHIPTUNE_RETRO`**: Polyphonic 8-bit procedural synthesizer with multi-tone sweeps and arpeggios.
- **Sub-Hz Precision Audio Synth**: Powered by floating-point phase accumulators (`phaseStep = freqHz / sampleRate`) eliminating integer pitch detuning, with `@Synchronized` tone locks and complete lifecycle resource release.
- **Bidirectional Settings Sync**: Select soundpack profiles in Companion or Watch settings with live sound preview chirps and cross-device sync over Wearable DataLayer and AOSP LAN.

#### 4. Verified Sideload Download & Wireless Pairing
- **Direct Release Sourcing**: Sideload downloader in the companion app now connects directly to official GitHub releases for 1-click watch APK downloads with progress reporting.
- **SPAKE2 TLS 1.3 Wireless Pairing**: 6-digit PIN pairing support for Wear OS 3/4/5 smartwatches (Android 11+).

---

### Downloads & Assets
- **Phone Companion App (Release)**: `DigiWear-Companion-release.apk`
- **Wear OS Smartwatch App (Release)**: `DigiWear-WearOS-release.apk`
- **AOSP Standard Smartwatch App (Release)**: `digiwear-aosp-standard-watch-release.apk`
- **Wear OS Debug Build**: `wear-os-app-debug.apk`
- **Phone Companion Debug Build**: `phone-companion-app-debug.apk`
