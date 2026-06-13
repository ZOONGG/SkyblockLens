package com.skyblocklens.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkyBlockLensConfig {
	public String language = "en_us";
	public String menuUiScale = "medium";
	public String accentColor = "#42E8C8";
	public String overlayColor = "#E06A4D";
	public String hudBackgroundColor = "#000000";
	public String toolbarBackgroundColor = "#0B1117";
	public String inventoryButtonBackgroundColor = "#151C23";
	public String inventoryCommandBackgroundColor = "#333A35";
	public String inventorySearchHighlightColor = "#E7A743";
	public String nameplateBackgroundColor = "#000000";
	public String nameplateTextColor = "#FFFFFF";
	public String nameplateOwnNameColor = "#42E8C8";
	public String scoreboardBackgroundColor = "#000000";
	public String slotLockKey = "key.keyboard.l";
	public String itemBrowserToggleKey = "key.keyboard.o";
	public String nameplateOwnAlias = "";
	public int slotLockSoundVolume = 20;
	public int toolbarSearchWidth = 420;
	public int toolbarSearchHeight = 22;
	public int itemBrowserWidth = 252;
	public int itemBrowserHeight = 0;
	public int itemBrowserIconSize = 22;
	public int toolbarOffsetX = 0;
	public int toolbarOffsetY = 0;
	public int itemBrowserOffsetX = 0;
	public int itemBrowserY = 8;
	public int hudBackgroundAlpha = 153;
	public int toolbarBackgroundAlpha = 255;
	public int inventoryButtonBackgroundAlpha = 255;
	public int inventoryCommandBackgroundAlpha = 190;
	public int nameplateBackgroundAlpha = 96;
	public int nameplateTextAlpha = 255;
	public int scoreboardBackgroundAlpha = 96;
	public int inventoryItemButtonX = 178;
	public int inventoryItemButtonY = 6;
	public int inventoryItemButtonWidth = 74;
	public int inventoryItemButtonHeight = 18;
	public boolean enabled = true;
	public boolean onlyOnSkyBlock = true;
	public boolean enhancedTooltips = true;
	public boolean itemBrowser = true;
	public boolean chatFilters = false;
	public boolean chatHighlights = true;
	public boolean apiEnabled = false;
	public boolean itemBrowserOverlayVisible = true;
	public double hudScale = 1.0D;
	public int notificationDurationSeconds = 4;
	public Map<String, HudModuleConfig> hudModules = new LinkedHashMap<>();
	public Map<String, Boolean> featureToggles = new LinkedHashMap<>();
	public Map<String, String> customKeybinds = new LinkedHashMap<>();
	public List<String> lockedSlots = new ArrayList<>();
	public List<String> slotBindings = new ArrayList<>();
	public List<String> blockedChatTerms = new ArrayList<>();
	public List<String> highlightChatTerms = new ArrayList<>();
	public List<InventoryCommandButtonConfig> inventoryCommandButtons = new ArrayList<>();

	public static SkyBlockLensConfig defaults() {
		SkyBlockLensConfig config = new SkyBlockLensConfig();
		config.hudModules.put("skyblock_status", new HudModuleConfig(true, 12, 12));
		config.hudModules.put("location", new HudModuleConfig(true, 12, 30));
		config.hudModules.put("purse", new HudModuleConfig(true, 12, 48));
		config.hudModules.put("bits", new HudModuleConfig(false, 12, 66));
		config.featureToggles.putAll(SkyBlockSettingsCatalog.defaultToggles());
		config.highlightChatTerms.add("RARE DROP");
		config.highlightChatTerms.add("SLAYER QUEST COMPLETE");
		config.highlightChatTerms.add("SPECIAL ZEALOT");
		return config;
	}

	public void normalize() {
		SkyBlockLensConfig defaults = defaults();
		if (!"ru_ru".equals(language) && !"en_us".equals(language)) {
			language = "en_us";
		}
		menuUiScale = "medium";
		accentColor = normalizeHex(accentColor, defaults.accentColor);
		overlayColor = normalizeHex(overlayColor, defaults.overlayColor);
		hudBackgroundColor = normalizeHex(hudBackgroundColor, defaults.hudBackgroundColor);
		toolbarBackgroundColor = normalizeHex(toolbarBackgroundColor, defaults.toolbarBackgroundColor);
		inventoryButtonBackgroundColor = normalizeHex(inventoryButtonBackgroundColor, defaults.inventoryButtonBackgroundColor);
		inventoryCommandBackgroundColor = normalizeHex(inventoryCommandBackgroundColor, defaults.inventoryCommandBackgroundColor);
		inventorySearchHighlightColor = normalizeHex(inventorySearchHighlightColor, defaults.inventorySearchHighlightColor);
		nameplateBackgroundColor = normalizeHex(nameplateBackgroundColor, defaults.nameplateBackgroundColor);
		nameplateTextColor = normalizeHex(nameplateTextColor, defaults.nameplateTextColor);
		nameplateOwnNameColor = normalizeHex(nameplateOwnNameColor, defaults.nameplateOwnNameColor);
		scoreboardBackgroundColor = normalizeHex(scoreboardBackgroundColor, defaults.scoreboardBackgroundColor);
		if (slotLockKey == null || slotLockKey.isBlank()) {
			slotLockKey = defaults.slotLockKey;
		}
		if (itemBrowserToggleKey == null || itemBrowserToggleKey.isBlank()) {
			itemBrowserToggleKey = defaults.itemBrowserToggleKey;
		}
		nameplateOwnAlias = sanitizeText(nameplateOwnAlias, "", 32);
		slotLockSoundVolume = Math.max(0, Math.min(100, slotLockSoundVolume));
		toolbarSearchWidth = Math.max(140, Math.min(560, toolbarSearchWidth));
		toolbarSearchHeight = Math.max(16, Math.min(34, toolbarSearchHeight));
		itemBrowserWidth = Math.max(188, Math.min(360, itemBrowserWidth));
		itemBrowserIconSize = Math.max(18, Math.min(32, itemBrowserIconSize));
		toolbarOffsetX = Math.max(-900, Math.min(900, toolbarOffsetX));
		toolbarOffsetY = Math.max(-900, Math.min(900, toolbarOffsetY));
		itemBrowserOffsetX = Math.max(-900, Math.min(900, itemBrowserOffsetX));
		itemBrowserY = Math.max(4, Math.min(900, itemBrowserY));
		itemBrowserHeight = Math.max(0, Math.min(1200, itemBrowserHeight));
		hudBackgroundAlpha = clampAlpha(hudBackgroundAlpha);
		toolbarBackgroundAlpha = clampAlpha(toolbarBackgroundAlpha);
		inventoryButtonBackgroundAlpha = clampAlpha(inventoryButtonBackgroundAlpha);
		inventoryCommandBackgroundAlpha = clampAlpha(inventoryCommandBackgroundAlpha);
		nameplateBackgroundAlpha = clampAlpha(nameplateBackgroundAlpha);
		nameplateTextAlpha = clampAlpha(nameplateTextAlpha);
		scoreboardBackgroundAlpha = clampAlpha(scoreboardBackgroundAlpha);
		inventoryItemButtonX = Math.max(-220, Math.min(280, inventoryItemButtonX));
		inventoryItemButtonY = Math.max(-120, Math.min(220, inventoryItemButtonY));
		inventoryItemButtonWidth = Math.max(34, Math.min(160, inventoryItemButtonWidth));
		inventoryItemButtonHeight = Math.max(16, Math.min(42, inventoryItemButtonHeight));
		hudScale = Math.max(0.5D, Math.min(2.0D, hudScale));
		notificationDurationSeconds = Math.max(2, Math.min(10, notificationDurationSeconds));
		for (Map.Entry<String, HudModuleConfig> entry : defaults.hudModules.entrySet()) {
			hudModules.putIfAbsent(entry.getKey(), entry.getValue());
		}
		for (HudModuleConfig module : hudModules.values()) {
			if (module != null) {
				module.scale = Math.max(0.5D, Math.min(2.0D, module.scale <= 0.0D ? 1.0D : module.scale));
				module.width = module.width <= 0 ? 0 : Math.max(72, Math.min(420, module.width));
				module.height = module.height <= 0 ? 0 : Math.max(22, Math.min(180, module.height));
			}
		}
		blockedChatTerms = normalizeTerms(blockedChatTerms, defaults.blockedChatTerms);
		highlightChatTerms = normalizeTerms(highlightChatTerms, defaults.highlightChatTerms);
		if (lockedSlots == null) {
			lockedSlots = new ArrayList<>();
		} else {
			lockedSlots = new ArrayList<>(new LinkedHashSet<>(lockedSlots.stream()
					.filter(value -> value != null && !value.isBlank())
					.toList()));
		}
		if (slotBindings == null) {
			slotBindings = new ArrayList<>();
		} else {
			slotBindings = new ArrayList<>(new LinkedHashSet<>(slotBindings.stream()
					.filter(value -> value != null && value.matches("player_inventory:\\d+->player_inventory:\\d+"))
					.toList()));
		}
		normalizeInventoryCommands();
		if (featureToggles == null) {
			featureToggles = new LinkedHashMap<>();
		}
		for (Map.Entry<String, Boolean> entry : SkyBlockSettingsCatalog.defaultToggles().entrySet()) {
			featureToggles.putIfAbsent(entry.getKey(), entry.getValue());
			if (!SkyBlockSettingsCatalog.isImplemented(entry.getKey())) {
				featureToggles.put(entry.getKey(), false);
			}
		}
		if (customKeybinds == null) {
			customKeybinds = new LinkedHashMap<>();
		} else {
			customKeybinds.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank()
					|| entry.getValue() == null || entry.getValue().isBlank());
		}
		syncLegacyFieldsToFeatureToggles();
	}

	public boolean featureEnabled(String id) {
		if (!SkyBlockSettingsCatalog.isImplemented(id)) {
			return false;
		}
		Boolean value = featureToggles.get(id);
		if (value != null) {
			return value;
		}
		return Boolean.TRUE.equals(SkyBlockSettingsCatalog.defaultValue(id));
	}

	public void setFeatureEnabled(String id, boolean enabled) {
		if (!SkyBlockSettingsCatalog.isImplemented(id)) {
			featureToggles.put(id, false);
			return;
		}
		featureToggles.put(id, enabled);
		syncFeatureToggleToLegacyField(id, enabled);
	}

	public double menuUiScaleFactor() {
		return 1.0D;
	}

	public String colorValue(String settingId) {
		return switch (settingId) {
			case "core.accent_color" -> accentColor;
			case "gui.hud_background_color" -> hudBackgroundColor;
			case "toolbar.background_color" -> toolbarBackgroundColor;
			case "inventory_buttons.background_color" -> inventoryButtonBackgroundColor;
			case "inventory_buttons.command_background_color" -> inventoryCommandBackgroundColor;
			case "itemlist.inventory_search_highlight_color" -> inventorySearchHighlightColor;
			case "misc.nameplates_background_color" -> nameplateBackgroundColor;
			case "misc.nameplates_text_color" -> nameplateTextColor;
			case "misc.nameplates_own_name_color" -> nameplateOwnNameColor;
			case "gui.scoreboard_background_color" -> scoreboardBackgroundColor;
			case "slot_locking.overlay_color" -> overlayColor;
			default -> accentColor;
		};
	}

	public void setColorValue(String settingId, String value) {
		String normalized = normalizeHex(value, colorValue(settingId));
		switch (settingId) {
			case "core.accent_color" -> accentColor = normalized;
			case "gui.hud_background_color" -> hudBackgroundColor = normalized;
			case "toolbar.background_color" -> toolbarBackgroundColor = normalized;
			case "inventory_buttons.background_color" -> inventoryButtonBackgroundColor = normalized;
			case "inventory_buttons.command_background_color" -> inventoryCommandBackgroundColor = normalized;
			case "itemlist.inventory_search_highlight_color" -> inventorySearchHighlightColor = normalized;
			case "misc.nameplates_background_color" -> nameplateBackgroundColor = normalized;
			case "misc.nameplates_text_color" -> nameplateTextColor = normalized;
			case "misc.nameplates_own_name_color" -> nameplateOwnNameColor = normalized;
			case "gui.scoreboard_background_color" -> scoreboardBackgroundColor = normalized;
			case "slot_locking.overlay_color" -> overlayColor = normalized;
			default -> {
			}
		}
	}

	public int accentArgb() {
		return parseHexColor(accentColor, 0xFF42E8C8);
	}

	public int hudBackgroundArgb() {
		return colorWithAlpha(hudBackgroundColor, hudBackgroundAlpha, 0x99000000);
	}

	public int toolbarBackgroundArgb() {
		return colorWithAlpha(toolbarBackgroundColor, toolbarBackgroundAlpha, 0xFF0B1117);
	}

	public int inventoryButtonBackgroundArgb() {
		return colorWithAlpha(inventoryButtonBackgroundColor, inventoryButtonBackgroundAlpha, 0xFF151C23);
	}

	public int inventoryCommandBackgroundArgb() {
		return colorWithAlpha(inventoryCommandBackgroundColor, inventoryCommandBackgroundAlpha, 0xBE333A35);
	}

	public int nameplateBackgroundArgb() {
		return colorWithAlpha(nameplateBackgroundColor, nameplateBackgroundAlpha, 0x60000000);
	}

	public int nameplateTextArgb() {
		return colorWithAlpha(nameplateTextColor, nameplateTextAlpha, 0xFFFFFFFF);
	}

	public int nameplateOwnNameArgb() {
		return parseHexColor(nameplateOwnNameColor, 0xFF42E8C8);
	}

	public int scoreboardBackgroundArgb() {
		return colorWithAlpha(scoreboardBackgroundColor, scoreboardBackgroundAlpha, 0x60000000);
	}

	public int overlayArgb(int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (parseHexColor(overlayColor, 0xFFE06A4D) & 0x00FFFFFF);
	}

	private void syncLegacyFieldsToFeatureToggles() {
		featureToggles.put("core.enabled", enabled);
		featureToggles.put("core.only_skyblock", onlyOnSkyBlock);
		featureToggles.put("tooltip_tweaks.enhanced_item_tooltips", enhancedTooltips);
		featureToggles.put("itemlist.local_browser", itemBrowser);
		featureToggles.put("notifications.chat_filters", chatFilters);
		featureToggles.put("notifications.chat_highlights", chatHighlights);
		featureToggles.put("api.enabled", apiEnabled);
	}

	private void syncFeatureToggleToLegacyField(String id, boolean value) {
		switch (id) {
			case "core.enabled" -> enabled = value;
			case "core.only_skyblock" -> onlyOnSkyBlock = value;
			case "tooltip_tweaks.enhanced_item_tooltips" -> enhancedTooltips = value;
			case "itemlist.local_browser" -> itemBrowser = value;
			case "notifications.chat_filters" -> chatFilters = value;
			case "notifications.chat_highlights" -> chatHighlights = value;
			case "api.enabled" -> apiEnabled = value;
			default -> {
			}
		}
	}

	private static String normalizeHex(String value, String fallback) {
		if (value == null) {
			return fallback;
		}
		String trimmed = value.trim();
		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.length() != 6) {
			return fallback;
		}
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			boolean hex = (c >= '0' && c <= '9')
					|| (c >= 'a' && c <= 'f')
					|| (c >= 'A' && c <= 'F');
			if (!hex) {
				return fallback;
			}
		}
		return "#" + trimmed.toUpperCase(Locale.ROOT);
	}

	private static List<String> normalizeTerms(List<String> values, List<String> fallback) {
		List<String> source = values == null ? fallback : values;
		Map<String, String> normalized = new LinkedHashMap<>();
		for (String value : source) {
			if (value == null) {
				continue;
			}
			String trimmed = value.trim().replaceAll("\\s+", " ");
			if (trimmed.isBlank() || "example-blocked-term".equalsIgnoreCase(trimmed)) {
				continue;
			}
			if (trimmed.length() > 80) {
				trimmed = trimmed.substring(0, 80);
			}
			normalized.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
		}
		return new ArrayList<>(normalized.values());
	}

	public static String sanitizeText(String value, String fallback, int maxLength) {
		String result = value == null ? fallback : value.trim().replaceAll("\\s+", " ");
		if (result == null) {
			result = "";
		}
		if (result.length() > maxLength) {
			result = result.substring(0, maxLength);
		}
		return result;
	}

	private void normalizeInventoryCommands() {
		if (inventoryCommandButtons == null) {
			inventoryCommandButtons = new ArrayList<>();
			return;
		}
		Map<String, InventoryCommandButtonConfig> unique = new LinkedHashMap<>();
		int index = 1;
		for (InventoryCommandButtonConfig button : inventoryCommandButtons) {
			if (button == null) {
				continue;
			}
			button.normalize(index);
			unique.putIfAbsent(button.id, button);
			index++;
		}
		inventoryCommandButtons = new ArrayList<>(unique.values());
	}

	public static int parseHexColor(String value, int fallback) {
		String normalized = normalizeHex(value, "");
		if (normalized.isBlank()) {
			return fallback;
		}
		return 0xFF000000 | Integer.parseInt(normalized.substring(1), 16);
	}

	public static int colorWithAlpha(String value, int alpha, int fallback) {
		int rgb = parseHexColor(value, fallback) & 0x00FFFFFF;
		return (clampAlpha(alpha) << 24) | rgb;
	}

	private static int clampAlpha(int value) {
		return Math.max(0, Math.min(255, value));
	}

	public static final class HudModuleConfig {
		public boolean enabled;
		public int x;
		public int y;
		public double scale = 1.0D;
		public int width;
		public int height;

		public HudModuleConfig() {
		}

		public HudModuleConfig(boolean enabled, int x, int y) {
			this.enabled = enabled;
			this.x = x;
			this.y = y;
			this.scale = 1.0D;
			this.width = 0;
			this.height = 0;
		}
	}

	public static final class InventoryCommandButtonConfig {
		public String id = "";
		public boolean enabled = true;
		public int x = 178;
		public int y = 30;
		public int width = 74;
		public int height = 18;
		public String label = "Command";
		public String command = "";
		public String icon = "/";
		public String textColor = "#FFFFFF";
		public String backgroundColor = "#333A35";
		public int backgroundAlpha = 190;

		public InventoryCommandButtonConfig() {
		}

		public InventoryCommandButtonConfig(String id, int x, int y) {
			this.id = id;
			this.x = x;
			this.y = y;
		}

		private void normalize(int index) {
			if (id == null || id.isBlank()) {
				id = "command_" + index;
			}
			id = id.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
			enabled = true;
			x = Math.max(-220, Math.min(280, x));
			y = Math.max(-120, Math.min(220, y));
			width = Math.max(34, Math.min(160, width));
			height = Math.max(16, Math.min(42, height));
			label = sanitizeText(label, "Command", 32);
			command = sanitizeText(command, "", 120);
			icon = sanitizeText(icon, "/", 48);
			textColor = normalizeHex(textColor, "#FFFFFF");
			backgroundColor = normalizeHex(backgroundColor, "#333A35");
			backgroundAlpha = clampAlpha(backgroundAlpha);
		}

		private static String sanitizeText(String value, String fallback, int maxLength) {
			String result = SkyBlockLensConfig.sanitizeText(value, fallback, maxLength);
			if (result.isBlank()) {
				result = fallback;
			}
			return result;
		}
	}
}
