package com.skyblocklens.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyblocklens.SkyBlockLensClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path = FabricLoader.getInstance().getConfigDir().resolve("skyblocklens.json");
	private SkyBlockLensConfig config = SkyBlockLensConfig.defaults();

	public SkyBlockLensConfig config() {
		return config;
	}

	public void load() {
		if (!Files.exists(path)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			SkyBlockLensConfig loaded = GSON.fromJson(reader, SkyBlockLensConfig.class);
			if (loaded != null) {
				config = loaded;
			}
		} catch (IOException | RuntimeException error) {
			SkyBlockLensClient.LOGGER.warn("Failed to load config, using defaults.", error);
			config = SkyBlockLensConfig.defaults();
		}
		config.normalize();
		save();
	}

	public void save() {
		try {
			Files.createDirectories(path.getParent());
			config.normalize();
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException error) {
			SkyBlockLensClient.LOGGER.warn("Failed to save config.", error);
		}
	}

	public void reset() {
		config = SkyBlockLensConfig.defaults();
		save();
		SkyBlockLensClient.reloadLanguage();
	}
}
