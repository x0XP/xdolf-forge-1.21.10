# Xdolf — AI-assisted Forge 1.21.10 rebuild

**Development build, not certified visually identical.** See [VISUAL_PARITY.md](VISUAL_PARITY.md) for restored visuals, validation and remaining reference gaps.

Development port of Xdolf to Minecraft **1.21.10**, Forge **60.1.0**, and **Java 21**.
An AI-assisted rebuild of the original Xdolf Minecraft client by x0XP and contributors.
Original 1.12.2 source: https://github.com/x0XP/xdolf

This repository contains the standalone Forge mod, with original attribution and GPL-3.0 licensing preserved. AI assistance does not imply full feature or visual parity.

## Install

Install Forge 60.1.0 for Minecraft 1.21.10, launch its profile once, and place
`xdolf-4.0.0-dev.8.jar` in that profile's `mods` folder. Launch with Java 21.
This is a client-only mod. It does not need installation on a server.

Press **grave/backtick** (the original key) or **Right Shift** in a world to open
the original click GUI. Drag a window by its title. The left header square pins it
on the HUD; the right square expands/collapses it. Left-click module names to toggle;
right-click names marked **+** for the original options window. Adjust numeric
settings in **Values**. Use `.bind` for key bindings and `.set` for additional
Forge-port settings. Windows start collapsed at their original positions down the
left edge. Their positions, open states and pins save in `config/xdolf-gui.properties`. All modules start disabled and turn off when changing worlds.
Settings, key bindings, and friends persist in the `config` folder.

Local chat commands:

- `.help`, `.gui`, `.mods`, `.alloff`
- `.toggle Sprint`, `.bind Sprint R`, `.bind Sprint NONE`
- `.set Flight` lists settings; `.set Flight Speed 1` changes a setting.
- `.friend` shows friend commands; friends are excluded from KillAura.
- `.spam` configures Spammer's message; the module must also be enabled.

Opening screens suspends action modules. Visual modules remain active; AutoRespawn
can act on the death screen. Freecam suspends other action modules. Flight,
ElytraFly and ElytraPlus are mutually exclusive. Vanilla key bindings can still
share keys with module bindings.

## Build

From this directory with a Java 21 JDK and internet access:

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Linux/macOS: `bash gradlew clean build` and `bash gradlew runClient`.
JARs are written to `build/libs`. The repository's GitHub Actions workflow builds
and uploads the development JAR and client test logs.

## Scope and verification

All 37 functional entries in the original module registry now have implementations,
with the original draggable click GUI rebuilt for its GUI entry. See [PORTING.md](PORTING.md) for differences.
Compilation and client menu startup have passed GitHub Actions. The workflow also
contains an opt-in singleplayer launch test (`-PxdolfSmokeTest`) that creates a test
world under the development run directory and enables visual modules briefly.
Consult the latest workflow result for that test's outcome.

This remains a development build: individual gameplay behavior, multiplayer server
compatibility, and combinations with other rendering mods need manual testing.
No claim of exact feature or visual parity with the old client is made.

GPL-3.0-only. Original attribution to x0XP, Sgt Pepper, and Xdolf contributors is
preserved. The JAR includes this mod's code and license, not bundled Minecraft code.
