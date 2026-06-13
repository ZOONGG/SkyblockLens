# Changelog

## 0.1.25

- Fixed live HUD/editor text fitting so resized modules keep full readable text
  instead of truncated labels, and kept HUD modules free of top accent strips.
- Changed color controls so the swatch opens the compact palette and the hex
  field is only for manual text input.
- Reworked the handled-screen item browser into a fixed far-right paged SkyBlock
  item panel with transparent hover name tooltips and no width/icon-size setting.
- Removed visible item favorites and disabled inventory buttons in gameplay;
  the old inventory button controller is archived under `docs/archived-code`.
- Added visual 3x3 crafting-grid recipes in item details and regenerated local
  SkyBlock item data with recipe slots and modern `ItemModel` icons.
- Optimized inventory/chest search highlighting by indexing local item data for
  stack matching instead of scanning the full item database per slot.

## 0.1.24

- Reworked the handled-inventory toolbar item browser into a persistent
  right-side SkyBlock item grid with scroll buttons, mouse-wheel scrolling,
  hover feedback, empty/missing-data states, and click-through item details.
- Replaced the sample item list with transformed local data from
  `NotEnoughUpdates-REPO`, preserving MIT attribution in `THIRD_PARTY_NOTICES.md`.
- Added item detail actions for recipe, usages, wiki, AH/BZ search, internal ID
  copy, and favorites, all behind explicit user clicks.
- Made inventory/chest search work from local SkyBlock item data in normal
  handled screens and removed the old nonmatching-slot dimming.
- Hid quick command buttons from gameplay while leaving their editor/config code
  in place for a future opt-in return.
- Removed visible search width/height settings from the settings menu, keeping
  search sizing in the toolbar editor, and made the toolbar editor respect the
  global anchor-grid setting.
- Reduced the settings color picker size and render cost.

## 0.1.23

- Improved the settings color picker input so clicking a hex field selects the
  current value, first typed hex digit replaces it, and the popup stays anchored
  to the setting row instead of following the cursor.
- Reduced HUD and toolbar editor resize handles and made HUD preview text scale
  from both module width and height.
- Fixed toolbar width calculation so disabled toolbar buttons no longer leave a
  phantom gap or offset the editor preview.
- Hardened Slot Locking against shift-click quick-move into protected inventory
  slots.
- Fixed SkyBlock item search normalization for Minecraft formatting codes in
  item display names.

## 0.1.22

- Fixed the client startup crash caused by duplicate toolbar setting IDs in the
  implemented-settings registry.
- Verified the settings registry no longer contains duplicate `Set.of` string
  entries that can crash Fabric entrypoint initialization.

## 0.1.21

- Removed Fullbright completely, including the setting and lightmap mixin.
- Changed settings colors to open an optimized popup HSV picker from the color
  square instead of rendering the heavy picker inside every row.
- Made dependent settings hide until their parent toggle is enabled, including
  Slot Lock sound volume and toolbar/search sub-options.
- Added a Toolbar Editor under GUI Locations for moving and resizing the
  inventory search toolbar near the hotbar.
- Reworked the handled-inventory toolbar to show only the SkyBlock item search
  field and one inventory-find button.
- Added a right-side local SkyBlock item results panel with recipe/source/info
  details while typing in the toolbar search field.
- Made HUD editor text resize with the module height and reduced the resize
  handle size.

## 0.1.20

- Replaced the settings color swatch grid with a proper HSV color picker:
  saturation/brightness square, hue bar, current-color preview, and clearer hex
  input.
- Fixed Fullbright so it no longer writes illegal gamma values. It now uses
  client lightmap rendering hooks instead of `options.gamma = 15`.
- Reworked HUD editor resizing to change module width and height independently
  from the cursor, with a solid visible resize handle and real preview text.
- Simplified keybind rows to one trash-style clear button and removed the
  visible reset button.
- Improved the inventory button editor with draggable/resizable command
  buttons, item-icon selection, and quick text/background color swatches.

## 0.1.19

- Locked the settings screen back to the stable medium layout and removed the
  visible menu scale control to avoid broken small/large UI states.
- Added a working Fullbright toggle that raises client gamma locally and
  restores the previous gamma when disabled.
- Added a confirmation dialog before resetting all SkyBlock Lens settings.
- Replaced remaining vanilla Minecraft buttons in mod-owned editor/browser
  screens with the custom SkyBlock Lens button style.
- Tightened settings row spacing so labels, descriptions, search, and controls
  do not overlap.
- Removed the shift-click bypass from Slot Locking so protected inventory and
  hotbar slots cannot be moved through the visible settings.

## 0.1.18

- Reworked the settings UI spacing, menu scale presets, hover feedback, and
  opening animation so the menu reads closer to the compact NEU-style layout.
- Fixed slider dragging so height/width controls no longer update the wrong
  setting.
- Added color palettes and opacity controls for HUD, toolbar, and inventory
  button backgrounds.
- Replaced global HUD scale with per-module resize handles in the HUD editor.
- Added an inventory button editor with draggable/resizable item and quick
  command buttons. Commands only run from an explicit button click.
- Improved handled-inventory toolbar placement and made search highlighting
  activate as soon as the user types a query.

## 0.1.17

- Made the `Only on Hypixel SkyBlock` setting a real central guard instead of a
  HUD-only switch.
- The setting now disables SkyBlock HUD modules, item tooltip additions,
  toolbar buttons, inventory item buttons, chat filters, chat highlights, and
  visual chat alerts outside Hypixel SkyBlock.
- Slot Locking intentionally remains active outside SkyBlock as a local item
  safety feature.

## 0.1.16

- Added real in-game editors for Notification `Chat Filters` and `Chat
  Highlights` terms.
- Filter/highlight terms can now be added, removed, cleared, localized, and
  saved to config from the settings UI.
- Removed the placeholder `example-blocked-term` default so chat filtering no
  longer looks configured when it is not.

## 0.1.15

- Fixed Slot Locking overlays rendering away from the actual inventory slots by
  using handled-screen local slot coordinates during slot rendering.
- Added locked hotbar overlays in the normal in-game HUD and blocked dropping
  the selected locked hotbar slot before Minecraft calls `dropSelectedItem`.
- Moved the handled-screen Toolbar search field below inventory screens,
  improved text input fallback, and made inventory search use local SkyBlock
  item names and aliases when possible.
- Removed the redundant settings group header row for simple single-group
  categories and adjusted menu scale factors for a more stable small/large UI.

## 0.1.14

- Fixed a Minecraft client startup crash caused by the handled-screen mixin
  injecting into a `keyReleased` method that no longer exists directly on
  `HandledScreen` in Minecraft 1.21.11.
- Slot Locking key release handling now uses the modern `Screen` override path,
  so Fabric can transform handled screens successfully at runtime.

## 0.1.13

- Added NEU Tooltip Tweaks parity for `Expand Pet Exp Requirement`.
- Pet tooltips with visible abbreviated XP progress can now get a localized
  full-number `EXP` line without using API calls, profile lookups, commands, or
  server automation.
- Updated English and Russian localization plus parity docs for the new toggle.

## 0.1.12

- Added safe NEU Slot Binding parity controls: `Enable Slot Binding` and
  `Don't Drop Bound Slots`.
- Holding the Slot Lock key on a player inventory slot can now create a
  persisted local binding by hovering a hotbar slot, with a cyan preview line
  and bound-slot overlay.
- Bound slots can optionally behave as protected slots for drop/click blocking.
  Automatic bound-slot item movement from NEU is intentionally left for a
  separate safety review.

## 0.1.11

- Added NEU Slot Locking parity controls for `Lock Slots in Trade` and
  `Disable Locking in Storage`.
- Slot Locking now receives handled-screen titles and uses safe client-side
  trade/storage title detection to decide whether locks should block clicks,
  drop keys, and overlays in those screens.
- Updated English and Russian localization plus parity docs for the new
  controls.

## 0.1.10

- Added remaining safe NEU Slot Locking controls for this slice: Slot Lock
  Sound, Slot Lock Sound Vol., and Reset Locked Slots.
- Slot lock/unlock actions now play a local configurable ding, and reset clears
  persisted locked slots without sending commands or touching server state.
- Updated English and Russian localization and parity docs for the new controls.

## 0.1.9

- Made the Settings menu scale control apply to the entire menu coordinate
  space. Small is now visibly compact, large is visibly larger, and mouse
  clicks/scrolling are mapped to the scaled UI.
- Improved Slot Locking parity with NEU by blocking number-key hotbar swaps
  into locked hotbar slots.

## 0.1.8

- Added NEU-style local recipe navigation inside the Item Browser: recipe
  ingredient rows and "Used In" rows can now be clicked to jump to the linked
  bundled item.
- Added local item detail history with a Back button and Backspace shortcut
  when the search field is not focused.
- Updated English and Russian localization and parity docs for recipe/usage
  navigation.

## 0.1.7

- Added a real NEU-inspired Toolbar subset for handled inventory screens:
  local search bar, settings shortcut, item browser shortcut, inventory search
  toggle, clear button, CTRL+F focus, and auto timeout for search highlighting.
- Added Toolbar settings for search bar visibility, toolbar buttons, CTRL+F,
  auto search timeout, search width, and search height.
- Added slot highlighting/dimming based on toolbar search text without sending
  commands or clicking slots for the player.
- Updated English and Russian localization and parity docs for Toolbar status.

## 0.1.6

- Fixed the handled-screen Slot Locking crash caused by querying
  `ScreenHandler#getType()` on screen handlers that do not support it, including
  creative inventory handlers.
- Reduced the visible settings catalog to connected modules only, so unfinished
  NEU parity categories are tracked in docs instead of appearing as fake
  toggles.
- Added real settings controls for menu scale, accent/favorite/slot-overlay
  colors, notification duration, Slot Locking keybind editing, and tooltip
  metadata toggles.
- Added local visual notifications for visible rare drop, special zealot,
  slayer completion, dungeon score, and market chat messages.
- Added a safe inventory item-browser button for handled screens.
- Updated English and Russian localization for the new visible controls.

## 0.1.5

- Fixed a client startup crash caused by registering the same Minecraft keybind
  category twice. The settings key and Slot Locking key now share one
  `skyblocklens:main` category.

## 0.1.4

- Reworked the local Item Browser into a more useful SkyBlock item lookup with
  persistent favorites, alias-aware search, recipe display, reverse "used in"
  lookup, list scrolling, and setting-controlled sections.
- Expanded bundled local item data with linked recipe ingredients so recipe and
  usage views can show real relationships without network access.
- Made item tooltip matching stricter so vanilla items no longer match SkyBlock
  entries only because they share the same base Minecraft item id.

## 0.1.3

- Stabilized Slot Locking so player inventory and hotbar locks use the stable
  player inventory index instead of screen-specific slot ids. A locked player
  slot now stays consistent across handled inventory screens.
- Slot Locking now removes duplicate lock entries during unlock/config
  normalization and no longer allows new locks on non-player container slots.
- Refactored settings control geometry into a small `UiRect` helper so buttons,
  sliders, dropdowns, toggles, and their hitboxes share the same bounds.

## 0.1.2

- Fixed Russian category localization in the settings category list.
- Removed the Project block from About and removed Security / IO Test from the
  visible settings menu.
- Reworked settings hitboxes so toggles, action buttons, dropdowns, and sliders
  only react when their actual control is clicked.
- Fixed HUD scale slider interaction and capped HUD scale at 150%.
- Improved settings layout with separated header/search/content areas, clipping
  for scrollable panels, a new teal/amber visual palette, hover states, and a
  small animated header accent.
- Replaced language toggle button with a dropdown-style selector.
- Added a real Slot Locking keybind, slot lock persistence, click blocking for
  locked slots, and a locked-slot overlay in handled inventory screens.

## 0.1.1

- Reworked the settings screen into a NEU-inspired SkyBlock category browser
  with a large left-side category list, collapsible groups, custom dark rows,
  search, scrollbars, toggles, action buttons, and a HUD scale slider.
- Added a shared SkyBlock feature catalog and persistent feature toggle map for
  safe modules such as Item List, Slot Locking, Tooltip Tweaks, Item Overlays,
  Skill Overlays, Dungeons, Mining, Fishing, Garden, AH/Bazaar Tweaks, Wardrobe
  Keybinds, Accessory Bag, Profile Viewer, Minion Helper, APIs, and Security.
- Connected existing tooltip, item browser, chat filter/highlight, HUD editor,
  language, API, and master switches to the new feature toggle system.
- Expanded English and Russian localization for the new SkyBlock settings
  catalog.

## 0.1.0

- Initial Fabric 1.21.11 MVP scaffold.
- Added settings UI, EN/RU language support, HUD modules, HUD editor, item
  browser, local tooltip data, chat QoL, SkyBlock context detection, and safety
  documentation.
