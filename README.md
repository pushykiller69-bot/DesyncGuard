<div align="center">
  <h1>🛡️ DesyncGuard</h1>
  <p><b>A lightweight, 100% client-side Fabric mod that detects common Minecraft desync problems and shows them on a HUD.</b></p>

  [![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-15803d?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
  [![Fabric](https://img.shields.io/badge/Loader-Fabric-0369a1?style=for-the-badge&logo=gitbook&logoColor=white)](https://fabricmc.net/)
  [![License](https://img.shields.io/badge/License-CC0--1.0-4b5563?style=for-the-badge)](LICENSE)
  [![Side](https://img.shields.io/badge/Side-Client--Only-ea580c?style=for-the-badge)]()
</div>

---

## 🔍 The Problem

Every Minecraft player knows these frustrating synchronization bugs that have existed in the game client for years:

*   **👻 Ghost Items (`MC-179845`)** — Moving items in your inventory on a laggy server causes them to snap back or disappear when clicked.
*   **📦 Unpickable Drops (`MC-4`)** — Items on the ground drift client-side, showing up in one position while the server thinks they are elsewhere.
*   **⚡ Rubber-Banding** — Walking forward only to be suddenly teleported back to where you were 2 seconds ago, often getting stuck in blocks.

Existing utilities like *AntiGhost* only provide manual command/keybind triggers. Nothing detects, alerts, and mitigates these issues **automatically** and **silently in the background**—until now.

---

## ✨ Features & Mitigations

| Feature | Visual | How it Works |
| :--- | :---: | :--- |
| **Inventory Desync Detector** | 🔴 | The server only sends slot data when it disagrees with your click. If it sends a stack that differs from what you hold shortly after a click, that slot is highlighted red (heatmap: more frequent = darker). |
| **Item Drop Desync Corrector** | 📦 | If your player hitbox touches a dropped item for **>1 second** without picking it up, DesyncGuard zeroes its client-side velocity to stop it drifting away. |
| **Rubber-Band Detection** | ⚡ | Detects server position corrections larger than `rbThreshold` (default 1.35 blocks) and logs the coordinates. Join/respawn syncs and long teleports (> `rbMaxBlocks`, default 12) are ignored. |
| **Ghost Block Detector** | 👻 | Flags a block you just broke that the server restores. |
| **TPS / Ping Spike HUD** | 📶 | TPS estimate from time packets, connection spike warning from latency. Press **H** for the session summary. |
| **Desync Warning HUD** | 🖥️ | A clean, minimalist overlay in the top-left of your screen with smooth fade-out animations. Shows a **Red warning** for rubber-banding and an **Orange warning** for inventory desync. |

---

## 🚀 Installation

1. Install the [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **1.20.4**.
2. Install the [Fabric API](https://modrinth.com/mod/fabric-api).
3. Drop `DesyncGuard-1.0.0.jar` into your `.minecraft/mods/` directory.
4. Launch the game!

---

## 🛠️ Building from Source

To compile and package the mod yourself:

```bash
# Clone the repository
git clone https://github.com/pushykiller69-bot/DesyncGuard.git
cd DesyncGuard

# Build the JAR file
./gradlew build
```
The compiled JAR will be saved in `build/libs/`.

---

## 💬 FAQ

#### Is this safe to use on public servers?
**Yes.** DesyncGuard is 100% client-side. It only reads/monitors network packets, does not send any extra packets, does not automate gameplay, and does not give you an unfair advantage. It is purely a visualization and sanity-check tool.

#### Does it work in singleplayer?
**Yes.** Singleplayer runs an internal server behind the scenes, so desyncs can still occur (especially on heavier modpacks or slower hardware).

#### Will it conflict with other mods?
DesyncGuard is designed to be lightweight. It uses highly targeted, non-intrusive Mixins to maximize compatibility with other mods.

---

## Known Issues
- Estimated TPS can bounce slightly during dimension changes or initial world load before the time packet buffer stabilizes.
- The history screen uses a simple text layout.

## 📄 License

This project is licensed under the **CC0-1.0 License** - see the [LICENSE](LICENSE) file for details.