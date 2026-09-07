# Xdolf source architecture

Organisation is a project invariant, not a cosmetic cleanup task. New work should strengthen the ownership boundaries below rather than adding another responsibility to an unrelated file.

## Module ownership

Every user-facing module must have exactly one implementation class and one source file.

The file must live in the folder matching the category shown in the ClickGUI:

- `module/player/` — Player modules
- `module/combat/` — Combat modules
- `module/render/` — Render modules
- `module/world/` — World modules

Do not place a Player module in a technical folder such as `movement/` or `network/`. If multiple modules need the same movement, inventory or protocol logic, place that reusable implementation under `module/support/` and keep the module class in its actual GUI category.

Do not declare modules as anonymous `new ClientModule(...) { ... }` implementations inside a registry, helper or another module. Module registries only instantiate module classes.

## Module infrastructure

- `module/registry/` owns module registration/order and lookup bridges.
- `module/support/` owns reusable module implementation helpers that are not themselves modules.
- `module/ClientModule.java` owns the base module contract.
- `module/ModuleManager.java` owns activation, dependency and conflict policy.

A support class must not silently become a second registry or a container for unrelated module implementations.

## Other source ownership

- `core/` — bootstrap, runtime coordination and global hooks
- `settings/` — typed settings, persistence and keybind configuration
- `chat/` — chat formatting/context/queue behaviour
- `command/` — dot-command parsing and command persistence
- `ui/clickgui/` — ClickGUI interaction and option rendering
- `ui/hud/` — HUD and notification rendering
- `render/` — shared world-render infrastructure
- `social/` — friend/social state
- `dev/` — smoke/development harnesses
- `mixin/` — mixins grouped by target area

## File design rules

1. Prefer one top-level type per file. Closely coupled private/nested implementation records or state objects can remain nested.
2. When a file gains a second independent responsibility, split it before adding more behaviour.
3. Shared behaviour belongs in a clearly named support/service class, not copied between modules.
4. Registries contain registration; renderers contain rendering; persistence classes contain persistence. Avoid convenience dumping grounds.
5. Preserve existing runtime behaviour while reorganising. Structural refactors must pass the normal Forge build and the full client/GUI/world smoke test before they are considered stable.
6. New folders should represent durable ownership boundaries. Do not create a folder for a single temporary implementation detail unless it is expected to own a coherent family of code.

## Java packages

Some existing source files still use the historical `com.x0xp.xdolf` package to preserve package-private contracts during the rebuild. Folder ownership is enforced now; package namespace migration should be performed deliberately as API boundaries are extracted, rather than by exposing internals purely to make a move compile.
