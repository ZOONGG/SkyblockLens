package com.skyblocklens.config;

public enum SkyBlockSettingCategory {
	ABOUT("about"),
	MISC("misc"),
	GUI_LOCATIONS("gui_locations"),
	NOTIFICATIONS("notifications"),
	ITEM_LIST("item_list"),
	TOOLBAR("toolbar"),
	INVENTORY_BUTTONS("inventory_buttons"),
	SLOT_LOCKING("slot_locking"),
	QUICK_SWAP("quick_swap"),
	TOOLTIP_TWEAKS("tooltip_tweaks"),
	ITEM_OVERLAYS("item_overlays"),
	SKILL_OVERLAYS("skill_overlays"),
	TODO_OVERLAYS("todo_overlays"),
	SLAYER_OVERLAY("slayer_overlay"),
	STORAGE_GUI("storage_gui"),
	DUNGEONS("dungeons"),
	ENCHANTING("enchanting"),
	MINING("mining"),
	FISHING("fishing"),
	GARDEN("garden"),
	IMPROVED_SB_MENUS("improved_sb_menus"),
	EQUIPMENT_HUD("equipment_hud"),
	CALENDAR("calendar"),
	TRADE_MENU("trade_menu"),
	PET_OVERLAY("pet_overlay"),
	WORLD_RENDERER("world_renderer"),
	AH_TWEAKS("ah_tweaks"),
	BAZAAR_TWEAKS("bazaar_tweaks"),
	RECIPE_TWEAKS("recipe_tweaks"),
	PRICE_GRAPH("price_graph"),
	WARDROBE_KEYBINDS("wardrobe_keybinds"),
	ACCESSORY_BAG("accessory_bag"),
	MUSEUM("museum"),
	PROFILE_VIEWER("profile_viewer"),
	MINION_HELPER("minion_helper"),
	APIS("apis");

	private final String id;

	SkyBlockSettingCategory(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public String key() {
		return "skyblocklens.category." + id;
	}
}
