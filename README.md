# SkyBlock Lens

SkyBlock Lens is a modern Fabric client-side quality-of-life mod for Hypixel
SkyBlock, inspired by NotEnoughUpdates while targeting modern Minecraft clients.

This is not a cheat client. No cheats, no macros, no automation, no anti-cheat
bypass, no hidden telemetry, and no unfair advantage features are allowed.

## Target

- Minecraft: `1.21.11`
- Loader: Fabric
- Language: Java 21
- License: LGPL-3.0-or-later

Java was chosen because Fabric's public examples, mappings, and client APIs are
Java-first, which keeps the MVP stable and easier to review.

## Current MVP Features

- Fabric client mod entrypoint.
- JSON config with safe defaults.
- NEU-inspired dark settings screen opened with `/sbl`, `/skyblocklens`, or a
  keybind.
- Settings categories for connected MVP modules: About/Core, Misc, GUI Locations,
  Notifications, Item List, Toolbar, Inventory Buttons, Slot Locking, Quick Swap,
  and Tooltip Tweaks.
- Search, scrollable category list, custom toggles, dropdowns, sliders, color
  pickers, keybind editing, action buttons, reset, true full-menu scale, and
  HUD scale controls.
- Dropdown-style language selector with English and Russian strings loaded from
  language files.
- English and Russian UI strings loaded from language files.
- Basic Hypixel/SkyBlock context detection from client-visible server, tablist,
  and actionbar data.
- `Only on Hypixel SkyBlock` now gates SkyBlock HUD modules, item tooltip
  additions, toolbar buttons, and chat
  filters/highlights/alerts outside SkyBlock. Slot Locking remains active as a
  local safety feature.
- Draggable HUD editor with basic HUD modules.
- NEU-inspired local toolbar in handled inventory screens with search,
  CTRL+F focus, a configurable far-right paged SkyBlock item grid, browser
  toggle, bottom placement, SkyBlock data matching, slot highlighting, and
  nonmatching-slot dimming while inventory search is active. The browser has a
  configurable toggle keybind and hides itself in detected SkyBlock dungeons.
- Local item browser using bundled JSON data derived from the NEU item data
  repo, with alias/lore/id-aware search, visual crafting-grid recipes, reverse
  "used in" lookup, clickable recipe/usage rows, and local recipe history
  backtracking.
- Tooltip additions from local item data plus local pet XP expansion for visible
  abbreviated pet progress lines.
- Chat filters, editable filter/highlight terms, highlights, and local visual
  notifications with no automatic responses.
- Safe inventory item-browser button with an editor for its position and size.
- Slot Locking keybind with stable player-inventory slot persistence, visual
  overlays, and client-side blocking for locked inventory slots, drops, and
  number-key hotbar swaps into locked slots. Locked hotbar slots are marked in
  the normal HUD and protected against the drop key. Lock/unlock sounds, sound volume,
  reset for locally saved locked slots, trade-window locking, and storage-screen
  disable behavior are configurable. Safe Slot Binding metadata, overlay
  preview, optional bound-slot drop protection, and user-initiated Shift+Left
  Click swapping from either side of a linked main-inventory/hotbar pair are
  implemented without macros or automatic item movement loops.
- Misc nameplate controls include nameplate background/text opacity, opt-in
  own-name display for supported camera views, local-only own-name replacement,
  and own-name color. GUI settings also expose scoreboard background color and
  opacity.
- Profile/API skeleton with networking disabled by default.

## Build

```powershell
.\gradlew.bat build
```

The built jar will be in `build/libs/`.

## Run Client

```powershell
.\gradlew.bat runClient
```

Inside Minecraft, use `/sbl` or `/skyblocklens` to open settings.

## Safety

Network access is not used by the MVP. Future network features must be explicit,
disableable, documented, rate-limited, and must never send secrets or personal
account data.

## Data Attribution

Bundled SkyBlock item metadata is transformed from
`NotEnoughUpdates/NotEnoughUpdates-REPO` and kept local in the mod. See
`THIRD_PARTY_NOTICES.md` for the MIT license notice and attribution.

Custom Hypixel player-head textures require item data that includes the modern
head profile/texture value for each SkyBlock internal item id. Without that
field the browser falls back to safe vanilla/category icons instead of guessing.

## NotEnoughUpdates Compatibility Goal

The goal is gradual legitimate feature parity with the safe QoL parts of NEU,
adapted for modern Fabric and Minecraft APIs. Old Forge 1.8.9 code must not be
blindly copied into this project.

The settings catalog should expose a NEU-like control only after the backing
behavior exists and has been checked for config persistence, localization, UI
quality, and crash safety. Planned NEU modules are tracked in
`FEATURE_PARITY.md` until their implementation lands.
