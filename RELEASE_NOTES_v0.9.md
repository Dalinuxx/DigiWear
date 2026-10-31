# DigiWear v0.9.0 Release Notes

### 🚶 Interactive Watch Face Walking & Roaming Animation
- **Authentic 2 Hz Roaming Cycle**: Partners now roam dynamically across the watch face with full 8-step walking cycles ([`SpriteFrame.WALK_A`](file:///c:/dev/DigiWatch/shared-vpet-core/src/main/kotlin/com/digiwear/core/model/SpriteFrame.kt) and [`SpriteFrame.WALK_B`](file:///c:/dev/DigiWatch/shared-vpet-core/src/main/kotlin/com/digiwear/core/model/SpriteFrame.kt)), subtle vertical bobs, horizontal directional flips, and idle intervals.
- **Smart Battery Protection**: Driven by an AndroidX WatchFace interactive coroutine that ticks at 500ms intervals only while the watch face is visible and awake. The ticker immediately suspends when ambient mode or sleep state is entered, guaranteeing zero background wakeups and all-day battery life.
- **Sleep & Lights-Out Synchronization**: Seamlessly responds to both natural sleep and artificial "lights off" V-Pet state; when lights are extinguished, the partner rests in sleeping pose (`SLEEP_A` / `SLEEP_B`) and roaming is paused.

---

### 🗺️ Live Adventure Progress Telemetry & Radial Bezel HUD
- **On-Dial Adventure Progression**: Real-time telemetry at the bottom of the dial that cycles every 3.5 seconds between:
  - **Adventure Node Progress**: `🗺️ AREA • Steps / Target` (e.g. `🗺️ NATIVE FOR • 150/500`)
  - **Daily Step Goal**: `👟 3,200 / 5,000 STEPS`
- **Boss Encounter Beacon**: When node step requirements are fulfilled, the dial pulses with a golden `⚔️ BOSS READY! • TAP APP` alert.
- **Polished Bezel Radial Arc**: The top radial progress sweep arc now features rounded caps (`Paint.Cap.ROUND`) for clean integration on round AMOLED smartwatch displays.
- **Ambient AOD Telemetry**: Ambient mode displays `${steps}st • ADV N${node} • ${batt}%` while adhering to the strict <= 15% on-pixel ratio (OPR) burn-in threshold.
- **Native Bottom Slot Protection**: Natively reserves bottom complication telemetry, preventing external system pedometers from overwriting DigiWear's rich adventure HUD.

---

### 👆 Modal-First Back Swipes & V-Pet Button C
- **Disabled System Swipe-to-Dismiss**: Added `@style/DigiWearTheme` setting `android:windowSwipeToDismiss = false`, allowing full application-level control over edge swipes.
- **Smart Gesture Discrimination**: Detects deliberate rightward back swipes (`dx >= 65px`, `dy < dx * 0.75`, duration 35–750ms) while ignoring vertical list scrolls in Settings and DigiDex. Includes `ACTION_CANCEL` handling for gesture interruption safety.
- **Modal-First Dismissal Priority**: Rightward back swipes dispatch [`VPetButton.C`](file:///c:/dev/DigiWatch/wear-os-app/src/main/kotlin/com/digiwear/wear/input/VPetButton.kt). If an in-screen modal or popup is open (such as DigiDex species detail cards, egg selectors, saga pickers, or battle keypads), it dismisses the popup in-place without abruptly popping the screen.
- **Full Navigation Back-Stack**: Navigating through subscreens (`MAIN` $\rightarrow$ `STATUS` $\rightarrow$ `DIGIDEX`) creates a proper back-stack, returning in reverse order upon swiping or pressing the back key.
- **Authentic Main Screen Button C**: On the `MAIN` screen, back swipes function authentically as the V-Pet Button C, cancelling open menu selections, closing pickers, or toggling lights off/on with piezo buzzer audio feedback.

---

### 🎨 Canonical 550-Species Master Sprite Audit
- **Exhaustive Roster Verification**: Audited all 550 species in `:shared-vpet-core` against official Bandai V-Pet, Pendulum, and Digimon Color references via [`FullRosterCanonAuditTest`](file:///c:/dev/DigiWatch/shared-vpet-core/src/test/kotlin/com/digiwear/core/FullRosterCanonAuditTest.kt).
- **Tertiary Shading Inversion Remediation**: Swapped inverted shading characters (`#` $\leftrightarrow$ `T`) across 65+ species to restore authentic depth and highlights.
- **Handcrafted Canonical Matrices**: Hand-aligned matrices for key species including #332 Flamedramon, #207 Palmon, #307 Angemon, #444 ShogunGekomon, and #570 MarineAngemon.
- **Regenerated Visual Guides**: Updated [`docs/digiwear_roster_spritesheet.png`](docs/digiwear_roster_spritesheet.png) and [`docs/digiwear_evolution_guide_chart.png`](docs/digiwear_evolution_guide_chart.png) with 100% canonical color-accurate pixel matrices.

---

### ⚔️ Live Real-Time Multiplayer Duel Hub & Secure Micro-Relay (M10)
- **Turnkey Micro-Relay Server**: Node.js WebSocket micro-relay server (`relay-server/`) featuring room-based matchmaking, zero-trust fighter stat validation, temporal anti-cheat, and commit-reveal fair RNG.
- **End-to-End Cryptography**: Ephemeral ECDH key exchange with AES-256-GCM authenticated payload encryption.
- **Companion App Live Duel Lobby**: Added dedicated Live Duel screen with local LAN or remote host configuration and real-time room joining.
- **Docker & VPS Deployment Guide**: Comprehensive setup guide in [`relay-server/README.md`](relay-server/README.md) for self-hosting with minimal resource footprint (<20MB RAM).
