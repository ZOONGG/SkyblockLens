package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class InventoryButtonController {
	private static UiRect itemButton = new UiRect(0, 0, 0, 0);
	private static final Map<String, Double> hoverAnimations = new HashMap<>();

	private InventoryButtonController() {
	}

	public static void render(DrawContext context, int mouseX, int mouseY, int screenX, int screenY) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!config.enabled || !config.featureEnabled("inventory_buttons.enable")
				|| !config.featureEnabled("inventory_buttons.item_browser")) {
			itemButton = new UiRect(0, 0, 0, 0);
			return;
		}
		int x = screenX + config.inventoryItemButtonX;
		int y = screenY + config.inventoryItemButtonY;
		itemButton = new UiRect(x, y, config.inventoryItemButtonWidth, config.inventoryItemButtonHeight);
		drawButton(context, itemButton, SkyBlockLensClient.i18n().tr("skyblocklens.inventory_buttons.items"),
				"minecraft:emerald", config.accentArgb(), config.inventoryButtonBackgroundArgb(), 0xFFF3F2E8,
				mouseX, mouseY, "item");
	}

	public static boolean mouseClicked(Click click, int screenX, int screenY) {
		if (click.button() != 0 || !itemButton.contains(click.x(), click.y())) {
			return false;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!config.enabled || !config.featureEnabled("inventory_buttons.enable")
				|| !config.featureEnabled("inventory_buttons.item_browser")) {
			return false;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(new ItemBrowserScreen(client.currentScreen));
		return true;
	}

	public static ItemStack iconStack(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			Item item = Registries.ITEM.get(Identifier.of(itemId));
			return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		} catch (RuntimeException ignored) {
			return ItemStack.EMPTY;
		}
	}

	private static void drawButton(
			DrawContext context,
			UiRect rect,
			String label,
			String icon,
			int accent,
			int background,
			int textColor,
			int mouseX,
			int mouseY,
			String id
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		double hover = hoverAnimation(id, rect.contains(mouseX, mouseY));
		int lift = (int) Math.round(hover);
		UiRect drawRect = new UiRect(rect.x(), rect.y() - lift, rect.width(), rect.height());
		context.fill(drawRect.x(), drawRect.y(), drawRect.right(), drawRect.bottom(), blend(background, 0xFF33454C, hover * 0.75D));
		context.fill(drawRect.x() + 1, drawRect.y() + 1, drawRect.right() - 1,
				drawRect.y() + Math.max(2, drawRect.height() / 2), blend(0x33202B32, 0x66465C64, hover));
		context.fill(drawRect.x(), drawRect.y(), drawRect.right(), drawRect.y() + 1, accent);
		drawBorder(context, drawRect, blend(0xFF050709, accent, hover * 0.5D));
		ItemStack stack = iconStack(icon);
		int textX = drawRect.x() + 4;
		int textWidth = drawRect.width() - 8;
		if (!stack.isEmpty() && drawRect.width() >= 32 && drawRect.height() >= 16) {
			int itemY = drawRect.y() + Math.max(1, (drawRect.height() - 16) / 2);
			context.drawItem(stack, drawRect.x() + 3, itemY);
			textX += 18;
			textWidth -= 18;
		}
		String fitted = fit(label, textWidth);
		context.drawTextWithShadow(client.textRenderer, Text.literal(fitted),
				textX + Math.max(0, (textWidth - client.textRenderer.getWidth(fitted)) / 2),
				drawRect.y() + Math.max(4, (drawRect.height() - 8) / 2), textColor);
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

	private static void drawBorder(DrawContext context, UiRect rect, int color) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
		context.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
		context.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
		context.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
	}

	private static double hoverAnimation(String key, boolean hover) {
		double current = hoverAnimations.getOrDefault(key, hover ? 1.0D : 0.0D);
		double target = hover ? 1.0D : 0.0D;
		current += (target - current) * 0.35D;
		if (Math.abs(target - current) < 0.02D) {
			current = target;
		}
		hoverAnimations.put(key, current);
		return current;
	}

	private static int blend(int from, int to, double progress) {
		double t = Math.max(0.0D, Math.min(1.0D, progress));
		int a = (int) Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = (int) Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
		int g = (int) Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
		int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
