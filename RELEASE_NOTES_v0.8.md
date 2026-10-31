# DigiWear v0.8.0 Release Notes

### 🖼️ Bundled 1:1 Digital World Wallpapers & Preset Gallery
- **4 Built-in HD Wallpapers**: Bundled directly into both the Wear OS and Phone Companion applications with zero external downloads required:
  - **Cyber Metropolis**: Midnight indigo sky, digital crescent moon, binary code clouds, and vintage CRT monitors/server racks with an illuminated cyan circuit walkway.
  - **Gear Savannah**: Nostalgic 90s anime daytime adventure with emerald plains, half-buried weathered brass clockwork cogs, and ancient circuit-carved stone obelisks.
  - **Cyberspace Grid**: Pure AMOLED-optimized deep void with floating holographic data cubes and emerald/cyan 3D perspective wireframe grid.
  - **Magma Core**: Industrial underground vault with turbine ventilation fans, copper heatsinks, and an iron platform suspended over glowing magma circuits.
- **Companion App Preset Picker**: Interactive wallpaper preset cards in Phone Settings $\rightarrow$ Photo Wallpaper with 1-tap wireless beaming to your smartwatch.
- **On-Watch Preset Cycler**: Direct on-wrist wallpaper preset selector in Wear OS Settings when Wallpaper Theme is active.
- **Instant Fallback**: Smartwatch automatically defaults to Cyber Metropolis if no external photo has been imported yet.

### 💩 Faithful Bandai Poop & Waste Mechanics
- **Original Stage Floor Placement**: Shifted waste positioning down to the bottom-right baseline (`DialSlot(r * 0.22f, r * 0.18f)`), matching original Bandai LCD geometry.
- **Natural Walking Overlay**: Waste is drawn before the partner sprite layer, allowing the Digimon to naturally walk over and across waste as it wanders.
- **High-Contrast Silhouette**: Scaled up to 20–28dp with a dark silhouette drop shadow backing and amber-golden body for high visibility over all background wallpapers.

### 🕒 App Clock & High-Contrast Typography
- **12-Hour AM/PM Support**: Complete 12h clock format integration with dynamic `AM`/`PM` indicator badges.
- **Ultra-Bold Monospaced Digital Clock**: Upgraded time display to `FontWeight.Black` monospaced font with high-contrast translucent capsule backing (`Color(0x7A030A10)`).
- **Legibility Drop Shadows**: Hardware-accelerated text drop shadows (`setShadowLayer`) across watch face Date (`THU, OCT 29`), Heart meters (`HGR`/`STR`), and telemetry for crystal clarity against bright or dark backgrounds.

### 🚨 Authentic Passive Call Signal Distress Indicator
- **Decoupled Distress Lamp**: Icon 7 is decoupled from physical [A] button cycling and touch focus, preventing accidental manual triggering.
- **Passive Attention Alert**: Functions authentically as a distress lamp that illuminates when the pet requires care (hunger, injury, bedtime, neglect) and auto-clears upon fulfilling needs.
- **Lights-Out Call Signal Suppression**: Turning lights off immediately extinguishes active call signals and cancels attention notifications; background decay and distress workers strictly suppress calls, buzzers, and vibration alerts while lights are out or partner is resting.

### 👆 Watch Face Gesture Optimization & Documentation
- **Tuned Double-Tap Window**: Relaxed double-tap threshold from 380ms to 500ms for easier app launching from the watch face.
- **In-App Gesture Guide**: Added a dedicated gesture reference card in Watch Settings explaining Double-Tap (Open DigiWear) and Long-Press (Customize Dial).
