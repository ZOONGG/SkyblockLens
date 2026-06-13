package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class NameplateController {
	private NameplateController() {
	}

	public static boolean shouldShowOwnName(PlayerLikeEntity player) {
		if (player == null) {
			return false;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null
				&& player == client.player
				&& SkyBlockLensClient.configStore().config().enabled
				&& SkyBlockLensClient.configStore().config().featureEnabled("misc.nameplates.enable")
				&& SkyBlockLensClient.configStore().config().featureEnabled("misc.nameplates.show_self");
	}

	public static Text overrideOwnNameLabel(Text label) {
		if (label == null || !nameplateStylingEnabled()) {
			return label;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return label;
		}
		String playerName = client.player.getName().getString();
		String raw = label.getString();
		if (playerName.isBlank() || raw.isBlank() || !raw.contains(playerName)) {
			return label;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		String replacement = config.nameplateOwnAlias == null || config.nameplateOwnAlias.isBlank()
				? playerName
				: config.nameplateOwnAlias;
		int color = config.nameplateOwnNameArgb() & 0x00FFFFFF;
		return Text.literal(raw.replace(playerName, replacement))
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
	}

	public static int adjustBackgroundColor(int original) {
		if (!nameplateStylingEnabled()) {
			return original;
		}
		return SkyBlockLensClient.configStore().config().nameplateBackgroundArgb();
	}

	public static int adjustTextColor(int original) {
		if (!nameplateStylingEnabled()) {
			return original;
		}
		return SkyBlockLensClient.configStore().config().nameplateTextArgb();
	}

	private static boolean nameplateStylingEnabled() {
		return SkyBlockLensClient.configStore().config().enabled
				&& SkyBlockLensClient.configStore().config().featureEnabled("misc.nameplates.enable");
	}
}
