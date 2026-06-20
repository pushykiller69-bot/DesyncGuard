# DesyncGuard

**A lightweight, 100% client-side Fabric mod that detects and mitigates common Minecraft desync bugs.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey.svg)](LICENSE)
[![Client-Side](https://img.shields.io/badge/Side-Client%20Only-orange.svg)]()

---

## The Problem

Every Minecraft player knows these frustrations:

- **Ghost Items** — You move an item in your inventory on a laggy server, but it snaps back or disappears when you click it (MC-179845).
- **Unpickable Drops** — Items on the ground appear to be at one position, but you can't pick them up because the server thinks they're somewhere else (MC-4).
- **Rubber-Banding** — You walk forward but suddenly teleport back to where you were 2 seconds ago, often getting stuck inside blocks.

These bugs have existed for **years** and Mojang has not fixed them. Existing mods like AntiGhost only provide manual commands — nothing detects and alerts you **automatically**.

## The Solution

**DesyncGuard** runs silently in the background and provides:

### 🔴 Inventory Ghost Item Watchdog
When you click a slot in your inventory, DesyncGuard starts a timer. If the server doesn't confirm the action within 500ms, the slot is highlighted with a **red overlay** to warn you it's a ghost item.

### 📦 Item Drop Desync Corrector
If your player's hitbox is touching a dropped item for more than 1 second without picking it up, DesyncGuard detects the MC-4 position desync and zeroes out the item's client-side velocity to prevent further drift.

### ⚡ Rubber-Band Detection
When the server forcefully corrects your position by more than 1.5 blocks, DesyncGuard detects the rubber-band event and logs it with exact coordinates.

### 🖥️ Desync Warning HUD
A clean, non-intrusive HUD overlay appears in the top-left corner with smooth fade-out animations whenever a desync event is detected:
- **Red warning** for rubber-banding
- **Orange warning** for inventory desyncs

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.20.4
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop `DesyncGuard-1.0.0.jar` into your `.minecraft/mods/` folder
4. Launch the game!

## FAQ

**Is this safe to use on servers?**
Yes. DesyncGuard is 100% client-side and only reads/monitors network packets. It does not send any extra packets, automate any actions, or modify gameplay. It is purely an information and visualization tool.

**Does it work in singleplayer?**
Yes. Even singleplayer runs an internal server, so desyncs can occur (especially on slower hardware or with heavy modpacks).

**Will it conflict with other mods?**
DesyncGuard is designed to be extremely lightweight and compatible. It uses targeted Mixins that should not conflict with other mods.

## Building from Source

```bash
git clone https://github.com/desyncguard/DesyncGuard.git
cd DesyncGuard
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Contributing

Contributions are welcome! If you find a new desync bug that could be detected client-side, please open an issue or PR.

## License

This project is licensed under the CC0-1.0 License - see the [LICENSE](LICENSE) file for details.
