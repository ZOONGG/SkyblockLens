# Iteration Log

## 2026-06-11 - Only On SkyBlock Guard Pass

### What Was Broken

- `Only on Hypixel SkyBlock` was visible in About/Core, but most connected
  SkyBlock features did not respect it.
- The setting mainly affected HUD rendering, while tooltips, toolbar buttons,
  inventory item buttons, chat filters, chat highlights, and notification
  alerts could still run outside SkyBlock.

### What Improved

- Added one central guard for SkyBlock feature surfaces.
- HUD modules, tooltip additions, handled-screen toolbar, inventory item-browser
  button, chat filters/highlights, and visual notification rendering now stop
  when the player is outside Hypixel SkyBlock and the setting is enabled.
- Slot Locking intentionally remains active as a local safety feature, so
  protected slots do not suddenly become droppable due to context detection.
- Updated English and Russian descriptions to match the real behavior.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, enable `Only on Hypixel SkyBlock`, and leave a detected SkyBlock
  context.
- Verify HUD modules, item tooltip additions, toolbar buttons, inventory item
  buttons, chat filters/highlights, and visual chat alerts do not run outside
  SkyBlock.
- Verify Slot Locking still protects locked slots outside SkyBlock.
- Disable `Only on Hypixel SkyBlock` and verify the same SkyBlock feature
  surfaces become visible/active again.

### Remaining Note

- The guard depends on the current client-visible SkyBlock detector. Improving
  detection coverage is a separate parity pass.

## 2026-06-11 - Editable Chat Filter And Highlight Terms

### What Was Broken

- `Chat Filters` was visible in Notifications, but the only default blocked
  value was a placeholder `example-blocked-term`.
- Filter and highlight term lists existed in config, but there was no in-game UI
  to edit them, so the settings felt fake unless the user edited JSON manually.

### What Improved

- Added `Edit Filter Terms` and `Edit Highlight Terms` actions under
  Notifications.
- Added a local editor screen with input, add, remove, clear, scroll, back, and
  immediate config saving.
- Removed the placeholder blocked term from defaults and config normalization.
- Chat filtering and highlighting now use user-controlled lists from the UI.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Notifications, and verify the edit actions are visible in
  English and Russian.
- Add a filter term, enable `Chat Filters`, then verify matching visible chat
  lines are hidden.
- Add a highlight term, enable `Chat Highlights`, then verify matching visible
  chat lines are highlighted with the SBL prefix.
- Close and reopen the game/config screen and verify the terms persist.

### Next Logical Parity Step

- Continue removing fake-feeling behavior by adding real editors or disabling
  unimplemented controls before broadening to another NEU category.

## 2026-06-11 - Slot Locking And Toolbar Usability Fixes

### What Was Broken

- Locked-slot overlays in handled screens were drawn with `screenX/screenY`
  added twice, so the marker could appear far away from the real slot.
- A locked hotbar slot was not protected when the normal in-game HUD was open,
  so pressing the drop key could still call Minecraft's `dropSelectedItem`.
- The handled-screen toolbar search sat above inventories, which made it cover
  content, and text input could fail on handled screens.
- Simple settings pages showed a redundant group header such as `Core` before
  the actual controls.

### What Improved

- Handled-screen slot overlays now render in the same local slot coordinate
  space Minecraft uses for `drawSlot`.
- The normal HUD now draws lock/binding overlays directly on hotbar slots.
- Dropping the selected locked hotbar slot is blocked before Minecraft performs
  the drop action.
- Toolbar search is now positioned below the inventory screen, has a key-input
  fallback for typed characters, and matches local SkyBlock item names/aliases
  where bundled item data exists.
- Single-group settings pages now start directly with controls instead of a
  decorative group title row.
- Small menu scale is more compact and large scale is less likely to overflow
  normal screens.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Run `.\gradlew.bat runClient --no-daemon --console=plain --stacktrace`.
- Lock an inventory slot and a hotbar slot; verify the overlay sits on the real
  slot in inventory screens and on the real hotbar slot in normal HUD.
- Select a locked hotbar slot and press the drop key; verify the item does not
  drop and the local locked-slot warning appears.
- Open a handled inventory, click the toolbar search field below the GUI, type
  a query such as `aote` for a bundled SkyBlock item, and verify matching slots
  highlight when present.
- Open `/sbl` and verify simple pages start with settings rows rather than a
  `Core`/group header row.

### Remaining Note

- The bundled SkyBlock item database is still small. Alias-aware search now
  uses it correctly, but full Hypixel SkyBlock item coverage needs a separate
  data import/parity pass with license attribution.

## 2026-06-11 - Handled Screen Startup Crash Fix

### What Was Broken

- `runClient` crashed while Minecraft was starting because the
  `HandledScreenMixin` tried to inject into `HandledScreen#keyReleased`.
- In Minecraft 1.21.11 that method is inherited from `Screen` rather than
  declared on `HandledScreen`, so Mixin could not find the target and aborted
  class transformation.

### What Improved

- Replaced the fragile `@Inject(method = "keyReleased")` hook with a normal
  `Screen#keyReleased(KeyInput)` override in the mixin class.
- Slot Locking still receives key-release events, but `HandledScreen` no longer
  fails runtime transformation.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Run `.\gradlew.bat runClient --no-daemon --console=plain --stacktrace`.
- Verify the log reaches `SkyBlock Lens initialized with 14 local items.` and
  does not contain `Mixin apply ... keyReleased`.

### Remaining Note

- The dev client may still log Realms/authentication errors for Fabric's
  development user. Those are unrelated to this mod startup crash.

## 2026-06-11 - Tooltip Pet EXP Expansion

### What Was Missing

- NEU 2.6.0 exposes `Expand Pet Exp Requirement` in Tooltip Tweaks.
- SkyBlock Lens had local item metadata tooltip controls, but no pet XP
  expansion setting or behavior.

### What Improved

- Added a real `Expand Pet Exp Requirement` toggle to Tooltip Tweaks.
- Pet tooltips are detected from visible pet markers such as `[Lvl ...]` and
  SkyBlock pet category lines.
- When a visible pet XP progress line uses abbreviated numbers such as
  `1.2M/3.4M`, SkyBlock Lens adds a localized full-number XP line underneath.
- The feature uses only the already visible tooltip text. It does not call APIs,
  inspect profiles, send commands, click slots, or move items.

### Safety Boundary

- NEU 1.8.9 calculates exact pet XP from pet NBT and pet-level data. This first
  1.21 adaptation intentionally limits itself to expanding abbreviated values
  already present in the tooltip, so it cannot expose hidden data or depend on
  fragile legacy NBT parsing.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Tooltip Tweaks, and verify `Expand Pet Exp Requirement`
  exists in English and Russian.
- Enable the setting and hover a pet tooltip that shows abbreviated XP progress,
  then verify a localized full-number XP line appears once below the progress
  line.
- Disable the setting and verify the extra line is not added.

### Next Logical Parity Step

- Continue Tooltip Tweaks with another safe local-only item tooltip option, or
  design an explicit, documented data path before attempting price/API-backed
  tooltip features.

## 2026-06-11 - Safe Slot Binding Metadata And Preview

### What Was Missing

- NEU 2.6.0 exposes `Enable Slot Binding` and `Don't Drop Bound Slots` in Slot
  Locking.
- SkyBlock Lens had lock persistence and blocking, but no way to create or show
  inventory-to-hotbar bindings.

### What Improved

- Added the NEU Slot Binding toggles with matching default states.
- Slot Binding can now store local `player_inventory:X->player_inventory:Y`
  relationships in config.
- Holding the Slot Lock key on a normal inventory slot and hovering a hotbar
  slot creates a local binding, shows a cyan preview line, and marks bound
  slots with a cyan overlay.
- `Don't Drop Bound Slots` now makes bound slots participate in drop/click
  blocking the same way NEU's safety option does.
- Reset Locked Slots now clears both locked slots and local slot bindings,
  matching NEU's reset of the combined SlotLockData object.

### Safety Boundary

- This iteration does not port NEU's automatic bound-slot window-click swap.
  The original implementation sends a window-click action after a click on a
  bound slot, so that behavior needs separate review before it can be adapted
  safely to Fabric 1.21.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Slot Locking, and verify `Enable Slot Binding` and
  `Don't Drop Bound Slots` exist in English and Russian.
- Hold the Slot Lock key over a normal player inventory slot, move the cursor
  to hotbar slot 1-8, and verify a cyan line/overlay appears and persists after
  reopening the inventory.
- Enable `Don't Drop Bound Slots`, try dropping or clicking a bound slot, and
  verify the local locked-slot warning blocks it.
- Press `Reset Locked Slots` and verify both locked and bound overlays disappear.

### Next Logical Parity Step

- Add focused tests or a guarded implementation for NEU's bound-slot swap after
  confirming the exact one-input-to-one-action behavior is safe on 1.21.

## 2026-06-11 - Slot Locking Trade And Storage Guards

### What Was Missing

- NEU 2.6.0 exposes `Lock Slots in Trade` and `Disable Locking in Storage` in
  the Slot Locking section.
- SkyBlock Lens already blocked locked player slots broadly, but it did not
  expose those NEU controls or adapt behavior based on trade/storage screens.

### What Improved

- Slot Locking now receives the current handled-screen title from the mixin.
- `Lock Slots in Trade` controls whether locked player slots block clicks,
  drop-key handling, and overlays in detected trade windows.
- `Disable Locking in Storage` disables lock-key handling, locked-slot blocking,
  and locked-slot overlays in detected Storage and Backpack screens.
- Detection is local and title-based. It does not send commands, click slots for
  the player, read account data, or perform any server automation.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Slot Locking, and verify `Lock Slots in Trade` and
  `Disable Locking in Storage` are visible in English and Russian.
- In a detected Storage or Backpack screen, enable `Disable Locking in Storage`
  and verify the Slot Locking key reports that locking is disabled there.
- In a detected trade window, toggle `Lock Slots in Trade` and verify locked
  slots only block movement while the setting is enabled.

### Next Logical Parity Step

- Implement actual slot binding only after the modern interaction model is
  mapped carefully, because NEU binding can move items between slots and must
  not become automation or a packet-abuse path in 1.21.

## 2026-06-11 - Slot Locking Sound And Reset

### What Was Missing

- NEU 2.6.0 exposes `Slot Lock Sound`, `Slot Lock Sound Vol.`, and `Resets
  Locked Slots` in the Slot Locking section.
- SkyBlock Lens already had the keybind, overlay, persistence, drop blocking,
  click blocking, and number-key hotbar swap blocking, but those three controls
  were still absent.

### What Improved

- Slot Locking now has a local sound toggle and 0-100 volume slider.
- Locking and unlocking a slot plays a configurable local ding when enabled.
- The Settings screen now has a Reset Locked Slots action that clears persisted
  `lockedSlots`, saves config immediately, and reports the result locally.
- The reset behavior is implemented directly client-side instead of running
  NEU's old `/neuresetslotlocking` command.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Slot Locking, and verify `Slot Lock Sound`, `Slot Lock
  Sound Vol.`, and `Reset Locked Slots` are visible in English and Russian.
- Lock and unlock a player inventory slot with the configured key and verify the
  sound follows the toggle and volume slider.
- Press Reset Locked Slots and verify the overlay disappears from previously
  locked slots after reopening a handled inventory screen.

### Next Logical Parity Step

- Continue Slot Locking parity with storage/trade-specific behavior only after
  those modern Fabric screens are represented safely in SkyBlock Lens.

## 2026-06-11 - Settings Scale And Slot Locking Reliability

### What Was Broken

- The Settings menu `small`, `medium`, and `large` modes only nudged panel
  dimensions. Text, controls, hitboxes, and spacing stayed almost the same, so
  the setting did not feel real.
- Slot Locking blocked direct clicks and drops, but it did not block the NEU
  parity case where a player presses number keys 1-9 to swap an item into a
  locked hotbar slot.

### What Improved

- Settings now render inside a scaled UI coordinate space. The whole menu,
  text, controls, search box, scroll regions, and hitboxes scale together.
- Small scale now uses a clearly compact `0.68x` UI, medium remains `1.0x`,
  and large uses `1.26x`.
- Mouse clicks, drags, and scroll checks are converted into scaled UI
  coordinates before hitbox handling.
- Slot Locking now blocks `SlotActionType.SWAP` when the target hotbar index is
  locked, matching the NEU behavior that prevents number-key bypasses.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, set Menu Scale to `Small`, `Medium`, and `Large`, and verify the
  whole menu visibly changes size while buttons, dropdowns, sliders, search,
  and scrolling still hit the correct controls.
- Lock a hotbar slot, open a handled inventory/container screen, hover another
  item, press that hotbar number key, and verify the swap is blocked with the
  local locked-slot warning.

### Next Logical Parity Step

- Add the remaining safe Slot Locking options from NEU one by one: reset locked
  slots, optional lock/unlock sound, and storage/trade-specific behavior after
  those screens exist in SkyBlock Lens.

## 2026-06-11 - Item Browser Recipe Navigation

### What Was Missing

- NEU's recipe GUI lets players move between related recipe items and return
  through recipe history.
- SkyBlock Lens showed recipe and "Used In" data, but those rows were static
  text. The player had to manually search for every ingredient or output.

### What Improved

- Recipe ingredient rows now become clickable when bundled `recipeItems` data
  links them to another local item.
- "Used In" rows now jump directly to the linked local item.
- The detail pane now has local history backtracking via a Back button and
  Backspace when the search field is not focused.
- No network requests, commands, slot clicks, or server actions are used.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Item List, open Item Browser, select `Aspect of the End`,
  and click `Enchanted Eye of Ender` or `Enchanted Diamond` in its recipe.
- Click `Back` or press Backspace while the search field is not focused and
  verify the previous item detail returns.
- Select `Enchanted Diamond` and click an entry under `Used In` to verify
  reverse recipe navigation.

### Next Logical Parity Step

- Expand the local item database and add a dedicated Recipe Tweaks page only
  once recipe tree, missing ingredient, compact layout, and favorite-recipe
  behavior are implemented.

## 2026-06-11 - NEU Toolbar Subset

### What Was Missing

- NEU has a toolbar surface with an item search bar, quick settings/search
  controls, CTRL+F focus behavior, and inventory search mode.
- SkyBlock Lens only had a single inventory item-browser button, so Toolbar was
  still a parity gap rather than a working module.

### What Improved

- Added a local handled-screen toolbar with a search field, settings shortcut,
  item-browser shortcut, inventory search toggle, clear button, and CTRL+F
  focus.
- Inventory search mode now highlights matching slots and dims non-matching
  slots by visible item name. It does not send commands or click slots.
- Added Toolbar settings for visibility, buttons, CTRL+F, auto timeout, search
  width, and search height.
- Added English and Russian localization for the Toolbar category and controls.
- Updated `FEATURE_PARITY.md` to mark the safe Toolbar subset as implemented
  and keep command-backed QuickCommands as planned.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open a handled inventory screen, press CTRL+F, type part of a visible item
  name, press Enter, and verify matching slots are highlighted.
- Use the SBL and Items toolbar buttons and verify they open settings and the
  local item browser without sending chat commands.

## 2026-06-11 - First NEU Parity Audit Pass

### What Was Broken

- Slot Locking could crash handled screens such as creative inventory because
  the lock key generation called `ScreenHandler#getType()` on handlers that do
  not expose a handler type.
- The settings menu still suggested broad NEU parity through placeholder-style
  categories, which made it hard to tell which controls were real.
- Several visible controls were still coarse: language was the only dropdown,
  Slot Locking key editing still depended on Minecraft's keybind screen, color
  choices were hardcoded, and notifications did not have a local visual layer.

### What Improved

- Slot Locking now keys player inventory locks by stable
  `player_inventory:<index>` without calling handler type APIs.
- The visible settings catalog now contains connected MVP surfaces only:
  About/Core, GUI Locations, Notifications, Item List, Inventory Buttons, Slot
  Locking, and Tooltip Tweaks.
- Added dropdown, slider, keybind, and color controls for real config fields:
  menu scale, notification duration, Slot Locking key, accent color, item
  favorite color, and slot overlay color.
- Added a small local notification manager for visible rare drop, special
  zealot, slayer completion, dungeon score, and market chat lines.
- Added a safe handled-screen item-browser button controlled by Inventory
  Buttons settings.
- Tooltip metadata lines can now be toggled independently for internal id,
  rarity, category, recipe, and missing recipe data.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl` and confirm every visible toggle/control changes real behavior or
  persistent config instead of acting as a placeholder.
- Open creative inventory and normal handled inventory screens with Slot
  Locking enabled; verify locked player inventory slots render overlays without
  crashing.
- Send or observe matching chat lines such as `RARE DROP`, `SPECIAL ZEALOT`,
  `SLAYER QUEST COMPLETE`, `Score:`, or Bazaar/Auction text and verify local
  notifications appear when the related settings are enabled.

## 2026-06-10 - Client Startup Crash Fix

### What Was Broken

- Minecraft crashed during the Fabric client entrypoint with
  `Category 'skyblocklens:main' is already registered`.
- The open-settings keybind and Slot Locking keybind each created the same
  `KeyBinding.Category`, which modern Minecraft rejects.

### What Improved

- `SkyBlockLensClient` now owns one shared keybind category.
- Slot Locking registers its keybind against the shared category instead of
  creating a duplicate category.

### How To Check

- Run `.\gradlew.bat runClient --no-daemon --console=plain`.
- The client should reach normal initialization and log
  `SkyBlock Lens initialized with 14 local items.`

## 2026-06-10 - Item Browser Product Pass

### What Was Low Quality

- The Item Browser felt like a demo screen: it listed a few sample entries but
  did not make several Item List settings meaningful. Favorites, alias search,
  and usage lookup were exposed in settings but had little or no real behavior.
- Tooltip matching trusted the base Minecraft item id too much. A normal
  vanilla item could match bundled SkyBlock data if it shared the same
  `minecraftItem`, which makes item information feel unreliable.

### What Improved

- The local Item Browser now respects Item List settings for alias search,
  recipe display, usage view, favorites, and hiding intentionally incomplete
  data.
- Favorite items are saved in config and sorted ahead of normal results.
- Bundled item data now includes linked `recipeItems`, allowing reverse "used
  in" lookup without network requests.
- Tooltip matching is stricter: SkyBlock entries match by visible item name,
  item id, or an explicitly allowed vanilla fallback only. Aliases remain for
  item browser search, not automatic tooltip attachment.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Open `/sbl`, go to Item List, open Item Browser, search for `aote`, favorite
  it, close and reopen the screen, and verify it stays favorited.
- Toggle `Search Aliases`, `Recipe View`, `Usage View`, and `Favorites` in Item
  List settings and verify the browser changes behavior instead of only saving
  decorative config values.
- Hover a normal vanilla diamond sword and verify it does not show Aspect of
  the End data unless the visible item name actually matches a bundled
  SkyBlock entry.

## 2026-06-10 - Slot Locking Stability And Settings Geometry

### What Was Low Quality

- Slot Locking stored locks as `screen type + slot id`. The same player hotbar
  or inventory slot can have different screen slot ids in different handled
  screens, so a lock could appear inconsistent between player inventory,
  storage, or container screens.
- Settings control geometry was duplicated across drawing and hitbox code. This
  made it easy for a drawn control and its clickable area to drift apart during
  UI changes.

### What Improved

- Player inventory and hotbar locks now use a stable `player_inventory:<index>`
  key based on the player inventory index.
- New Slot Locking operations only target player inventory slots. Non-player
  container slots are ignored for new locks.
- Locked slot config is normalized to remove null, blank, and duplicate lock
  keys.
- Added `UiRect` for shared UI bounds and migrated key settings controls to use
  the same rectangle for rendering and hitbox registration.

### How To Check

- Run `.\gradlew.bat build --no-daemon --console=plain --stacktrace`.
- Launch the client, open a handled inventory screen, hover a player inventory
  or hotbar slot, press the Slot Locking key, then verify the lock overlay and
  click blocking remain consistent after opening another handled screen.
- In `/sbl`, verify settings toggles, action buttons, dropdowns, and sliders
  still only react when the actual control is clicked.
