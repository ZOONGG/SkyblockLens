````md
# AGENTS.md

## Project

This repository is a Minecraft Fabric mod project for a modern Hypixel SkyBlock quality-of-life mod inspired by NotEnoughUpdates.

The project goal is to build a legitimate, open-source, client-side QoL mod for the latest modern Minecraft version supported by Hypixel, using Fabric.

This is NOT a cheat client.

## Core Rules

- Keep changes minimal, safe, and production-ready.
- Do not rewrite large parts of the project unless explicitly requested.
- Prefer small, reviewable commits and clear module boundaries.
- Do not add hidden telemetry, ads, trackers, remote code execution, token collection, or suspicious network behavior.
- Do not store secrets, API keys, session tokens, Minecraft account data, or personal user data in the repository.
- Always preserve UTF-8.
- Keep line endings stable using `.gitattributes`.
- After finishing changes, run `git add -A`.

## Mod Loader and Version

- Use Fabric.
- Target the latest stable Minecraft 1.21.x version that is realistically usable on Hypixel modern client.
- Use the official Fabric example mod and Fabric documentation as architecture references.
- Do not use outdated Forge 1.8.9 architecture directly.
- Do not assume old Minecraft classes, names, events, rendering APIs, or GUI APIs still exist.

## Reference Project

Original reference:
https://github.com/NotEnoughUpdates/NotEnoughUpdates

Use it as a feature and UX reference, not as code to blindly copy.

If code, assets, JSON data, or logic are reused from NotEnoughUpdates, preserve license notices and attribution. Respect the original project's license.

## Forbidden Features

Never implement or scaffold:

- macros;
- auto-clickers;
- auto-farming;
- auto-mining;
- auto-fishing;
- automated dungeon solvers that perform actions;
- automatic terminal completion;
- packet abuse;
- ghost blocks;
- anti-knockback;
- reach, aim assist, triggerbot, kill aura;
- movement hacks;
- anti-cheat bypass;
- hidden mod behavior;
- server-side exploit helpers;
- anything similar to Oringo or other cheat clients.

Allowed direction: client-side QoL, HUD, tooltips, item browser, recipe viewer, chat filters, profile viewer, timers, settings UI, visual overlays that do not automate gameplay.

## Architecture

Use a clean modular architecture:

- `core` - mod initialization, config, logging, lifecycle.
- `config` - settings, serialization, defaults.
- `i18n` - language switching and translation keys.
- `ui` - config menu, screens, widgets, HUD editor.
- `hud` - HUD modules and positioning.
- `skyblock` - SkyBlock detection, scoreboard/actionbar/tablist parsing.
- `items` - item database, item browser, tooltips, recipes.
- `chat` - filters, highlights, chat QoL.
- `api` - optional future API integration, disabled by default.
- `security` - safety checks and suspicious behavior prevention.

Avoid god classes. Avoid mixing GUI, parsing, config, and networking in one file.

## Internationalization

- All visible UI strings must use lang files.
- Support at least English and Russian.
- Do not hardcode visible text in Java/Kotlin code.
- Russian translation must be natural and understandable.
- Translation keys should be stable and grouped by feature.

Example style:

```properties
neu_modern.config.title=NEU Modern Settings
neu_modern.config.language=Language
neu_modern.hud.enabled=Enable HUD
````

## UI and UX

The settings menu should be modern and polished, but not a 1:1 clone of original NEU.

Required UI principles:

* dark-style interface by default;
* clean categories;
* search through settings;
* toggles, sliders, dropdowns;
* reset settings button;
* HUD preview/editor;
* keyboard and mouse friendly;
* no broken scaling on different resolutions;
* no unreadable text;
* no placeholder-only UI.

## Data

* Local JSON data is allowed.
* Remote data updates must be explicit, safe, transparent, and disableable.
* Never download executable files.
* Never execute remote code.
* If data is missing, the mod must degrade gracefully instead of crashing.

## Hypixel and SkyBlock Detection

* Detect Hypixel/SkyBlock safely using scoreboard, tablist, actionbar, or known client-side signals.
* If the player is not on Hypixel/SkyBlock, disable SkyBlock-specific features gracefully.
* Never spam logs or chat.
* Never send automatic commands unless explicitly approved and clearly safe.

## Networking

Network access must be opt-in per feature.

Before adding any network request:

* explain why it is needed;
* make it disableable;
* avoid sending personal data;
* handle failures gracefully;
* rate-limit requests;
* document the endpoint in README or docs.

## Build and Test

Before finishing a task:

* run Gradle build;
* fix compilation errors;
* run formatting if configured;
* update README/CHANGELOG if user-facing behavior changed;
* update docs if architecture or features changed.

If the environment cannot run Minecraft, still ensure the project compiles.

## Documentation

Maintain these docs:

* `README.md` - what the mod is, install/build instructions, no-cheat disclaimer.
* `FEATURE_PARITY.md` - comparison with NotEnoughUpdates features.
* `SECURITY.md` - privacy and safety rules.
* `CHANGELOG.md` - user-facing changes.
* `docs/hypixel-rules.md` - summary of allowed/disallowed feature categories.

## MVP Definition

The MVP is done only when:

* the Fabric mod loads;
* Gradle build passes;
* settings menu opens;
* English and Russian language switching works;
* basic HUD modules exist;
* item browser exists with local sample data;
* item tooltip enhancements exist;
* chat filters/highlights exist;
* SkyBlock/Hypixel detection has graceful fallback;
* README, FEATURE_PARITY, SECURITY, and CHANGELOG exist;
* forbidden cheat-like features are explicitly excluded.

## Final Response Format

At the end of every task, report:

* what changed;
* files touched;
* build/test result;
* what remains;
* any risky area or assumption.

```
```
