package com.skyblocklens.config;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkyBlockSettingsCatalog {
	private static final List<SkyBlockSettingPage> PAGES = List.of(
			SkyBlockSettingPage.of(SkyBlockSettingCategory.ABOUT,
					group("about.core", true,
							SkyBlockSetting.toggle("core.enabled", true),
							SkyBlockSetting.toggle("core.only_skyblock", true),
							SkyBlockSetting.dropdown("core.language"),
							SkyBlockSetting.color("core.accent_color"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.MISC,
					group("misc.nameplates", true,
							toggle("misc.nameplates.enable"),
							toggle("misc.nameplates.show_self", false),
							SkyBlockSetting.action("misc.nameplates_own_alias"),
							SkyBlockSetting.color("misc.nameplates_own_name_color"),
							SkyBlockSetting.color("misc.nameplates_background_color"),
							SkyBlockSetting.slider("misc.nameplates_background_alpha"),
							SkyBlockSetting.color("misc.nameplates_text_color"),
							SkyBlockSetting.slider("misc.nameplates_text_alpha"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.GUI_LOCATIONS,
					group("gui.editor", true,
							SkyBlockSetting.action("hud.open_editor"),
							SkyBlockSetting.action("toolbar.open_editor"),
							SkyBlockSetting.color("gui.hud_background_color"),
							SkyBlockSetting.slider("gui.hud_background_alpha"),
							SkyBlockSetting.color("gui.scoreboard_background_color"),
							SkyBlockSetting.slider("gui.scoreboard_background_alpha"),
							SkyBlockSetting.color("toolbar.background_color"),
							SkyBlockSetting.slider("toolbar.background_alpha"),
							toggle("gui.snap_to_edges"),
							toggle("gui.show_anchor_grid"),
							toggle("gui.lock_layout", false))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.NOTIFICATIONS,
					group("notifications.chat", true,
							toggle("notifications.chat_filters", false),
							SkyBlockSetting.action("notifications.edit_filters"),
							toggle("notifications.chat_highlights"),
							SkyBlockSetting.action("notifications.edit_highlights"),
							toggle("notifications.rare_drop"),
							toggle("notifications.special_zealot"),
							toggle("notifications.slayer_complete"),
							toggle("notifications.dungeon_score"),
							toggle("notifications.market"),
							SkyBlockSetting.slider("notifications.duration"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.ITEM_LIST,
					group("item_list.browser", true,
							SkyBlockSetting.action("itemlist.open_browser"),
							toggle("itemlist.local_browser"),
							toggle("itemlist.browser_overlay"),
							SkyBlockSetting.keybind("itemlist.toggle_browser_keybind"),
							toggle("itemlist.hide_in_dungeons"),
							toggle("itemlist.overlay_while_typing"),
							SkyBlockSetting.color("itemlist.inventory_search_highlight_color"),
							toggle("itemlist.recipe_view"),
							toggle("itemlist.usage_view"),
							toggle("itemlist.search_aliases"),
							toggle("itemlist.hide_missing_data"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.TOOLBAR,
					group("toolbar.main", true,
							toggle("toolbar.enable"),
							toggle("toolbar.search_bar"),
							toggle("toolbar.inventory_search_button"),
							toggle("toolbar.ctrl_f"),
							toggle("toolbar.auto_turnoff_search"),
							SkyBlockSetting.color("toolbar.background_color"),
							SkyBlockSetting.slider("toolbar.background_alpha"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.INVENTORY_BUTTONS,
					group("inventory_buttons.main", true,
							toggle("inventory_buttons.enable"),
							SkyBlockSetting.action("inventory_buttons.open_editor"),
							toggle("inventory_buttons.item_browser"),
							SkyBlockSetting.color("inventory_buttons.background_color"),
							SkyBlockSetting.slider("inventory_buttons.background_alpha"),
							SkyBlockSetting.color("inventory_buttons.command_background_color"),
							SkyBlockSetting.slider("inventory_buttons.command_background_alpha"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.SLOT_LOCKING,
					group("slot_locking.main", true,
							toggle("slot_locking.enable"),
							toggle("slot_locking.binding"),
							toggle("slot_locking.binding_also_locks", false),
							SkyBlockSetting.keybind("slot_locking.keybind"),
							toggle("slot_locking.sound"),
							SkyBlockSetting.slider("slot_locking.sound_volume"),
							toggle("slot_locking.lock_slots_in_trade"),
							toggle("slot_locking.disable_in_storage", false),
							toggle("slot_locking.hotbar"),
							toggle("slot_locking.inventory"),
							toggle("slot_locking.warning"),
							toggle("slot_locking.overlay"),
							SkyBlockSetting.color("slot_locking.overlay_color"),
							SkyBlockSetting.action("slot_locking.reset"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.QUICK_SWAP,
					group("quick_swap.main", true,
							toggle("quick_swap.enable"),
							toggle("quick_swap.binding"),
							SkyBlockSetting.keybind("quick_swap.keybind"),
							toggle("quick_swap.overlay"))),
			SkyBlockSettingPage.of(SkyBlockSettingCategory.TOOLTIP_TWEAKS,
					group("tooltip_tweaks.items", true,
							toggle("tooltip_tweaks.enhanced_item_tooltips"),
							toggle("tooltip_tweaks.internal_id"),
							toggle("tooltip_tweaks.rarity"),
							toggle("tooltip_tweaks.category"),
							toggle("tooltip_tweaks.pet_extend_exp", false),
							toggle("tooltip_tweaks.missing_recipe_notice")))
	);

	private static final Set<String> IMPLEMENTED_SETTINGS = Set.of(
			"core.enabled",
			"core.only_skyblock",
			"core.language",
			"core.accent_color",
			"misc.nameplates.enable",
			"misc.nameplates.show_self",
			"misc.nameplates_own_alias",
			"misc.nameplates_own_name_color",
			"misc.nameplates_background_color",
			"misc.nameplates_background_alpha",
			"misc.nameplates_text_color",
			"misc.nameplates_text_alpha",
			"hud.open_editor",
			"toolbar.open_editor",
			"gui.hud_background_color",
			"gui.hud_background_alpha",
			"gui.scoreboard_background_color",
			"gui.scoreboard_background_alpha",
			"toolbar.background_color",
			"toolbar.background_alpha",
			"gui.snap_to_edges",
			"gui.show_anchor_grid",
			"gui.lock_layout",
			"notifications.chat_filters",
			"notifications.edit_filters",
			"notifications.chat_highlights",
			"notifications.edit_highlights",
			"notifications.rare_drop",
			"notifications.special_zealot",
			"notifications.slayer_complete",
			"notifications.dungeon_score",
			"notifications.market",
			"notifications.duration",
			"itemlist.open_browser",
			"itemlist.local_browser",
			"itemlist.browser_overlay",
			"itemlist.toggle_browser_keybind",
			"itemlist.hide_in_dungeons",
			"itemlist.overlay_while_typing",
			"itemlist.inventory_search_highlight_color",
			"itemlist.recipe_view",
			"itemlist.usage_view",
			"itemlist.search_aliases",
			"itemlist.hide_missing_data",
			"toolbar.enable",
			"toolbar.search_bar",
			"toolbar.inventory_search_button",
			"toolbar.ctrl_f",
			"toolbar.auto_turnoff_search",
			"inventory_buttons.enable",
			"inventory_buttons.open_editor",
			"inventory_buttons.item_browser",
			"inventory_buttons.background_color",
			"inventory_buttons.background_alpha",
			"inventory_buttons.command_background_color",
			"inventory_buttons.command_background_alpha",
			"quick_swap.enable",
			"quick_swap.binding",
			"quick_swap.keybind",
			"quick_swap.overlay",
			"slot_locking.enable",
			"slot_locking.binding",
			"slot_locking.binding_also_locks",
			"slot_locking.keybind",
			"slot_locking.sound",
			"slot_locking.sound_volume",
			"slot_locking.lock_slots_in_trade",
			"slot_locking.disable_in_storage",
			"slot_locking.hotbar",
			"slot_locking.inventory",
			"slot_locking.warning",
			"slot_locking.overlay",
			"slot_locking.overlay_color",
			"slot_locking.reset",
			"tooltip_tweaks.enhanced_item_tooltips",
			"tooltip_tweaks.internal_id",
			"tooltip_tweaks.rarity",
			"tooltip_tweaks.category",
			"tooltip_tweaks.pet_extend_exp",
			"tooltip_tweaks.missing_recipe_notice"
	);
	private static final Map<SkyBlockSettingCategory, SkyBlockSettingPage> PAGES_BY_CATEGORY = buildPagesByCategory();
	private static final Map<String, Boolean> DEFAULT_TOGGLES = buildDefaultToggles();

	private SkyBlockSettingsCatalog() {
	}

	public static List<SkyBlockSettingPage> pages() {
		return PAGES;
	}

	public static SkyBlockSettingPage page(SkyBlockSettingCategory category) {
		return PAGES_BY_CATEGORY.get(category);
	}

	public static Map<String, Boolean> defaultToggles() {
		return DEFAULT_TOGGLES;
	}

	public static Boolean defaultValue(String id) {
		return DEFAULT_TOGGLES.get(id);
	}

	public static boolean isImplemented(String id) {
		return IMPLEMENTED_SETTINGS.contains(id);
	}

	private static SkyBlockSetting toggle(String id) {
		return SkyBlockSetting.toggle(id, true);
	}

	private static SkyBlockSetting toggle(String id, boolean defaultEnabled) {
		return SkyBlockSetting.toggle(id, defaultEnabled);
	}

	private static SkyBlockSettingGroup group(String id, boolean expandedByDefault, SkyBlockSetting... settings) {
		return SkyBlockSettingGroup.of(id, expandedByDefault, settings);
	}

	private static Map<SkyBlockSettingCategory, SkyBlockSettingPage> buildPagesByCategory() {
		Map<SkyBlockSettingCategory, SkyBlockSettingPage> pages = new EnumMap<>(SkyBlockSettingCategory.class);
		for (SkyBlockSettingPage page : PAGES) {
			pages.put(page.category(), page);
		}
		return pages;
	}

	private static Map<String, Boolean> buildDefaultToggles() {
		Map<String, Boolean> toggles = new LinkedHashMap<>();
		for (SkyBlockSettingPage page : PAGES) {
			for (SkyBlockSettingGroup group : page.groups()) {
				for (SkyBlockSetting setting : group.settings()) {
					if (setting.control() == SkyBlockSettingControl.TOGGLE) {
						toggles.put(setting.id(), setting.defaultEnabled() && isImplemented(setting.id()));
					}
				}
			}
		}
		return Map.copyOf(toggles);
	}
}
