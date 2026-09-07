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

The click GUI uses the original grave/backtick default key. Right-click a module to open its inset option card, and hover modules or settings for descriptions and live status information. The ClickGUI fades in when opened, and panel scrollbars stay clear of the red enabled-module indicator rail. Module expansion indicators use the same bundled TTF renderer as the module labels, and boolean option toggles animate their knob, track and state colours rather than snapping between states. Text, choices, toggles, numbers and keybinds share one typed setting system. Spammer's message, normal/anti-spam mode and delay can be edited there as well as through `.spam`. Module states, keybinds and settings are stored under Minecraft's `config` folder. Normal inventory/chat/settings screens do **not** disable enabled modules.

The enabled-modules HUD list is motion-aware: newly enabled modules slide in from the right, existing entries smoothly reflow when their sorted position changes, and disabled modules slide back out rather than disappearing instantly.

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

See [docs/COMMANDS.md](docs/COMMANDS.md) for the full command reference.

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

## Project structure

The Java source is grouped by responsibility so the repository is easier to navigate. The existing Java package declarations are intentionally retained while the client is actively being rebuilt, preserving package-private contracts without introducing unnecessary API churn.

- `src/main/java/com/x0xp/xdolf/core/` — bootstrap, runtime hooks and core client state
- `src/main/java/com/x0xp/xdolf/module/` — module base/registry code and modules grouped by role
- `src/main/java/com/x0xp/xdolf/settings/` — typed settings, configuration and keybind handling
- `src/main/java/com/x0xp/xdolf/chat/` — shared chat formatting, context and queueing
- `src/main/java/com/x0xp/xdolf/command/` — command handling
- `src/main/java/com/x0xp/xdolf/ui/clickgui/` — ClickGUI panels, options, persistence and tooltips
- `src/main/java/com/x0xp/xdolf/ui/hud/` — HUD and notification rendering
- `src/main/java/com/x0xp/xdolf/render/` — shared world/render infrastructure and visual styling
- `src/main/java/com/x0xp/xdolf/social/` — friend/social state
- `src/main/java/com/x0xp/xdolf/dev/` — development and smoke-test harnesses
- `src/main/java/com/x0xp/xdolf/mixin/` — mixins grouped by target area
- `src/main/resources/` — Forge metadata, mixin configuration and bundled assets
- `src/test/` — standalone style/regression tests
- `docs/` — user/developer documentation
- `.github/` — GitHub Actions build/test workflows
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle wrapper required for reproducible builds
- `build.gradle`, `gradle.properties`, `settings.gradle` — build configuration
- `LICENSE` — project license

## License and attribution

Xdolf is released under **GPL-3.0-only**. Original attribution to x0XP, Sgt Pepper and Xdolf contributors is preserved.

Minecraft Forge is a build/runtime dependency and is not bundled as source in this repository or inside the Xdolf mod JAR. Forge remains licensed by its own authors under its own terms.
