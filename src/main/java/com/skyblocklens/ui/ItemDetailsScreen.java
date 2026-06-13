package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.items.SkyBlockItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class ItemDetailsScreen extends Screen {
	private static final int PANEL_W = 376;
	private static final int PANEL_H = 276;

	private final Screen parent;
	private final SkyBlockItem item;
	private final List<ActionTarget> buttons = new ArrayList<>();
	private ViewMode mode = ViewMode.INFO;
	private int scroll;
	private int maxScroll;
	private String feedback = "";
	private long feedbackUntilMillis;

	public ItemDetailsScreen(Screen parent, SkyBlockItem item) {
		super(Text.literal(item == null ? "" : item.name));
		this.parent = parent;
		this.item = item;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x88000000);
		buttons.clear();
		UiRect panel = panel();
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		context.fill(panel.x() + 4, panel.y() + 4, panel.right() + 4, panel.bottom() + 4, 0x66000000);
		context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF0111820);
		context.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + 32, 0xF01B252D);
		drawBorder(context, panel, config.accentArgb());
		if (item == null) {
			drawCentered(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.no_results"),
					panel.centerX(), panel.y() + panel.height() / 2, 0xFFAAB7C4);
			return;
		}

		ItemStack stack = SkyBlockLensClient.itemRepository().iconStack(item);
		if (!stack.isEmpty()) {
			context.drawItem(stack, panel.x() + 12, panel.y() + 10);
		}
		context.drawTextWithShadow(textRenderer, Text.literal(fit(item.name, panel.width() - 74)),
				panel.x() + 34, panel.y() + 9, rarityColor(item.rarity, config.accentArgb()));
		context.drawTextWithShadow(textRenderer, Text.literal(fit(item.id, panel.width() - 74)),
				panel.x() + 34, panel.y() + 21, 0xFF8FA3AF);
		drawCloseButton(context, new UiRect(panel.right() - 24, panel.y() + 7, 16, 16), mouseX, mouseY);

		int buttonY = panel.bottom() - 54;
		int buttonX = panel.x() + 12;
		buttonX = addButton(context, buttonX, buttonY, 70, "skyblocklens.items.action.recipe", Action.RECIPE, mouseX, mouseY);
		buttonX = addButton(context, buttonX, buttonY, 70, "skyblocklens.items.action.usages", Action.USAGES, mouseX, mouseY);
		buttonX = addButton(context, buttonX, buttonY, 66, "skyblocklens.items.action.wiki", Action.WIKI, mouseX, mouseY);
		buttonX = addButton(context, buttonX, buttonY, 58, "skyblocklens.items.action.ah", Action.AH, mouseX, mouseY);
		addButton(context, buttonX, buttonY, 58, "skyblocklens.items.action.bz", Action.BZ, mouseX, mouseY);

		buttonX = panel.x() + 12;
		buttonY = panel.bottom() - 28;
		buttonX = addButton(context, buttonX, buttonY, 104, "skyblocklens.items.action.copy", Action.COPY, mouseX, mouseY);
		addButton(context, buttonX, buttonY, 72, "skyblocklens.items.action.back", Action.BACK, mouseX, mouseY);

		UiRect content = new UiRect(panel.x() + 12, panel.y() + 44, panel.width() - 24, panel.height() - 106);
		drawContent(context, content);
		drawFeedback(context, panel);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() != 0) {
			return super.mouseClicked(click, doubled);
		}
		UiRect close = new UiRect(panel().right() - 24, panel().y() + 7, 16, 16);
		if (close.contains(click.x(), click.y())) {
			close();
			return true;
		}
		for (ActionTarget target : buttons) {
			if (target.bounds().contains(click.x(), click.y())) {
				perform(target.action());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		UiRect content = new UiRect(panel().x() + 12, panel().y() + 44, panel().width() - 24, panel().height() - 106);
		if (content.contains(mouseX, mouseY)) {
			scroll = MathHelper.clamp(scroll - (int) Math.round(verticalAmount * 24.0D), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void drawContent(DrawContext context, UiRect content) {
		int y = content.y() - scroll;
		int startY = y;
		context.enableScissor(content.x(), content.y(), content.right(), content.bottom());
		y = drawMeta(context, content, y);
		if (mode == ViewMode.INFO) {
			y = drawInfo(context, content, y);
		} else if (mode == ViewMode.RECIPE) {
			y = drawRecipe(context, content, y);
		} else {
			y = drawUsages(context, content, y);
		}
		context.disableScissor();
		maxScroll = Math.max(0, y - startY - content.height() + 10);
		scroll = MathHelper.clamp(scroll, 0, maxScroll);
		drawScrollbar(context, content.right() - 4, content.y(), content.bottom(), scroll, maxScroll);
	}

	private int drawMeta(DrawContext context, UiRect content, int y) {
		String meta = item.rarity;
		if (!item.category.isBlank()) {
			meta += meta.isBlank() ? item.category : " / " + item.category;
		}
		if (!meta.isBlank()) {
			context.drawTextWithShadow(textRenderer, Text.literal(fit(meta, content.width() - 10)), content.x(), y, 0xFFB7C9D1);
			y += 15;
		}
		if (!item.npcSellPrice.isBlank()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.npc_sell") + ": "
					+ item.npcSellPrice), content.x(), y, 0xFFE7A743);
			y += 13;
		}
		if (!item.bazaarInfo.isBlank()) {
			context.drawTextWithShadow(textRenderer, Text.literal(fit(SkyBlockLensClient.i18n().tr("skyblocklens.items.bazaar") + ": "
					+ item.bazaarInfo, content.width() - 10)), content.x(), y, 0xFF65E6A2);
			y += 13;
		}
		if (!item.auctionInfo.isBlank()) {
			context.drawTextWithShadow(textRenderer, Text.literal(fit(SkyBlockLensClient.i18n().tr("skyblocklens.items.auction") + ": "
					+ item.auctionInfo, content.width() - 10)), content.x(), y, 0xFF65E6A2);
			y += 13;
		}
		return y + 4;
	}

	private int drawInfo(DrawContext context, UiRect content, int y) {
		if (!item.description.isBlank()) {
			y = drawWrapped(context, item.description, content.x(), y, content.width() - 8, 0xFFE6F4FF, 5) + 8;
		}
		if (!item.lore.isEmpty()) {
			drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.lore"), content.x(), y);
			y += 14;
			for (String line : item.lore) {
				String value = line.isBlank() ? " " : line;
				context.drawTextWithShadow(textRenderer, Text.literal(fit(value, content.width() - 12)),
						content.x() + 6, y, 0xFFD8D3C6);
				y += 11;
			}
			y += 5;
		}
		return drawSources(context, content, y);
	}

	private int drawRecipe(DrawContext context, UiRect content, int y) {
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.recipe"), content.x(), y);
		y += 14;
		if (!hasRecipeSlots(item.recipeSlots)) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.recipe_unavailable")),
					content.x() + 6, y, 0xFF8494A1);
			return y + 18;
		}
		int gridX = content.x() + 10;
		int gridY = y + 3;
		int slot = 20;
		for (int index = 0; index < 9; index++) {
			int col = index % 3;
			int row = index / 3;
			drawCraftSlot(context, gridX + col * slot, gridY + row * slot, item.recipeSlots.get(index));
		}
		int arrowX = gridX + slot * 3 + 14;
		int arrowY = gridY + 24;
		context.fill(arrowX, arrowY, arrowX + 20, arrowY + 2, 0xFFB7C9D1);
		context.fill(arrowX + 16, arrowY - 4, arrowX + 18, arrowY + 6, 0xFFB7C9D1);
		context.fill(arrowX + 18, arrowY - 2, arrowX + 20, arrowY + 4, 0xFFB7C9D1);
		drawOutputSlot(context, arrowX + 34, gridY + 20);
		return y + 72;
	}

	private int drawUsages(DrawContext context, UiRect content, int y) {
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.used_in"), content.x(), y);
		y += 14;
		List<SkyBlockItem> usages = SkyBlockLensClient.itemRepository().itemsUsing(item, 64);
		if (usages.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.uses_unavailable")),
					content.x() + 6, y, 0xFF8494A1);
			return y + 18;
		}
		for (SkyBlockItem usage : usages) {
			context.drawTextWithShadow(textRenderer, Text.literal("- " + fit(usage.name, content.width() - 18)),
					content.x() + 6, y, 0xFFD7FBE8);
			y += 12;
		}
		return y + 5;
	}

	private int drawSources(DrawContext context, UiRect content, int y) {
		if (item.sources.isEmpty()) {
			return y;
		}
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.sources"), content.x(), y);
		y += 14;
		for (String source : item.sources) {
			context.drawTextWithShadow(textRenderer, Text.literal("- " + fit(source, content.width() - 18)),
					content.x() + 6, y, 0xFF8494A1);
			y += 12;
		}
		return y + 5;
	}

	private void perform(Action action) {
		switch (action) {
			case RECIPE -> {
				mode = ViewMode.RECIPE;
				scroll = 0;
			}
			case USAGES -> {
				mode = ViewMode.USAGES;
				scroll = 0;
			}
			case WIKI -> openWiki();
			case AH -> sendCommand("ah " + item.name);
			case BZ -> sendCommand("bz " + item.name);
			case COPY -> {
				MinecraftClient.getInstance().keyboard.setClipboard(item.id);
				showFeedback(SkyBlockLensClient.i18n().tr("skyblocklens.items.copied"));
			}
			case BACK -> close();
		}
	}

	private void openWiki() {
		String url = item.wikiUrl == null || item.wikiUrl.isBlank()
				? "https://wiki.hypixel.net/" + item.name.trim().replace(" ", "_")
				: item.wikiUrl;
		try {
			Util.getOperatingSystem().open(url);
		} catch (RuntimeException ignored) {
			showFeedback(url);
		}
	}

	private void sendCommand(String rawCommand) {
		String command = rawCommand == null ? "" : rawCommand.trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (!command.isBlank() && client.player != null && client.player.networkHandler != null) {
			client.player.networkHandler.sendChatCommand(command);
		}
	}

	private int addButton(DrawContext context, int x, int y, int w, String key, Action action, int mouseX, int mouseY) {
		UiRect rect = new UiRect(x, y, w, 20);
		drawSmallButton(context, rect, SkyBlockLensClient.i18n().tr(key), mouseX, mouseY,
				action == Action.RECIPE && mode == ViewMode.RECIPE || action == Action.USAGES && mode == ViewMode.USAGES);
		buttons.add(new ActionTarget(rect, action));
		return rect.right() + 6;
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY, boolean active) {
		boolean hover = rect.contains(mouseX, mouseY);
		int bg = active ? 0xFF24443F : hover ? 0xFF31424C : 0xFF26323A;
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), bg);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + rect.height() / 2,
				hover ? 0xFF3E5660 : 0xFF35464E);
		drawBorder(context, rect, active ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF070A0C);
		String fitted = fit(label, rect.width() - 8);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				rect.centerX() - textRenderer.getWidth(fitted) / 2, rect.y() + 6, 0xFFF3F2E8);
	}

	private void drawCloseButton(DrawContext context, UiRect rect, int mouseX, int mouseY) {
		boolean hover = rect.contains(mouseX, mouseY);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hover ? 0xFF3E2222 : 0xFF26323A);
		drawBorder(context, rect, hover ? 0xFFE06A4D : 0xFF070A0C);
		context.fill(rect.x() + 4, rect.y() + 4, rect.x() + 5, rect.y() + 5, 0xFFE6F4FF);
		context.fill(rect.x() + 5, rect.y() + 5, rect.x() + 6, rect.y() + 6, 0xFFE6F4FF);
		context.fill(rect.x() + 6, rect.y() + 6, rect.x() + 7, rect.y() + 7, 0xFFE6F4FF);
		context.fill(rect.x() + 10, rect.y() + 4, rect.x() + 11, rect.y() + 5, 0xFFE6F4FF);
		context.fill(rect.x() + 9, rect.y() + 5, rect.x() + 10, rect.y() + 6, 0xFFE6F4FF);
		context.fill(rect.x() + 8, rect.y() + 6, rect.x() + 9, rect.y() + 7, 0xFFE6F4FF);
		context.fill(rect.x() + 4, rect.y() + 10, rect.x() + 5, rect.y() + 11, 0xFFE6F4FF);
		context.fill(rect.x() + 5, rect.y() + 9, rect.x() + 6, rect.y() + 10, 0xFFE6F4FF);
		context.fill(rect.x() + 6, rect.y() + 8, rect.x() + 7, rect.y() + 9, 0xFFE6F4FF);
		context.fill(rect.x() + 10, rect.y() + 10, rect.x() + 11, rect.y() + 11, 0xFFE6F4FF);
		context.fill(rect.x() + 9, rect.y() + 9, rect.x() + 10, rect.y() + 10, 0xFFE6F4FF);
		context.fill(rect.x() + 8, rect.y() + 8, rect.x() + 9, rect.y() + 9, 0xFFE6F4FF);
	}

	private void drawSectionTitle(DrawContext context, String title, int x, int y) {
		context.drawTextWithShadow(textRenderer, Text.literal(title), x, y, SkyBlockLensClient.configStore().config().accentArgb());
	}

	private static boolean hasRecipeSlots(List<String> slots) {
		if (slots == null || slots.size() < 9) {
			return false;
		}
		return slots.stream().anyMatch(slot -> slot != null && !slot.isBlank());
	}

	private void drawCraftSlot(DrawContext context, int x, int y, String token) {
		UiRect rect = new UiRect(x, y, 18, 18);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF7B7B7B);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFF9A9A9A);
		drawBorder(context, rect, 0xFF3A3A3A);
		ItemStack stack = SkyBlockLensClient.itemRepository().iconStackForToken(token);
		if (!stack.isEmpty()) {
			context.drawItem(stack, x + 1, y + 1);
			String count = tokenCount(token);
			if (!count.isBlank() && !"1".equals(count)) {
				context.drawTextWithShadow(textRenderer, Text.literal(count),
						x + 17 - textRenderer.getWidth(count), y + 10, 0xFFFFFFFF);
			}
		}
	}

	private void drawOutputSlot(DrawContext context, int x, int y) {
		UiRect rect = new UiRect(x, y, 20, 20);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF7B7B7B);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFFB5B5B5);
		drawBorder(context, rect, SkyBlockLensClient.configStore().config().accentArgb());
		ItemStack stack = SkyBlockLensClient.itemRepository().iconStack(item);
		if (!stack.isEmpty()) {
			context.drawItem(stack, x + 2, y + 2);
		}
	}

	private static String tokenCount(String token) {
		if (token == null) {
			return "";
		}
		int separator = token.lastIndexOf(':');
		if (separator <= 0 || separator == token.length() - 1) {
			return "";
		}
		String count = token.substring(separator + 1);
		for (int i = 0; i < count.length(); i++) {
			if (!Character.isDigit(count.charAt(i))) {
				return "";
			}
		}
		return count;
	}

	private int drawWrapped(DrawContext context, String text, int x, int y, int maxWidth, int color, int maxLines) {
		int line = 0;
		for (String part : wrap(text, maxWidth)) {
			if (line >= maxLines) {
				break;
			}
			context.drawTextWithShadow(textRenderer, Text.literal(part), x, y + line * 11, color);
			line++;
		}
		return y + line * 11;
	}

	private List<String> wrap(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (textRenderer.getWidth(candidate) <= maxWidth) {
				current = new StringBuilder(candidate);
				continue;
			}
			if (!current.isEmpty()) {
				lines.add(current.toString());
			}
			current = new StringBuilder(fit(word, maxWidth));
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines.isEmpty() ? List.of("") : lines;
	}

	private void drawScrollbar(DrawContext context, int x, int top, int bottom, int value, int max) {
		context.fill(x, top, x + 3, bottom, 0xFF070A0C);
		if (max <= 0) {
			return;
		}
		int height = bottom - top;
		int thumbHeight = Math.max(18, height * height / (height + max));
		int thumbY = top + (height - thumbHeight) * value / max;
		context.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, SkyBlockLensClient.configStore().config().accentArgb());
	}

	private void drawFeedback(DrawContext context, UiRect panel) {
		if (feedback.isBlank() || System.currentTimeMillis() > feedbackUntilMillis) {
			return;
		}
		context.drawTextWithShadow(textRenderer, Text.literal(fit(feedback, panel.width() - 24)),
				panel.x() + 12, panel.bottom() - 76, 0xFFE7A743);
	}

	private void showFeedback(String text) {
		feedback = text == null ? "" : text;
		feedbackUntilMillis = System.currentTimeMillis() + 1600L;
	}

	private UiRect panel() {
		int w = Math.min(PANEL_W, Math.max(280, width - 32));
		int h = Math.min(PANEL_H, Math.max(220, height - 32));
		return new UiRect((width - w) / 2, (height - h) / 2, w, h);
	}

	private void drawBorder(DrawContext context, UiRect rect, int color) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
		context.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
		context.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
		context.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
	}

	private void drawCentered(DrawContext context, String text, int centerX, int y, int color) {
		context.drawTextWithShadow(textRenderer, Text.literal(text), centerX - textRenderer.getWidth(text) / 2, y, color);
	}

	private String fit(String text, int maxWidth) {
		if (textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		int suffixWidth = textRenderer.getWidth(suffix);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			String next = result.toString() + text.charAt(i);
			if (textRenderer.getWidth(next) + suffixWidth > maxWidth) {
				break;
			}
			result.append(text.charAt(i));
		}
		return result + suffix;
	}

	private static int rarityColor(String rarity, int fallback) {
		return switch (rarity == null ? "" : rarity) {
			case "COMMON" -> 0xFFFFFFFF;
			case "UNCOMMON" -> 0xFF55FF55;
			case "RARE" -> 0xFF5555FF;
			case "EPIC" -> 0xFFAA00AA;
			case "LEGENDARY" -> 0xFFFFAA00;
			case "MYTHIC" -> 0xFFFF55FF;
			case "DIVINE", "SPECIAL", "VERY_SPECIAL" -> 0xFFFF5555;
			default -> fallback;
		};
	}

	private enum ViewMode {
		INFO,
		RECIPE,
		USAGES
	}

	private enum Action {
		RECIPE,
		USAGES,
		WIKI,
		AH,
		BZ,
		COPY,
		BACK
	}

	private record ActionTarget(UiRect bounds, Action action) {
	}
}
