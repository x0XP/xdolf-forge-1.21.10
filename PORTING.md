# Port coverage

The dev.8 visual restoration compiles and runs. See [VISUAL_PARITY.md](VISUAL_PARITY.md) for exact reference coverage and remaining verification.

Source reference: original commit `6d0de4589cc8475380113aee0bdaa2ab4280feef`.
The original registry has 38 entries: 37 functional modules and GUI.
All functional entries have implementations compiled against Forge 60.1.0 / Minecraft 1.21.10.
This table describes implemented behavior, not exhaustive runtime certification.

| Modules | Port behavior |
| --- | --- |
| Sprint, AutoWalk | Automatic sprint and forward movement |
| AutoRespawn, AutoLog, CrystalLog | Respawn and health/crystal proximity disconnects |
| Flight, ElytraFly, ElytraPlus | Controlled flight and elytra movement; mutually exclusive |
| EntitySpeed, EntityStep, HorseJump | Ridden entity movement, step height and charged jump |
| NoFall, AntiHunger, Criticals | Modern packet implementations; server-dependent effectiveness |
| AntiVelocity | Local velocity suppression and explosion knockback filtering |
| Jesus, SafeWalk | Fluid movement/collision and edge protection |
| NoSlowdown | Reduced ice slipping, following the original module's behavior |
| Timer | Client tick target adjustment |
| FastPlace, Speedmine | Placement delay and mining progress adjustments |
| AutoArmor, AutoEat | Modern equipment/data-component and inventory handling |
| AutoFish | Own-bobber splash detection and scheduled reel/recast |
| KillAura, CrystalAura | Nearby visible target attacks; KillAura respects friends/teams |
| Spammer | Configured repeated chat message, disabled by default |
| Freecam | Detached camera with body input and other action modules suspended |
| Fullbright, NoHurtCam | Local lighting and hurt-camera hooks |
| EntityESP | Vanilla outlines or original-colour translucent world boxes |
| Chams | Textured player rendering with depth testing disabled; missing original render reference |
| XRay | Ore/storage block filtering, exposed faces and chunk visibility hooks |
| StorageESP, Tracers, Nametags | World-space boxes, original-distance tracer colours, and original-font player health labels |
| Trajectories | Held-projectile block-hit prediction |
| GUI | Original seven draggable windows, module order/labels, 13 Values sliders, right-click options, pinning, Info/Radar and saved window state |

## State and differences

Settings and bindings persist in `config/xdolf.properties`; friends in
`config/xdolf-friends.txt`. Enabled states intentionally do not persist.

The click GUI now follows `clickgui/XdolfGuiClick`, `elements` and `windows`:
100-unit windows, half-unit borders, translucent black panels/background, red
controls, original row/slider spacing, hover colours and AWT typography. It uses
the original `new Font("Roboto", PLAIN, 36)` lookup and quarter-scale glyph rendering;
the operating system's fallback font is used if Roboto is absent, as in the source.
The original layout starts collapsed. Numeric controls restore the original labels, units, ranges and default values.
Crystal Speed uses attacks per second, Mine Speed sets minimum mining progress,
and fishing delays use seconds. Existing saved values remain where valid.

Macros, waypoint system, protocol switching, and bundled OptiFine/shader client
are not reproduced. Waypoints and AutoTotem were
commented out in the old registry and are not implemented here. The complete old
command set is not carried over; `.help` describes the supported local commands.

The visual rewrite uses world-space rendering through Forge frame passes.
Trajectories preserve the original block-ray prediction; they do not predict all entity collisions. Server authority can
reject movement, timing, mining, inventory or packet behavior. Other rendering mods
may conflict with mixins; compatibility is not established.

## Verification

GitHub Actions has compiled the complete module set and launched/rendered its menu.
The workflow additionally runs an opt-in singleplayer test with visual modules
active, uploads its log, and fails if the success marker is absent. This is a smoke
test, not proof that every rendering hook or gameplay module behaves correctly.
Before treating this as a stable release, manually test module toggles, settings,
key persistence, inventory restoration, movement, death/disconnect, friend filtering,
and multiplayer behavior on the intended server.
