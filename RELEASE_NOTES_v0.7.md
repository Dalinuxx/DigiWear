# DigiWear v0.7.0 Release Notes

### 🧭 Wear OS Adventure & Radar Tile (Milestone 7)
- **Dedicated ProtoLayout Radar Glanceable Tile**: Track live expedition sector progress directly from your Wear OS tile carousel without launching the app. Displays current biome/hazard, step progress toward sector boss, and instant 1-tap shortcut.
- **Dynamic Encounter Beacon**: Pulsing `⚡ BOSS READY ⚡` indicator triggers automatically when step requirements are satisfied, providing direct entry to boss battles.

### 🏃 Health Connect & Workout Engine 2.0 (Milestone 8)
- **Calorie-Driven Evolution Acceleration**: Optional opt-in sync via Android Health Connect accelerates digital evolution growth (+15 minutes growth bonus per 50 active kcal burned).
- **Workout History Room Migration v6**: Expanded database architecture records real-time workout sessions, active heart rate zones, and step velocity with zero data loss.
- **Full Offline Fallback**: All core step-based training functions continue running autonomously when Health Connect permissions are omitted.

### 🗺️ Visual Campaign & Saga Creator (Milestone 9)
- **In-App Visual Node Architect**: Design, customize, and build interactive branching story campaigns directly in the Phone Companion app.
- **550-Monster Boss Roster & Sector Conditions**: Assign custom dialogue, environmental hazards, step targets, and boss encounters across all 550 species.
- **Wireless Watch Sync**: Seamlessly beam custom JSON campaigns to Wear OS via Google Play Services DataLayer.

### ⌚ Adaptive Adventure UI
- **Radial Bezel Quest Arc**: Real-time 360° progress arc on the watch face perimeter reflects active expedition steps with zero battery impact.
- **Environment Particle Weather Engine**: Ambient particle simulations (dust storms, blizzard flakes, digital rain, solar flares) react to active biomes during outdoor expeditions.
- **Expedition 1-Tap Pill**: Clean edge-flush pill on the watch face jumps straight into the current adventure without navigating menus.
- **Optional Toggle**: Full adventure overlay can be toggled on/off in Watch Settings.

### 👾 Roster Audit & Starter Egg Fixes
- **16 Canonical Starter Digi-Eggs**: Added the 6 expansion starter eggs (`Cotsucomon`, `Fufumon`, `Algomon Baby I`, `Dokimon`, `Choromon`, `Bubbmon`) across watch and phone egg pickers, complete with correct lineage origins and evolution rules.
- **Egg Selection ID Bug Fix**: Fixed an issue where expansion starter eggs previously defaulted to Botamon on watch reset.
- **Roster Naming Consistency**: Standardized all 550 species names across all documentation and guides, eliminating obsolete data suffixes (`Budmon`, `Caprimon`, `Plotmon`, `Mochimon`, `Bubbmon`).

### 📦 Build & Distribution Modernization
- **Streamlined Binaries**: Pruned redundant generic Gradle packages, dropping Wear OS release size from 73.8 MB to **38.5 MB** and Phone Companion from 105.7 MB to **70.3 MB**.
- **Automated Gradle Pipeline**: New `syncReleaseBin` task keeps branded release binaries in sync with strict Git tracking rules.
