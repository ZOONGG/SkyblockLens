package com.skyblocklens.chat;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.ConfigStore;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.i18n.SblI18n;
import com.skyblocklens.notifications.NotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

public final class ChatController {
	private final ConfigStore configStore;
	private final SblI18n i18n;
	private final NotificationManager notificationManager;

	public ChatController(ConfigStore configStore, SblI18n i18n, NotificationManager notificationManager) {
		this.configStore = configStore;
		this.i18n = i18n;
		this.notificationManager = notificationManager;
	}

	public boolean allowChatMessage(Text message) {
		SkyBlockLensConfig config = configStore.config();
		if (!config.enabled || message == null || !SkyBlockLensClient.skyBlockFeaturesAllowed()) {
			return true;
		}
		String plain = message.getString();
		if (config.chatFilters && config.featureEnabled("notifications.chat_filters")
				&& containsAny(plain, config.blockedChatTerms)) {
			return false;
		}
		NotificationMatch notification = matchNotification(config, plain);
		if (notification != null) {
			notificationManager.push(notification.titleKey(), plain);
		}
		if (config.chatHighlights && config.featureEnabled("notifications.chat_highlights")
				&& (containsAny(plain, config.highlightChatTerms) || notification != null)) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.inGameHud != null) {
				client.inGameHud.getChatHud().addMessage(highlight(message));
				return false;
			}
		}
		return true;
	}

	public Text modifyGameMessage(Text message, boolean overlay) {
		SkyBlockLensConfig config = configStore.config();
		if (!config.enabled || !config.chatHighlights || !config.featureEnabled("notifications.chat_highlights")
				|| message == null || !SkyBlockLensClient.skyBlockFeaturesAllowed()) {
			return message;
		}
		String plain = message.getString();
		NotificationMatch notification = matchNotification(config, plain);
		if (notification != null) {
			notificationManager.push(notification.titleKey(), plain);
		}
		if (containsAny(plain, config.highlightChatTerms) || notification != null) {
			return highlight(message);
		}
		return message;
	}

	private MutableText highlight(Text message) {
		return Text.literal(i18n.tr("skyblocklens.chat.highlight_prefix"))
				.formatted(Formatting.GOLD)
				.append(message.copy().formatted(Formatting.YELLOW));
	}

	private static boolean containsAny(String value, Iterable<String> terms) {
		String lower = value.toLowerCase(Locale.ROOT);
		for (String term : terms) {
			if (term != null && !term.isBlank() && lower.contains(term.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private static NotificationMatch matchNotification(SkyBlockLensConfig config, String value) {
		if (!config.featureEnabled("notifications.chat_highlights") || value == null) {
			return null;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		if (config.featureEnabled("notifications.rare_drop") && (lower.contains("rare drop")
				|| lower.contains("rng meter") || lower.contains("crazy rare drop"))) {
			return new NotificationMatch("skyblocklens.notification.rare_drop");
		}
		if (config.featureEnabled("notifications.special_zealot") && lower.contains("special zealot")) {
			return new NotificationMatch("skyblocklens.notification.special_zealot");
		}
		if (config.featureEnabled("notifications.slayer_complete") && (lower.contains("slayer quest complete")
				|| lower.contains("boss slain"))) {
			return new NotificationMatch("skyblocklens.notification.slayer_complete");
		}
		if (config.featureEnabled("notifications.dungeon_score") && (lower.contains("score:")
				|| lower.contains("dungeon score") || lower.contains("grade:"))) {
			return new NotificationMatch("skyblocklens.notification.dungeon_score");
		}
		if (config.featureEnabled("notifications.market") && (lower.contains("auction")
				|| lower.contains("bazaar") || lower.contains("buy order") || lower.contains("sell offer"))) {
			return new NotificationMatch("skyblocklens.notification.market");
		}
		return null;
	}

	private record NotificationMatch(String titleKey) {
	}
}
