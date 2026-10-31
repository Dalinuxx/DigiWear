# DigiWear v0.6.0 Release Notes

### Core Architecture & Performance Optimizations
- **O(1) Indexed Recipe Lookups**: Added pre-indexed lookup maps (`RECIPES_BY_TARGET` and `RECIPES_BY_ANY_SPECIES`) in `JogressEngine` and optimized `DigiDexEngine` species unlock hints, reducing species list rendering overhead from O(N) to O(1).
- **Watch Face Render Loop Hardening**: Added 30-second TTL battery percentage caching and zero-allocation scratch rect re-use in `DigiWatchFaceService`, eliminating per-frame Binder IPC queries and garbage collection pauses at 12fps.
- **12-Frame Procedural Sprite Frame Uniqueness**: Refined `SpriteMatrixRegistry` frame transforms so all 12 frames (`IDLE_1`, `IDLE_2`, `WALK_1`, `WALK_2`, `FEED_START`, `JOY_EAT`, `SLEEP_1`, `SLEEP_2`, `REFUSAL`, `ATTACK_CHARGE`, `ATTACK_RELEASE`, `INJURED`) produce distinct matrices across all 550 species (including horizontally symmetric Digimon and eyelid glints).

### Wear OS Smartwatch Overhaul & Safe-Zone Geometry
- **Circular Bezel Insets**: Overhauled `WatchDisplayConfig` on round displays (`horizPad = 14.dp, vertPad = 26.dp`), keeping list headers, complication controls, and action buttons safely inside the circular bezel.
- **Modal Overlays & Opaque Scrims**: Converted semi-transparent `0xEE` modal scrims to opaque backgrounds to eliminate high-contrast text ghosting; narrowed modal cards (`.fillMaxWidth(0.78f..0.86f)`) in `MainPetScreen`, `CutInOverlay`, `WatchDigiDexScreen`, and `AdventureScreen`.
- **Battle QR Matrix & Defeat Screen**: Scaled down the on-wrist QR matrix container (`96.dp`) so the bottom `SHOW TEXT CODE` action is 100% visible inside the lower bezel chord; adjusted combat formula summary typography to eliminate orphan line wraps.
- **Tile & Complication Fallbacks**: Hardened `DigiWearTileService` to display `// CRYO-STASIS` whenever a partner is in cold sleep.

### Phone Companion App & Widget Improvements
- **Home Screen AppWidget Vault Fallback**: Updated `DigiWearAppWidgetProvider` to fall back to the stored partner in Cryo-Vault when no active smartwatch has synced, preventing default Botamon resets on phone-only installs.
- **System Status Bar Insets**: Forced light status-bar icons (`isAppearanceLightStatusBars = false`) across dark application bars and added `.statusBarsPadding()` to the full-screen Wireless ADB Sideloader modal.
- **Responsive Theme Swatch & Shell Grids**: Replaced horizontal-scroll strips in `SettingsScreen` with a 2-column theme grid and single-row equal-weight color swatches, and converted the 6-shell selector in `PetDashboardScreen` into a responsive 2x3 grid.
- **Embedded Sideload Binaries**: Recompiled R8-minified, release-signed watch binaries (`versionCode = 19`, `versionName = "0.6.0"`) and synchronized them to `release_bin/` and `phone-companion-app` assets for direct Wi-Fi watch installation without a PC.
