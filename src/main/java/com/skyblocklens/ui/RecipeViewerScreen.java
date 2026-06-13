package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.items.ItemRepository;
import com.skyblocklens.items.SkyBlockItem;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class RecipeViewerScreen extends Screen {
	private static final int SLOT = 26;
	private static final int PANEL_W = 204;
	private static final int PANEL_H = 126;

	private final Screen parent;
	private final Deque<String> history = new ArrayDeque<>();
	private final List<RecipeTarget> targets = new ArrayList<>();
	private SkyBlockItem item;
	private UiRect closeButton = new UiRect(0, 0, 0, 0);
	private UiRect backButton = new UiRect(0, 0, 0, 0);

	public RecipeViewerScreen(Screen parent, SkyBlockItem item) {
		super(Text.literal(item == null ? "" : item.name));
		this.parent = parent;
		this.item = item;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x88000000);
		targets.clear();
		UiRect panel = panel();
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(panel.x() + 4, panel.y() + 4, panel.right() + 4, panel.bottom() + 4, 0x66000000);
		context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF0111820);
		drawBorder(context, panel, accent);
		closeButton = new UiRect(panel.right() - 22, panel.y() + 6, 16, 16);
		drawCloseButton(context, closeButton, mouseX, mouseY);
		backButton = history.isEmpty() ? new UiRect(0, 0, 0, 0) : new UiRect(panel.x() + 6, panel.y() + 6, 16, 16);
		if (!history.isEmpty()) {
			drawBackButton(context, backButton, mouseX, mouseY);
		}
		if (item == null) {
			drawCentered(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.no_results"), panel.centerX(),
					panel.y() + panel.height() / 2 - 4, 0xFFAAB7C4);
			super.render(context, mouseX, mouseY, delta);
			return;
		}
		drawRecipe(context, panel);
		drawHoverTooltip(context, mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (closeButton.contains(click.x(), click.y()) && click.button() == 0) {
			close();
			return true;
		}
		if (backButton.contains(click.x(), click.y()) && click.button() == 0) {
			goBack();
			return true;
		}
		for (RecipeTarget target : targets) {
			if (!target.bounds().contains(click.x(), click.y())) {
				continue;
			}
			if (click.button() == 1) {
				openWiki(target.item());
				return true;
			}
			if (click.button() == 0) {
				select(target.item());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
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

	private void drawRecipe(DrawContext context, UiRect panel) {
		if (!hasRecipeSlots(item.recipeSlots)) {
			drawCentered(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.recipe_unavailable"),
					panel.centerX(), panel.y() + panel.height() / 2 - 4, 0xFF8494A1);
			return;
		}
		int gridX = panel.x() + 18;
		int gridY = panel.y() + 34;
		for (int index = 0; index < 9; index++) {
			int col = index % 3;
			int row = index / 3;
			drawCraftSlot(context, gridX + col * SLOT, gridY + row * SLOT, item.recipeSlots.get(index));
		}
		int arrowX = gridX + SLOT * 3 + 12;
		int arrowY = gridY + SLOT + 8;
		context.fill(arrowX, arrowY, arrowX + 22, arrowY + 2, 0xFFB7C9D1);
		context.fill(arrowX + 17, arrowY - 5, arrowX + 20, arrowY + 7, 0xFFB7C9D1);
		context.fill(arrowX + 20, arrowY - 3, arrowX + 23, arrowY + 5, 0xFFB7C9D1);
		drawOutputSlot(context, arrowX + 34, gridY + SLOT - 4);
	}

	private void drawCraftSlot(DrawContext context, int x, int y, String token) {
		UiRect rect = new UiRect(x, y, 22, 22);
		drawSlotFrame(context, rect, 0xFF3A3A3A);
		ItemStack stack = SkyBlockLensClient.itemRepository().iconStackForToken(token);
		if (!stack.isEmpty()) {
			context.drawItem(stack, x + 3, y + 3);
			String count = tokenCount(token);
			if (!count.isBlank() && !"1".equals(count)) {
				context.drawTextWithShadow(textRenderer, Text.literal(count),
						x + 21 - textRenderer.getWidth(count), y + 13, 0xFFFFFFFF);
			}
		}
		SkyBlockLensClient.itemRepository().findById(ItemRepository.tokenId(token))
				.ifPresent(target -> targets.add(new RecipeTarget(rect, target)));
	}

	private void drawOutputSlot(DrawContext context, int x, int y) {
		UiRect rect = new UiRect(x, y, 24, 24);
		drawSlotFrame(context, rect, SkyBlockLensClient.configStore().config().accentArgb());
		ItemStack stack = SkyBlockLensClient.itemRepository().iconStack(item);
		if (!stack.isEmpty()) {
			context.drawItem(stack, x + 4, y + 4);
		}
		targets.add(new RecipeTarget(rect, item));
	}

	private void drawSlotFrame(DrawContext context, UiRect rect, int border) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF7B7B7B);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFF9A9A9A);
		drawBorder(context, rect, border);
	}

	private void drawHoverTooltip(DrawContext context, int mouseX, int mouseY) {
		for (RecipeTarget target : targets) {
			if (!target.bounds().contains(mouseX, mouseY)) {
				continue;
			}
			String label = target.item().name == null || target.item().name.isBlank() ? target.item().id : target.item().name;
			String fitted = fit(label, Math.min(240, width - 20));
			int tooltipW = textRenderer.getWidth(fitted) + 10;
			int x = MathHelper.clamp(mouseX + 10, 4, width - tooltipW - 4);
			int y = MathHelper.clamp(mouseY - 18, 4, height - 18);
			context.fill(x + 2, y + 2, x + tooltipW + 2, y + 17, 0x66000000);
			context.fill(x, y, x + tooltipW, y + 15, 0xF0101820);
			drawBorder(context, new UiRect(x, y, tooltipW, 15), SkyBlockLensClient.configStore().config().accentArgb());
			context.drawTextWithShadow(textRenderer, Text.literal(fitted), x + 5, y + 4, 0xFFF3F2E8);
			return;
		}
	}

	private void select(SkyBlockItem target) {
		if (target == null || item == null || target.id.equals(item.id)) {
			return;
		}
		history.push(item.id);
		while (history.size() > 24) {
			history.removeLast();
		}
		item = target;
	}

	private void goBack() {
		while (!history.isEmpty()) {
			SkyBlockItem previous = SkyBlockLensClient.itemRepository().findById(history.pop()).orElse(null);
			if (previous != null) {
				item = previous;
				return;
			}
		}
	}

	private void openWiki(SkyBlockItem target) {
		String url = target.wikiUrl == null || target.wikiUrl.isBlank()
				? "https://wiki.hypixel.net/" + target.name.trim().replace(" ", "_")
				: target.wikiUrl;
		try {
			Util.getOperatingSystem().open(url);
		} catch (RuntimeException ignored) {
			if (client != null) {
				client.keyboard.setClipboard(url);
			}
		}
	}

	private static boolean hasRecipeSlots(List<String> slots) {
		if (slots == null || slots.size() < 9) {
			return false;
		}
		return slots.stream().anyMatch(slot -> slot != null && !slot.isBlank());
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

	private void drawBackButton(DrawContext context, UiRect rect, int mouseX, int mouseY) {
		boolean hover = rect.contains(mouseX, mouseY);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hover ? 0xFF31424C : 0xFF26323A);
		drawBorder(context, rect, hover ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF070A0C);
		context.fill(rect.x() + 5, rect.y() + 7, rect.x() + 13, rect.y() + 9, 0xFFE6F4FF);
		context.fill(rect.x() + 4, rect.y() + 6, rect.x() + 7, rect.y() + 10, 0xFFE6F4FF);
	}

	private void drawCentered(DrawContext context, String text, int centerX, int y, int color) {
		String fitted = fit(text, PANEL_W - 24);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted), centerX - textRenderer.getWidth(fitted) / 2, y, color);
	}

	private UiRect panel() {
		int w = Math.min(PANEL_W, Math.max(170, width - 32));
		int h = Math.min(PANEL_H, Math.max(110, height - 32));
		return new UiRect((width - w) / 2, (height - h) / 2, w, h);
	}

	private void drawBorder(DrawContext context, UiRect rect, int color) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
		context.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
		context.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
		context.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
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

	private record RecipeTarget(UiRect bounds, SkyBlockItem item) {
	}
}
