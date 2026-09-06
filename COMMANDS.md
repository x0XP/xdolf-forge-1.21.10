# Original commands and keybinds — dev.9

Original source: [Xdolf commands](https://github.com/x0XP/xdolf/tree/main/minecraft/com/x0xp/xdolf/commands).
Commands begin with a dot and are handled locally. Unknown dot commands are also consumed locally.
Press period in the world to open chat with a dot already entered.

| Command | Behaviour |
| --- | --- |
| `.help` | List the original registered command syntax |
| `.toggle <module>` | Toggle a module, accepting original displayed names |
| `.timer <speed>` | Set client Timer speed (enable Timer separately) |
| `.alloff` | Disable enabled modules and report the count |
| `.say <message>` | Explicitly send chat or a slash command |
| `.modlist` | Module names and descriptions |
| `.spam mode <normal/antispam>` | Select plain messages or randomized suffixes |
| `.spam msg <message>` | Set the repeated message |
| `.spam delay <milliseconds>` | Set the original millisecond delay |
| `.rotate <yaw> <pitch>` | Set player rotation |
| `.view <player/off>` | View a loaded player's camera, or return to yours |
| `.bind add <module> <key>` | Bind a module or GUI |
| `.bind del <key>` | Remove the first module binding on that key, like the source |
| `.friend add <name> [alias]` | Add a friend; alias is accepted but unused, like the source |
| `.friend del <name>` / `.friend list` / `.friend clear` | Manage friends |
| `.impersonate <chat/whisper> <name> <message>` | Display a local-only chat line |
| `.xray add/del <block>` | Edit the XRay block selection and rebuild chunks when active |
| `.waypoint add/del <name>` / `.waypoint clear` | Store or remove named coordinates |
| `.praiseore` | Original random-quote command with neutral original entries only |
| `.deathcoords` | Show the most recently recorded death coordinates |
| `.info <player>` | Loaded player's health, distance and enchanted equipment |
| `.music` | Start game music |
| `.hide <logo/mods/potions>` | Toggle original HUD visibility flags |
| `.follow <player>` | Face the player and toggle AutoWalk, as in the original one-shot command |
| `.macro add <key> <command>` / `.macro del <key>` | Bind a dot command; multiple macros on one key all run |
| `.vclip <height>` | Move the player or ridden entity vertically; server-dependent |

Key names translate from LWJGL 2 names to GLFW: letters, digits, function keys,
LSHIFT/RSHIFT, LCONTROL/RCONTROL, LMENU/RMENU, GRAVE, RETURN, navigation keys,
and numpad names. Never copy raw LWJGL 2 numeric key codes into GLFW configuration.
GUI is rebindable. Right Shift remains a convenience alias only while GUI has its
default GRAVE binding. Key repeats and typing in other screens do not toggle modules.

The port aliases `.gui`, `.mods`, `.t`, `.set`, `.bind <module> <key/NONE>`, and
`.friend remove` remain available. `.macro` executes dot commands; use `.say`
inside a macro when you explicitly want it to send something to the server.

## Module state and persistence

World/player changes reset temporary references, timers, camera and held actions,
then resume selected modules when a player/world is available. They no longer
turn every module off. Settings and enabled states save in `config/xdolf.properties`.
Spammer and Freecam remain enabled through a reconnect in the same client session,
but start off after a client relaunch, matching original FileManager.saveHacks.
Modules that deliberately disable themselves (for example AutoLog) still do so.

Macro text, waypoint dimensions/coordinates, spam options and XRay edits persist
in `config/xdolf-commands.properties`; friend names retain their existing file.
Old port settings/keybinds are retained. The old 1.12.2 `Xdolf/*.txt` files are not
automatically imported. Waypoint rendering was commented out in the original
registry; this restores the registered storage commands, not an unregistered module.

## Explicit differences

Modern chat limits, registry IDs and server-authoritative movement still apply.
Inputs are validated instead of reproducing legacy exceptions or malformed files.
The original praiseore array contained personal allegations about a third party;
automatic approval review blocked publishing that array. Only its neutral entries
are included. No server messages are sent during development to outside servers.

The opt-in client test exercises original commands, key dispatch, macros, saved
states and keybinds, then disconnects and creates another integrated-server world
to verify module selections survive a real connection/world/player replacement.
The latest workflow result records whether these checks passed.
