package com.skyblocklens.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skyblocklens.SkyBlockLensClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class SblI18n {
	private static final Gson GSON = new Gson();
	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
	}.getType();

	private String language;
	private Map<String, String> translations = new HashMap<>();

	public SblI18n(String language) {
		setLanguage(language);
	}

	public String language() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = "ru_ru".equals(language) ? "ru_ru" : "en_us";
		this.translations = load(this.language);
	}

	public String tr(String key) {
		return translations.getOrDefault(key, key);
	}

	public String trState(boolean enabled) {
		return tr(enabled ? "skyblocklens.config.enabled" : "skyblocklens.config.disabled");
	}

	private static Map<String, String> load(String language) {
		String path = "/assets/" + SkyBlockLensClient.MOD_ID + "/lang/" + language + ".json";
		try (InputStream stream = SblI18n.class.getResourceAsStream(path)) {
			if (stream == null) {
				return Map.of();
			}
			try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
				return loaded == null ? Map.of() : loaded;
			}
		} catch (Exception error) {
			SkyBlockLensClient.LOGGER.warn("Failed to load language file {}.", language, error);
			return Map.of();
		}
	}
}
