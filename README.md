# Xdolf for Forge 1.21.10

AI-assisted rebuild of the original Xdolf Minecraft client for **Minecraft 1.21.10**, **Forge 60.1.0**, and **Java 21**.

This project ports the original Xdolf behaviour, GUI, commands and visuals to the modern Minecraft renderer and client APIs while keeping the project client-only.

Original Xdolf source: https://github.com/x0XP/xdolf

> Development build. Behaviour and visuals are actively being tested and refined.

## Install

1. Install Forge 60.1.0 for Minecraft 1.21.10 and launch it once.
2. Put the latest `xdolf-4.0.0-dev.*.jar` into the profile's `mods` folder.
3. Launch Minecraft with Java 21.

Xdolf is client-only and does not need to be installed on the server.

The click GUI uses the original grave/backtick default key. Right-click a module to open its inset option card, and hover modules or settings for descriptions and live status information. Text, choices, toggles, numbers and keybinds share one typed setting system. Spammer's message, normal/anti-spam mode and delay can be edited there as well as through `.spam`. Module states, keybinds and settings are stored under Minecraft's `config` folder. Normal inventory/chat/settings screens do **not** disable enabled modules.

Announcer batches ordinary activity into a single rate-limited chat message. Walking distance, mined block types, eaten items, jumps and attacks can each be enabled independently, and its default delay is 1800 ms.

## Commands

Xdolf uses the original `.` command prefix. Examples:

- `.help`
- `.toggle Fullbright`
- `.bind add AutoSprint R`
- `.macro add F8 .toggle Fullbright`
- `.set Flight Speed 1`
- `.friend add <name>`
- `.waypoint add <name>`
- `.xray add minecraft:diamond_ore`

See [COMMANDS.md](COMMANDS.md) for the full command reference.

## Current build

The active client includes the original-style click GUI and restored gameplay/render modules, including Freecam, Tracers, Nametags, XRay, Waypoints, LogoutSpot and AutoTotem. Modern compatibility code is kept internal rather than exposed as separate user-facing features.

## Build

Requires a Java 21 JDK.

Windows:

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Linux/macOS:

```bash
bash gradlew clean build
bash gradlew runClient
```

Built JARs are written to `build/libs`. GitHub Actions also compiles the project and runs client/smoke validation for development commits.

## Project files

- `src/` — Xdolf source and tests
- `.github/` — GitHub Actions build/test workflow
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle wrapper required for reproducible builds
- `build.gradle`, `gradle.properties`, `settings.gradle` — build configuration
- `COMMANDS.md` — command reference
- `LICENSE` — project license

## License and attribution

Xdolf is released under **GPL-3.0-only**. Original attribution to x0XP, Sgt Pepper and Xdolf contributors is preserved.

Minecraft Forge is a build/runtime dependency and is not bundled as source in this repository or inside the Xdolf mod JAR. Forge remains licensed by its own authors under its own terms.
