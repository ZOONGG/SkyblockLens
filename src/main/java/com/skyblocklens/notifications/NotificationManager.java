package com.skyblocklens.notifications;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.ConfigStore;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.i18n.SblI18n;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class NotificationManager {
	private static final int MAX_VISIBLE = 4;
	private static final int WIDTH = 214;

	private final ConfigStore configStore;
	private final SblI18n i18n;
	private final Deque<Entry> entries = new ArrayDeque<>();
	private String lastBody = "";
	private long lastPushMillis;

	public NotificationManager(ConfigStore configStore, SblI18n i18n) {
		this.configStore = configStore;
		this.i18n = i18n;
	}

	public void push(String titleKey, String body) {
		push(titleKey, body, configStore.config().accentArgb());
	}

	public void push(String titleKey, String body, int color) {
		SkyBlockLensConfig config = configStore.config();
		if (!config.enabled || body == null || body.isBlank()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (body.equals(lastBody) && now - lastPushMillis < 1000L) {
			return;
		}
		lastBody = body;
		lastPushMillis = now;
		entries.addFirst(new Entry(i18n.tr(titleKey), body, color, now));
		while (entries.size() > 8) {
			entries.removeLast();
		}
	}

	public void render(DrawContext context) {
		SkyBlockLensConfig config = configStore.config();
		if (!config.enabled || !SkyBlockLensClient.skyBlockFeaturesAllowed()) {
			entries.clear();
			return;
		}
		long now = System.currentTimeMillis();
		long duration = config.notificationDurationSeconds * 1000L;
		entries.removeIf(entry -> now - entry.createdMillis > duration);
		if (entries.isEmpty()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		int x = Math.max(8, client.getWindow().getScaledWidth() - WIDTH - 10);
		int y = 12;
		int drawn = 0;
		Iterator<Entry> iterator = entries.iterator();
		while (iterator.hasNext() && drawn < MAX_VISIBLE) {
			Entry entry = iterator.next();
			int age = (int) (now - entry.createdMillis);
			int alpha = age > duration - 500L ? Math.max(70, (int) ((duration - age) / 500.0D * 220.0D)) : 220;
			int bg = (alpha << 24) | 0x111820;
			int border = (0xFF << 24) | (entry.color & 0x00FFFFFF);
			int h = 42;
			context.fill(x, y, x + WIDTH, y + h, bg);
			context.fill(x, y, x + WIDTH, y + 1, border);
			context.fill(x, y + h - 1, x + WIDTH, y + h, 0xFF05080A);
			context.fill(x, y, x + 1, y + h, 0xFF05080A);
			context.fill(x + WIDTH - 1, y, x + WIDTH, y + h, 0xFF05080A);
			context.drawTextWithShadow(client.textRenderer, Text.literal(fit(entry.title, WIDTH - 14)), x + 7, y + 7, border);
			context.drawTextWithShadow(client.textRenderer, Text.literal(fit(entry.body, WIDTH - 14)), x + 7, y + 22, 0xFFECE6D8);
			y += h + 6;
			drawn++;
		}
	}

	private static String fit(String text, int maxWidth) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		int suffixWidth = client.textRenderer.getWidth(suffix);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			String next = result.toString() + text.charAt(i);
			if (client.textRenderer.getWidth(next) + suffixWidth > maxWidth) {
				break;
			}
			result.append(text.charAt(i));
		}
		return result + suffix;
	}

	private record Entry(String title, String body, int color, long createdMillis) {
	}
}
