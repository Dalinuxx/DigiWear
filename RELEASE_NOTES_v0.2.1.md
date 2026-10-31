# DigiWear v0.2.1 Official Release Notes

### Complete Overview
DigiWear v0.2.1 delivers a comprehensive cross-app consistency overhaul, introducing resolution-independent custom vector assets, full 16-key hardware and simulation settings parity between smartwatch and phone, minute-precision sleep scheduling, streamlined companion navigation, persistent adventure campaign progress, and refined high-contrast cyber retro styling.

---

### Highlights & New Features

#### 1. 100% Custom Vector Asset System (`DigiVectorIcon`)
- **Zero System Emojis & Zero Material Icons**: Replaced all placeholder emojis and standard Google Material Icons with authentic, resolution-independent vector paths across both the Companion App and Smartwatch App.
- **Retro Cyber Iconography**: Bespoke canvas vector drawings for Meat, Protein, Training Glove, Crossed Swords, Flush Wave, Bandage, Calling Attention, Digi-Egg, Quest Compass, Radar Scanner, Vault Safe, NFC Wave, Camera, Beam Antenna, and Help Question Badge.
- **Crisp AMOLED Scaling**: All icons render with sub-pixel sharpness across circular smartwatches, rectangular screens, and phone displays at any DPI.

#### 2. Cross-App Settings Parity & Simulation Engine Refinement
- **Minute-Precision Sleep Schedule Parser**: Rewrote `PetSimulationEngine.isSleepWindowWithSchedule()` to parse custom minute-accurate bedtime and wake windows (e.g. `20:00–07:00`, `21:00–07:00`, `22:00–08:00`, `23:00–08:00`), supporting midnight-crossing schedules with safe canonical fallbacks.
- **Full 16-Key Settings Synchronization**: Added dedicated companion UI selectors for:
  - **Button Rail Placement** (`RIGHT (BANDAI OG)` vs `LEFT`)
  - **Canonical 1997 Lifespan** (Faithful 1997 death and traited egg inheritance vs modern immortal)
  - **Battle Injuries** (25% defeat injury risk requiring first aid vs safe sparring)
  - **Sleep Schedule Window** (`20:00–07:00`, `21:00–07:00`, `22:00–08:00`, `23:00–08:00`)
  - **Ambient Display Style** (`MINIMAL DOT`, `OUTLINE WIRE`, `DIMMED RETRO`)
- **Dual Bridge Transmission**: Beaming settings from the Companion transmits the complete 16-setting contract across Wear OS DataLayer and standalone AOSP LAN sockets without resetting watch preferences.

#### 3. Persistent Adventure Sagas & Stage Replay
- **Independent Saga Progression**: Campaign progress is now tracked independently per adventure arc, allowing tamers to switch freely between sagas without losing cleared milestones.
- **Sector Stage Selection**: Jump immediately to frontline progress or replay any cleared sector for training and item drops.
- **Streamlined Quest UI**: Active saga tracking on the main Expedition dashboard, with detailed sector roadmaps and custom campaign architect moved into clean secondary modals.

#### 4. High-Contrast Glowing Borders & UI Polish
- **Active Contrast Glow**: Toggled options, filter chips, vault tabs, and campaign selection cards feature vibrant `1.5.dp` illuminated borders (`#64FFDA`, `#00E5FF`, `#FF5252`) and bold white typography (`Color.White`) to prevent selected items from darkening into dark AMOLED backgrounds.
- **Authentic Virtual Pet Battle Cut-Ins**: Eliminated modern green-dot reticles in favor of canonical manga speedlines, attribute aura bursts, and high-impact battle title banners.
- **Streamlined Companion Header & Nav**: Top bar standardized to `"DigiWear"` with a clean gear icon; Settings text label removed from bottom navigation bar.
- **Debug-Gated Sprite Studio**: 12-frame matrix development studio is isolated behind `BuildConfig.DEBUG` for developer builds, keeping production companion releases focused and lightweight.
- **Sideload Help & Setup Guide**: Repositioned the Wireless ADB guide to a prominent full-width card button with the new `DigiIconType.HELP` question badge.
- **Standardized Watch Dismissal**: All watch overlay screens now offer unified `BACK [C]` return controls for touch and hardware stem navigation.
- **Cyber Styled Dialogs**: Standardized modal alerts with `DigiWearPalette.CardSurface`, cyber borders, and monospace button actions.

---

### Release Binaries (`release_bin/`)
- `DigiWear-WearOS-release.apk` (Wear OS 3.0+ Signed Release, v0.2.1)
- `DigiWear-Companion-release.apk` (Android Phone Companion Signed Release, v0.2.1)
- `digiwear-aosp-standard-watch-release.apk` (Standalone AOSP / Full Android Watch Signed Release, v0.2.1)
- `wear-os-app-debug.apk` & `phone-companion-app-debug.apk` (Development Debug builds, v0.2.1)
