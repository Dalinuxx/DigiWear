# DigiWear v0.4.0 Release Notes

### UI polish & fixes
- V-Pet page: removed redundant Watch Settings / Fusion Lab cards (use the top gear and the Vault tab).
- Roster: #1115 was mislabeled "Minomon Color" and is now **Budmon** (with its own signature attack); removed stray " Color" suffixes from Caprimon, Bubbmon, Mochimon, Plotmon.
- D-COM: readable **REROLL SEED** button (re-rolls the 16-bit battle PRNG seed, with toast), NFC READY status pill no longer wraps, MetalGarurumon fits on one line, bit-field table no longer collides.
- Settings: Button Rail Placement now has an **OFF (TOUCH)** option.

### Wireless ADB sideload
- Pairing card now has its own Watch IP field, accepts pasted `IP:Port`, shows the pairing target, and gives actionable guidance for "Could not exchange peer info" (keep the watch screen awake on Pair new device).
