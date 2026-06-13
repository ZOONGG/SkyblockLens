package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.items.SkyBlockItem;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ItemBrowserScreen extends Screen {
	private static final int PANEL_MARGIN = 12;
	private static final int ROW_HEIGHT = 25;
	private static final int MAX_RESULTS = 256;

	private final Screen parent;
	private final List<DetailLink> detailLinks = new ArrayList<>();
	private final Deque<String> history = new ArrayDeque<>();
	private TextFieldWidget search;
	private SkyBlockItem selected;
	private String searchValue = "";
	private int listScroll;
	private int maxListScroll;
	private UiRect historyBackButton;
	private UiRect backButton;

	public ItemBrowserScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.browser.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		UiRect searchBounds = searchBounds();
		search = new TextFieldWidget(
				textRenderer,
				searchBounds.x(),
				searchBounds.y(),
				searchBounds.width(),
				searchBounds.height(),
				Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.search"))
		);
		search.setPlaceholder(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.search")));
		search.setText(searchValue);
		search.setChangedListener(value -> {
			searchValue = value;
			listScroll = 0;
			selected = null;
			syncSelection(currentResults());
		});
		addDrawableChild(search);
		backButton = new UiRect(width - 92, height - 28, 80, 20);
		syncSelection(currentResults());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xDD070A10);
		drawShell(context);
		if (browserEnabled()) {
			List<SkyBlockItem> results = currentResults();
			syncSelection(results);
			renderList(context, results, mouseX, mouseY);
			renderDetail(context, mouseX, mouseY);
		} else {
			renderDisabled(context);
		}
		super.render(context, mouseX, mouseY, delta);
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (backButton != null && backButton.contains(click.x(), click.y()) && click.button() == 0) {
			close();
			return true;
		}
		if (historyBackButton != null && historyBackButton.contains(click.x(), click.y()) && click.button() == 0) {
			goBackInHistory();
			return true;
		}
		if (click.button() == 0 && browserEnabled()) {
			for (DetailLink link : detailLinks) {
				if (link.bounds().contains(click.x(), click.y())) {
					selectItem(link.item(), true);
					return true;
				}
			}
		}
		if (click.button() == 0 && browserEnabled()) {
			UiRect list = listBounds();
			if (list.contains(click.x(), click.y())) {
				List<SkyBlockItem> results = currentResults();
				int index = (int) ((click.y() - list.y() + listScroll) / ROW_HEIGHT);
				if (index >= 0 && index < results.size()) {
					selectItem(results.get(index), false);
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (browserEnabled() && listBounds().contains(mouseX, mouseY)) {
			listScroll = MathHelper.clamp(listScroll - (int) Math.round(verticalAmount * ROW_HEIGHT), 0, maxListScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (keyInput.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && search != null && !search.isFocused()
				&& !history.isEmpty()) {
			goBackInHistory();
			return true;
		}
		if (search != null && search.isFocused() && search.keyPressed(keyInput)) {
			return true;
		}
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (search != null && search.isFocused() && search.charTyped(charInput)) {
			return true;
		}
		return super.charTyped(charInput);
	}

	@Override
	public void close() {
		SkyBlockLensClient.configStore().save();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void drawShell(DrawContext context) {
		UiRect listPanel = listPanelBounds();
		UiRect detailPanel = detailPanelBounds();
		context.fill(PANEL_MARGIN, PANEL_MARGIN, width - PANEL_MARGIN, height - 36, 0xFF111821);
		context.fill(listPanel.x(), listPanel.y(), listPanel.right(), listPanel.bottom(), 0xFF0A1018);
		context.fill(detailPanel.x(), detailPanel.y(), detailPanel.right(), detailPanel.bottom(), 0xFF151B23);
		drawBorder(context, listPanel, 0xFF2F3E46);
		drawBorder(context, detailPanel, 0xFF2F3E46);
		context.fill(detailPanel.x() + 1, detailPanel.y() + 1, detailPanel.right() - 1, detailPanel.y() + 28, 0xFF1D2830);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.browser.title")),
				detailPanel.x() + 12, detailPanel.y() + 10, SkyBlockLensClient.configStore().config().accentArgb());
	}

	private void renderList(DrawContext context, List<SkyBlockItem> results, int mouseX, int mouseY) {
		UiRect list = listBounds();
		int totalHeight = results.size() * ROW_HEIGHT;
		maxListScroll = Math.max(0, totalHeight - list.height());
		listScroll = MathHelper.clamp(listScroll, 0, maxListScroll);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.results")
				.replace("{count}", String.valueOf(results.size()))), list.x(), list.y() - 13, 0xFF9CB6C6);
		if (results.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.no_results")),
					list.x() + 6, list.y() + 8, 0xFFAAB7C4);
			return;
		}
		context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
		for (int index = 0; index < results.size(); index++) {
			SkyBlockItem item = results.get(index);
			int y = list.y() - listScroll + index * ROW_HEIGHT;
			if (y + ROW_HEIGHT < list.y() || y > list.bottom()) {
				continue;
			}
			boolean active = selected != null && selected.id.equals(item.id);
			boolean hovered = mouseX >= list.x() && mouseX <= list.right() && mouseY >= y && mouseY <= y + ROW_HEIGHT - 3;
			context.fill(list.x(), y, list.right() - 5, y + ROW_HEIGHT - 3,
					active ? 0xFF24443F : hovered ? 0xFF1F2A31 : 0xFF101720);
			context.drawTextWithShadow(textRenderer, Text.literal(fit(item.name, list.width() - 38)),
					list.x() + 18, y + 5, active ? 0xFF42E8C8 : 0xFFE6F4FF);
			String meta = item.rarity.isBlank() ? item.id : item.rarity;
			context.drawTextWithShadow(textRenderer, Text.literal(fit(meta, list.width() - 38)),
					list.x() + 18, y + 15, 0xFF8494A1);
		}
		context.disableScissor();
		drawScrollbar(context, list.right() - 4, list.y(), list.bottom(), listScroll, maxListScroll);
	}

	private void renderDetail(DrawContext context, int mouseX, int mouseY) {
		historyBackButton = null;
		detailLinks.clear();
		UiRect detail = detailContentBounds();
		if (selected == null) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.no_results")),
					detail.x(), detail.y(), 0xFFAAB7C4);
			return;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int y = detail.y();
		context.drawTextWithShadow(textRenderer, Text.literal(fit(selected.name, detail.width() - 130)), detail.x(), y, 0xFF8FD7FF);
		int buttonRight = detail.right() - 6;
		if (!history.isEmpty()) {
			String label = SkyBlockLensClient.i18n().tr("skyblocklens.items.history_back");
			historyBackButton = new UiRect(buttonRight - 74, y - 4, 74, 20);
			drawSmallButton(context, historyBackButton, label, mouseX, mouseY);
		}
		y += 16;
		String meta = detailMeta(selected);
		context.drawTextWithShadow(textRenderer, Text.literal(fit(meta, detail.width())), detail.x(), y, 0xFFAAB7C4);
		y += 18;
		if (!selected.description.isBlank()) {
			y = drawWrapped(context, selected.description, detail.x(), y, detail.width(), 0xFFE6F4FF, 3) + 10;
		}
		if (!selected.aliases.isEmpty()) {
			String aliases = SkyBlockLensClient.i18n().tr("skyblocklens.items.aliases") + ": " + String.join(", ", selected.aliases);
			context.drawTextWithShadow(textRenderer, Text.literal(fit(aliases, detail.width())), detail.x(), y, 0xFF8494A1);
			y += 16;
		}
		if (config.featureEnabled("itemlist.recipe_view")) {
			y = drawRecipe(context, detail, y);
		}
		if (config.featureEnabled("itemlist.usage_view")) {
			y = drawUsages(context, detail, y);
		}
		drawSources(context, detail, y);
	}

	private int drawRecipe(DrawContext context, UiRect detail, int y) {
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.recipe"), detail.x(), y);
		y += 14;
		if (!hasRecipeSlots(selected.recipeSlots)) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.recipe_unavailable")),
					detail.x() + 8, y, 0xFF8494A1);
			return y + 20;
		}
		int gridX = detail.x() + 8;
		int gridY = y + 3;
		int slot = 20;
		for (int index = 0; index < 9; index++) {
			int col = index % 3;
			int row = index / 3;
			drawCraftSlot(context, gridX + col * slot, gridY + row * slot, selected.recipeSlots.get(index));
		}
		int arrowX = gridX + slot * 3 + 14;
		int arrowY = gridY + 24;
		context.fill(arrowX, arrowY, arrowX + 20, arrowY + 2, 0xFFB7C9D1);
		context.fill(arrowX + 16, arrowY - 4, arrowX + 18, arrowY + 6, 0xFFB7C9D1);
		context.fill(arrowX + 18, arrowY - 2, arrowX + 20, arrowY + 4, 0xFFB7C9D1);
		drawOutputSlot(context, arrowX + 34, gridY + 20);
		return y + 74;
	}

	private int drawUsages(DrawContext context, UiRect detail, int y) {
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.used_in"), detail.x(), y);
		y += 14;
		List<SkyBlockItem> usages = SkyBlockLensClient.itemRepository().itemsUsing(selected, 8);
		if (usages.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.uses_unavailable")),
					detail.x() + 8, y, 0xFF8494A1);
			return y + 20;
		}
		for (SkyBlockItem item : usages) {
			UiRect row = new UiRect(detail.x() + 4, y - 2, detail.width() - 8, 13);
			drawLinkRow(context, row, "- " + item.name, item);
			y += 13;
		}
		return y + 8;
	}

	private void drawLinkRow(DrawContext context, UiRect rect, String label, SkyBlockItem item) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x221C3A34);
		context.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), SkyBlockLensClient.configStore().config().accentArgb());
		context.drawTextWithShadow(textRenderer, Text.literal(fit(label, rect.width() - 12)),
				rect.x() + 4, rect.y() + 2, 0xFFD7FBE8);
		detailLinks.add(new DetailLink(rect, item));
	}

	private void drawSources(DrawContext context, UiRect detail, int y) {
		if (selected.sources.isEmpty()) {
			return;
		}
		drawSectionTitle(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.sources"), detail.x(), y);
		y += 14;
		for (String source : selected.sources) {
			context.drawTextWithShadow(textRenderer, Text.literal("- " + fit(source, detail.width() - 18)),
					detail.x() + 8, y, 0xFF8494A1);
			y += 13;
		}
	}

	private void renderDisabled(DrawContext context) {
		UiRect detail = detailContentBounds();
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.items.disabled")),
				detail.x(), detail.y(), 0xFFE06A4D);
	}

	private List<SkyBlockItem> currentResults() {
		if (!browserEnabled()) {
			return List.of();
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return SkyBlockLensClient.itemRepository().search(
				search == null ? searchValue : search.getText(),
				MAX_RESULTS,
				config.featureEnabled("itemlist.search_aliases"),
				config.featureEnabled("itemlist.hide_missing_data")
		);
	}

	private void syncSelection(List<SkyBlockItem> results) {
		if (results.isEmpty()) {
			selected = null;
			return;
		}
		if (selected != null) {
			return;
		}
		selected = results.getFirst();
	}

	private void selectItem(SkyBlockItem item, boolean pushHistory) {
		if (item == null || (selected != null && selected.id.equals(item.id))) {
			return;
		}
		if (pushHistory && selected != null) {
			history.push(selected.id);
			while (history.size() > 24) {
				history.removeLast();
			}
		}
		selected = item;
	}

	private void goBackInHistory() {
		while (!history.isEmpty()) {
			String id = history.pop();
			SkyBlockItem item = SkyBlockLensClient.itemRepository().findById(id).orElse(null);
			if (item != null) {
				selected = item;
				return;
			}
		}
	}

	private boolean browserEnabled() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return config.enabled && config.itemBrowser && config.featureEnabled("itemlist.local_browser");
	}

	private String detailMeta(SkyBlockItem item) {
		StringBuilder result = new StringBuilder(item.id);
		if (!item.rarity.isBlank()) {
			result.append(" / ").append(item.rarity);
		}
		if (!item.category.isBlank()) {
			result.append(" / ").append(item.category);
		}
		return result.toString();
	}

	private void drawSectionTitle(DrawContext context, String title, int x, int y) {
		context.drawTextWithShadow(textRenderer, Text.literal(title), x, y, 0xFF65E6A2);
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
		ItemStack stack = SkyBlockLensClient.itemRepository().iconStack(selected);
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

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY) {
		if (rect == null) {
			return;
		}
		boolean hovered = rect.contains(mouseX, mouseY);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hovered ? 0xFF31424C : 0xFF26323A);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + rect.height() / 2,
				hovered ? 0xFF3E5660 : 0xFF35464E);
		drawBorder(context, rect, 0xFF070A0C);
		String fitted = fit(label, rect.width() - 10);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				rect.centerX() - textRenderer.getWidth(fitted) / 2, rect.y() + 6, 0xFFF3F2E8);
	}

	private void drawScrollbar(DrawContext context, int x, int top, int bottom, int scroll, int maxScroll) {
		context.fill(x, top, x + 3, bottom, 0xFF070A0C);
		if (maxScroll <= 0) {
			return;
		}
		int height = bottom - top;
		int thumbHeight = Math.max(18, height * height / (height + maxScroll));
		int thumbY = top + (height - thumbHeight) * scroll / maxScroll;
		context.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, 0xFF42E8C8);
	}

	private void drawBorder(DrawContext context, UiRect rect, int color) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
		context.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
		context.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
		context.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
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
		List<String> lines = new java.util.ArrayList<>();
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

	private UiRect searchBounds() {
		UiRect listPanel = listPanelBounds();
		return new UiRect(listPanel.x() + 8, listPanel.y() + 22, listPanel.width() - 16, 20);
	}

	private UiRect listBounds() {
		UiRect listPanel = listPanelBounds();
		return new UiRect(listPanel.x() + 8, listPanel.y() + 58, listPanel.width() - 12, listPanel.height() - 66);
	}

	private UiRect detailContentBounds() {
		UiRect detailPanel = detailPanelBounds();
		return new UiRect(detailPanel.x() + 14, detailPanel.y() + 44, detailPanel.width() - 28, detailPanel.height() - 58);
	}

	private UiRect listPanelBounds() {
		int listWidth = Math.min(248, Math.max(190, width / 4));
		return new UiRect(PANEL_MARGIN, PANEL_MARGIN, listWidth, height - 48);
	}

	private UiRect detailPanelBounds() {
		UiRect listPanel = listPanelBounds();
		int x = listPanel.right() + 12;
		return new UiRect(x, PANEL_MARGIN, Math.max(180, width - x - PANEL_MARGIN), height - 48);
	}

	private record DetailLink(UiRect bounds, SkyBlockItem item) {
	}
}
