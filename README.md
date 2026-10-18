# DesyncGuard

Client-side Fabric mod for Minecraft 1.20.4. Tracks inventory corrections, rejected block breaks, player position corrections, ping and estimated server TPS. Press **H** to open the session summary.

## What it can do

- Detect inventory corrections after a click and highlight corrected slots.
- Detect when the server restores a block after a predicted break.
- Track server position corrections, ping spikes and estimated TPS.
- Compare TPS, ping variation and client FPS to show likely client, server or connection slowdowns.
- Show alerts on the HUD. Set `hudEnabled` to `false` in `config/desyncguard.json` to hide them.
- Record session statistics and optionally write position corrections to `desyncguard_session.log`.

The diagnosis is an estimate, not proof of fault. Ping measures round-trip latency, not bandwidth; network jitter and delayed TPS packets can affect the estimate. Minecraft servers control inventory, block and player state. DesyncGuard leaves incoming server corrections intact so Minecraft can resynchronize; it cannot prevent server lag, force a server to accept an action, or eliminate rubber-banding caused by the server or network.

## Build

```text
.\gradlew.bat build
```

The JAR is in `build/libs/`.

## Install

Install Fabric Loader and Fabric API for Minecraft 1.20.4, then place the JAR in the `mods` folder.

## License

CC0-1.0
