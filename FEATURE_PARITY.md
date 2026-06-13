# Feature Parity With NotEnoughUpdates

SkyBlock Lens uses NotEnoughUpdates as a feature and UX reference, not as a
source tree to copy blindly. Modern Minecraft/Fabric APIs are different from
Forge 1.8.9, so every feature must be adapted and reviewed for Hypixel safety.

## Implemented In MVP

- NEU-inspired settings menu with a scrollable category list, search, custom
  dark rows, animated toggles, dropdowns, sliders, color pickers, keybind
  editing, reset button, true full-menu scale modes, and HUD scale/editor
  controls.
- Visible settings are limited to modules with real connected behavior in this
  branch: About/Core, GUI Locations, Notifications, Item List, Toolbar, Slot
  Locking, and Tooltip Tweaks. Planned NEU categories are tracked below instead
  of being exposed as fake toggles.
- English and Russian language files.
- Basic HUD modules with draggable positions.
- NEU-inspired Toolbar subset for handled screens: local search bar, fixed
  far-right paged SkyBlock item grid, browser toggle, CTRL+F focus, inventory
  search highlighting, auto search timeout, bottom placement, SkyBlock data
  matching, and editor-configurable search box size.
- Safe Hypixel/SkyBlock context detection from client-visible signals.
- Central `Only on Hypixel SkyBlock` guard for SkyBlock HUD modules, item
  tooltip additions, toolbar buttons, and chat
  filters/highlights/alerts outside SkyBlock. Slot Locking intentionally remains
  active as a local safety feature.
- Local item browser with bundled SkyBlock item data, alias-aware search,
  visual crafting-grid recipe display, reverse "used in" lookup, clickable
  recipe/usage navigation, and local recipe history backtracking.
- Item tooltip additions from local data, with separate controls for internal
  id, rarity, category, recipe, missing-recipe lines, and the NEU
  `Expand Pet Exp Requirement` tooltip tweak for visible abbreviated pet XP
  progress lines.
- Chat filters with an in-game term editor, editable chat highlight terms, and
  local visual notifications for visible rare drop, special zealot, slayer
  completion, dungeon score, and market messages.
- Slot locking with a registered keybind, stable player-inventory slot
  persistence across handled screens, locked-slot overlay, click/drop blocking,
  selected-hotbar drop blocking in normal HUD, hotbar overlays, number-key
  hotbar swap blocking in handled inventory screens, configurable lock/unlock
  sound and volume, a reset action for locally saved locks,
  trade-window lock blocking, storage-screen disable behavior, safe Slot Binding
  creation/preview overlays, persisted binding metadata, and `Don't Drop Bound
  Slots` protection.
- Former inventory button code is archived for possible future rework, but no
  inventory buttons are shown in gameplay.
- Profile/API skeleton with networking disabled.

## Implement Later

- NEU feature-surface expansion for Item Overlays, Skill Overlays, Todo
  Overlays, Slayer Overlay, Storage GUI, Dungeons, Enchanting, Mining, Fishing,
  Garden, Improved SkyBlock Menus, Equipment HUD, Calendar, Trade Menu, Pet
  Overlay, AH/Bazaar Tweaks, Recipe Tweaks, Price Graph, Wardrobe Keybinds,
  Accessory Bag, Museum, Profile Viewer, Minion Helper, and APIs.
- Remaining Toolbar parity: QuickCommands, configurable click type, calculation
  clipboard behavior, search history/autocomplete, and command-backed actions.
  These must be reimplemented safely rather than sending commands implicitly.
- Richer local item metadata for prices, obtain methods, and exact custom icons
  where the bundled static repo does not provide enough detail.
- Dedicated Recipe Tweaks surface: recipe tree pages, missing-ingredient
  highlighting, and compact recipe layouts.
- Remaining Tooltip Tweaks parity: exact pet NBT level calculation, price
  information, custom/scrollable tooltip rendering, tooltip borders, reforge and
  gemstone stat cleanup, enchant highlighting, missing enchant lists, RNG meter
  hints, and garden visitor tooltip helpers.
- Auction and Bazaar views with explicit opt-in API settings.
- Profile viewer backed by transparent, disableable API integration.
- Storage GUI.
- Inventory command buttons that only open safe client-side screens, if they are
  reintroduced as an explicit opt-in feature.
- Slot Binding automatic bound-slot item swap behavior from NEU. This must be
  reviewed separately because the original sends window-click actions.
- Calendar, timers, minion helper, accessory bag helpers, and richer overlays.

## Requires Modern Adaptation

- Dungeon map and dungeon overlays.
- Custom SkyBlock menus.
- Fishing, mining, farming, garden, slayer, and combat overlays.
- Waypoints and visual helpers.
- Texture/resource-pack style replacements.
- Item rarity and enchantment visual customization.

## Forbidden / Not Allowed

- Macros, autoclickers, auto-farming, auto-mining, auto-fishing.
- Automated dungeon solvers that perform actions.
- Automatic terminal completion.
- Packet abuse, ghost blocks, anti-knockback, reach, aim assist, triggerbot,
  kill aura, and movement hacks.
- Anti-cheat bypass, hidden behavior, or server exploit helpers.
- Any feature similar to cheat clients such as Oringo.

## Reference

Original Moulberry release page: <https://github.com/Moulberry/NotEnoughUpdates/releases>

Current maintained-release mirror/baseline:
<https://github.com/NotEnoughUpdates/NotEnoughUpdates/releases/tag/2.6.0>

Current NEU comparison baseline: NotEnoughUpdates `2.6.0` release
(`9b1fcfe`, released 2025-06-16; latest release checked 2026-06-11). Continue
comparing feature-by-feature against that release before exposing new settings.

NEU is licensed under LGPL-3.0-or-later. Static item data from
`NotEnoughUpdates-REPO` is MIT-licensed and attributed in
`THIRD_PARTY_NOTICES.md`. If code, assets, JSON data, or implementation logic are
copied or adapted, preserve license notices and update notices.
