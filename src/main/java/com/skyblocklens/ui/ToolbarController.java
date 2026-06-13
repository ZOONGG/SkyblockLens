package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.items.SkyBlockItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ToolbarController {
	private static final int ICON_BUTTON_WIDTH = 24;
	private static final int GAP = 4;
	private static final int BROWSER_WIDTH = 186;
	private static final int BROWSER_MARGIN = 4;
	private static final int BROWSER_CELL = 22;
	private static final int PAGE_BUTTON_SIZE = 16;
	private static final int SEARCH_TIMEOUT_MILLIS = 120_000;
	private static final int QUERY_LIMIT = 64;

	private static UiRect searchBox = new UiRect(0, 0, 0, 0);
	private static UiRect browserToggleButton = new UiRect(0, 0, 0, 0);
	private static UiRect inventorySearchButton = new UiRect(0, 0, 0, 0);
	private static UiRect sidebar = new UiRect(0, 0, 0, 0);
	private static UiRect sidebarGrid = new UiRect(0, 0, 0, 0);
	private static UiRect browserUpButton = new UiRect(0, 0, 0, 0);
	private static UiRect browserDownButton = new UiRect(0, 0, 0, 0);
	private static final List<ResultHitbox> resultHitboxes = new ArrayList<>();
	private static final Map<String, Double> hoverAnimations = new HashMap<>();

	private static String query = "";
	private static String undoQuery = "";
	private static String selectedItemId = "";
	private static String cachedResultKey = "";
	private static List<SkyBlockItem> cachedResults = List.of();
	private static boolean focused;
	private static boolean selectAllQuery;
	private static boolean inventorySearchMode;
	private static int browserPage;
	private static int maxBrowserPage;
	private static long lastInteractionMillis;
	private static String lastFallbackChar = "";
	private static long lastFallbackMillis;
	private static String feedbackText = "";
	private static long feedbackUntilMillis;

	private ToolbarController() {
	}

	public static void render(DrawContext context, int mouseX, int mouseY, int screenX, int screenY,
			int backgroundWidth, int backgroundHeight) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!enabled(config)) {
			clearBounds();
			return;
		}
		tickTimeout(config);
		int searchWidth = config.toolbarSearchWidth;
		int height = config.toolbarSearchHeight;
		int toolbarWidth = toolbarWidth(config);
		int x = Math.max(8, Math.min((context.getScaledWindowWidth() - toolbarWidth) / 2 + config.toolbarOffsetX,
				context.getScaledWindowWidth() - toolbarWidth - 8));
		int y = Math.max(8, Math.min(context.getScaledWindowHeight() - 42 + config.toolbarOffsetY,
				context.getScaledWindowHeight() - height - 8));
		int accent = config.accentArgb();

		if (config.featureEnabled("toolbar.search_bar")) {
			searchBox = new UiRect(x, y, searchWidth, height);
			drawSearchBox(context, searchBox, mouseX, mouseY, config, accent);
			x = searchBox.right() + GAP;
		} else {
			searchBox = new UiRect(0, 0, 0, 0);
		}

		if (browserButtonVisible(config)) {
			browserToggleButton = drawIconToolbarButton(context, "browser", x, y, ICON_BUTTON_WIDTH, height,
					mouseX, mouseY, config.itemBrowserOverlayVisible ? accent : 0xFF52616A, config.toolbarBackgroundArgb(),
					Icon.GRID);
			x = browserToggleButton.right() + GAP;
		} else {
			browserToggleButton = new UiRect(0, 0, 0, 0);
		}

		if (config.featureEnabled("toolbar.inventory_search_button")) {
			int searchAccent = SkyBlockLensConfig.parseHexColor(config.inventorySearchHighlightColor, accent);
			inventorySearchButton = drawIconToolbarButton(context, "inventory", x, y, ICON_BUTTON_WIDTH, height,
					mouseX, mouseY, inventorySearchMode ? searchAccent : accent, config.toolbarBackgroundArgb(),
					Icon.SEARCH);
		} else {
			inventorySearchButton = new UiRect(0, 0, 0, 0);
		}

		renderFeedback(context, y, config);
		renderItemSidebar(context, mouseX, mouseY, screenX, screenY, backgroundWidth, backgroundHeight, config);
		renderToolbarTooltip(context, mouseX, mouseY, config);
	}

	public static boolean mouseClicked(Click click, ScreenHandler handler, String screenTitle, int screenX, int screenY) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!enabled(config) || click.button() != 0) {
			return false;
		}
		if (searchBox.contains(click.x(), click.y()) && config.featureEnabled("toolbar.search_bar")) {
			focused = true;
			selectAllQuery = false;
			touch();
			return true;
		}
		focused = false;
		selectAllQuery = false;
		if (browserToggleButton.contains(click.x(), click.y())) {
			config.itemBrowserOverlayVisible = !config.itemBrowserOverlayVisible;
			SkyBlockLensClient.configStore().save();
			touch();
			return true;
		}
		if (inventorySearchButton.contains(click.x(), click.y())) {
			handleInventorySearchClick(handler, config);
			return true;
		}
		if (browserUpButton.contains(click.x(), click.y())) {
			browserPage = MathHelper.clamp(browserPage - 1, 0, maxBrowserPage);
			touch();
			return true;
		}
		if (browserDownButton.contains(click.x(), click.y())) {
			browserPage = MathHelper.clamp(browserPage + 1, 0, maxBrowserPage);
			touch();
			return true;
		}
		for (ResultHitbox hitbox : resultHitboxes) {
			if (hitbox.bounds().contains(click.x(), click.y())) {
				selectedItemId = hitbox.item().id;
				MinecraftClient client = MinecraftClient.getInstance();
				client.setScreen(new ItemDetailsScreen(client.currentScreen, hitbox.item()));
				touch();
				return true;
			}
		}
		return false;
	}

	public static boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
		if (!sidebar.contains(mouseX, mouseY) && !sidebarGrid.contains(mouseX, mouseY)) {
			return false;
		}
		int direction = verticalAmount > 0.0D ? -1 : 1;
		browserPage = MathHelper.clamp(browserPage + direction, 0, maxBrowserPage);
		touch();
		return true;
	}

	public static boolean keyPressed(KeyInput input) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!enabled(config)) {
			return false;
		}
		boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
		if (config.featureEnabled("toolbar.ctrl_f") && ctrl && input.key() == GLFW.GLFW_KEY_F) {
			focused = true;
			selectAllQuery = true;
			touch();
			return true;
		}
		if (!focused) {
			return false;
		}
		touch();
		if (ctrl && handleControlShortcut(input.key())) {
			return true;
		}
		switch (input.key()) {
			case GLFW.GLFW_KEY_ESCAPE -> {
				focused = false;
				selectAllQuery = false;
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				focused = false;
				selectAllQuery = false;
				return true;
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (selectAllQuery) {
					setQuery("");
					selectAllQuery = false;
				} else if (!query.isEmpty()) {
					setQuery(query.substring(0, query.length() - 1));
				}
				return true;
			}
			case GLFW.GLFW_KEY_DELETE -> {
				setQuery("");
				selectAllQuery = false;
				inventorySearchMode = false;
				return true;
			}
			default -> {
				return appendPrintableFromKey(input);
			}
		}
	}

	public static boolean charTyped(CharInput input) {
		if (!focused || !input.isValidChar() || !enabled(SkyBlockLensClient.configStore().config())) {
			return false;
		}
		String typed = input.asString();
		if (typed.isEmpty()) {
			return true;
		}
		long now = System.currentTimeMillis();
		if (typed.equals(lastFallbackChar) && now - lastFallbackMillis < 90L) {
			return true;
		}
		if (typed.isBlank() && typed.charAt(0) != ' ') {
			return true;
		}
		appendText(typed);
		touch();
		return true;
	}

	public static void renderSlotOverlay(DrawContext context, Slot slot, int screenX, int screenY) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!enabled(config) || !config.featureEnabled("toolbar.inventory_search_button")
				|| !inventorySearchMode || query.isBlank() || slot == null) {
			return;
		}
		ItemStack stack = slot.getStack();
		boolean match = !stack.isEmpty() && SkyBlockLensClient.itemRepository().stackMatchesQuery(
				stack.getName().getString(),
				Registries.ITEM.getId(stack.getItem()).toString(),
				query,
				config.featureEnabled("itemlist.search_aliases"));
		if (!match) {
			return;
		}
		int x = screenX + slot.x;
		int y = screenY + slot.y;
		int accent = SkyBlockLensConfig.parseHexColor(config.inventorySearchHighlightColor, config.accentArgb());
		int pulse = 80 + (int) (Math.sin(System.currentTimeMillis() / 160.0D) * 40.0D);
		int color = (Math.max(120, pulse) << 24) | (accent & 0x00FFFFFF);
		context.fill(x - 1, y - 1, x + 17, y + 1, color);
		context.fill(x - 1, y + 15, x + 17, y + 17, color);
		context.fill(x - 1, y - 1, x + 1, y + 17, color);
		context.fill(x + 15, y - 1, x + 17, y + 17, color);
	}

	private static void renderItemSidebar(DrawContext context, int mouseX, int mouseY, int screenX, int screenY,
			int backgroundWidth, int backgroundHeight, SkyBlockLensConfig config) {
		resultHitboxes.clear();
		browserUpButton = new UiRect(0, 0, 0, 0);
		browserDownButton = new UiRect(0, 0, 0, 0);
		maxBrowserPage = 0;
		if (!browserOverlayVisible(config)) {
			sidebar = new UiRect(0, 0, 0, 0);
			sidebarGrid = new UiRect(0, 0, 0, 0);
			return;
		}
		int panelWidth = Math.min(BROWSER_WIDTH, Math.max(132, context.getScaledWindowWidth() - 12));
		int x = Math.max(BROWSER_MARGIN, context.getScaledWindowWidth() - panelWidth - BROWSER_MARGIN);
		int y = 8;
		int h = Math.max(118, context.getScaledWindowHeight() - y - 46);
		sidebar = new UiRect(x, y, panelWidth, h);

		int border = config.accentArgb();
		context.fill(sidebar.x() + 3, sidebar.y() + 3, sidebar.right() + 3, sidebar.bottom() + 3, 0x66000000);
		context.fill(sidebar.x(), sidebar.y(), sidebar.right(), sidebar.bottom(), 0xD6080E14);
		context.fill(sidebar.x() + 1, sidebar.y() + 1, sidebar.right() - 1, sidebar.y() + 24, 0xE6151C23);
		drawBorder(context, sidebar, border);
		MinecraftClient client = MinecraftClient.getInstance();
		context.drawTextWithShadow(client.textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.results")),
				sidebar.x() + 8, sidebar.y() + 9, border);

		browserUpButton = new UiRect(sidebar.right() - 42, sidebar.y() + 5, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE);
		browserDownButton = new UiRect(sidebar.right() - 22, sidebar.y() + 5, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE);
		drawArrowButton(context, browserUpButton, mouseX, mouseY, true, border);
		drawArrowButton(context, browserDownButton, mouseX, mouseY, false, border);

		if (SkyBlockLensClient.itemRepository().isEmpty()) {
			drawCenteredState(context, SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.data_missing"), 0xFFE06A4D);
			return;
		}
		List<SkyBlockItem> results = currentResults(config);
		if (results.isEmpty()) {
			drawCenteredState(context, SkyBlockLensClient.i18n().tr("skyblocklens.items.no_results"), 0xFFAAB7C4);
			return;
		}
		renderItemGrid(context, results, mouseX, mouseY, config);
	}

	private static void renderItemGrid(DrawContext context, List<SkyBlockItem> results, int mouseX, int mouseY,
			SkyBlockLensConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		sidebarGrid = new UiRect(sidebar.x() + 7, sidebar.y() + 29, sidebar.width() - 14, sidebar.height() - 36);
		int columns = Math.max(1, sidebarGrid.width() / BROWSER_CELL);
		int rows = Math.max(1, sidebarGrid.height() / BROWSER_CELL);
		int pageSize = Math.max(1, columns * rows);
		maxBrowserPage = Math.max(0, (results.size() - 1) / pageSize);
		browserPage = MathHelper.clamp(browserPage, 0, maxBrowserPage);
		int pageStart = browserPage * pageSize;
		int pageEnd = Math.min(results.size(), pageStart + pageSize);
		if (maxBrowserPage > 0) {
			String label = (browserPage + 1) + "/" + (maxBrowserPage + 1);
			context.drawTextWithShadow(client.textRenderer, Text.literal(label),
					sidebar.right() - 68 - client.textRenderer.getWidth(label), sidebar.y() + 9, 0xFFAAB7C4);
		}

		SkyBlockItem hoveredItem = null;
		context.enableScissor(sidebarGrid.x(), sidebarGrid.y(), sidebarGrid.right(), sidebarGrid.bottom());
		for (int index = pageStart; index < pageEnd; index++) {
			SkyBlockItem item = results.get(index);
			int localIndex = index - pageStart;
			int col = localIndex % columns;
			int row = localIndex / columns;
			int cellX = sidebarGrid.x() + col * BROWSER_CELL;
			int cellY = sidebarGrid.y() + row * BROWSER_CELL;
			UiRect cell = new UiRect(cellX, cellY, BROWSER_CELL - 2, BROWSER_CELL - 2);
			boolean hover = cell.contains(mouseX, mouseY);
			boolean active = item.id.equals(selectedItemId);
			double hoverProgress = hoverAnimation("item:" + item.id, hover || active);
			context.fill(cell.x(), cell.y(), cell.right(), cell.bottom(),
					active ? 0xFF24443F : blend(0xCC101720, 0xEE1F2A31, hoverProgress));
			drawBorder(context, cell, active ? config.accentArgb() : blend(0xFF1D2830, config.accentArgb(), hoverProgress * 0.45D));
			ItemStack stack = SkyBlockLensClient.itemRepository().iconStack(item);
			int itemX = cell.x() + Math.max(1, (cell.width() - 16) / 2);
			int itemY = cell.y() + 3;
			if (!stack.isEmpty()) {
				context.drawItem(stack, itemX, itemY);
			} else {
				context.fill(itemX + 3, itemY + 3, itemX + 13, itemY + 13, 0xFF2D3740);
				drawBorder(context, new UiRect(itemX + 3, itemY + 3, 10, 10), 0xFF52616A);
			}
			if (hover) {
				hoveredItem = item;
			}
			resultHitboxes.add(new ResultHitbox(cell, item));
		}
		context.disableScissor();
		if (hoveredItem != null) {
			drawItemNameTooltip(context, hoveredItem, mouseX, mouseY, config);
		}
	}

	private static List<SkyBlockItem> currentResults(SkyBlockLensConfig config) {
		String key = query + "|" + config.featureEnabled("itemlist.search_aliases")
				+ "|" + config.featureEnabled("itemlist.hide_missing_data")
				+ "|" + SkyBlockLensClient.itemRepository().items().size();
		if (key.equals(cachedResultKey)) {
			return cachedResults;
		}
		cachedResultKey = key;
		cachedResults = SkyBlockLensClient.itemRepository().search(
				query,
				Math.max(1, SkyBlockLensClient.itemRepository().items().size()),
				config.featureEnabled("itemlist.search_aliases"),
				config.featureEnabled("itemlist.hide_missing_data")
		);
		browserPage = 0;
		if (cachedResults.stream().noneMatch(item -> item.id.equals(selectedItemId))) {
			selectedItemId = cachedResults.isEmpty() ? "" : cachedResults.getFirst().id;
		}
		return cachedResults;
	}

	private static void handleInventorySearchClick(ScreenHandler handler, SkyBlockLensConfig config) {
		touch();
		if (query.isBlank()) {
			inventorySearchMode = false;
			showFeedback(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.enter_query"));
			return;
		}
		inventorySearchMode = !inventorySearchMode;
		if (!inventorySearchMode) {
			return;
		}
		if (countMatchingSlots(handler, config) <= 0) {
			showFeedback(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.not_found"));
		}
	}

	private static int countMatchingSlots(ScreenHandler handler, SkyBlockLensConfig config) {
		if (handler == null) {
			return 0;
		}
		int count = 0;
		for (Slot slot : handler.slots) {
			if (slot == null || !slot.hasStack()) {
				continue;
			}
			ItemStack stack = slot.getStack();
			if (SkyBlockLensClient.itemRepository().stackMatchesQuery(
					stack.getName().getString(),
					Registries.ITEM.getId(stack.getItem()).toString(),
					query,
					config.featureEnabled("itemlist.search_aliases"))) {
				count++;
			}
		}
		return count;
	}

	private static void drawSearchBox(DrawContext context, UiRect rect, int mouseX, int mouseY, SkyBlockLensConfig config, int accent) {
		MinecraftClient client = MinecraftClient.getInstance();
		boolean hover = rect.contains(mouseX, mouseY);
		double hoverProgress = hoverAnimation("search", hover || focused);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(),
				focused ? blend(config.toolbarBackgroundArgb(), 0xFF142129, 0.55D)
						: blend(config.toolbarBackgroundArgb(), 0xFF26343C, hoverProgress * 0.45D));
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + Math.max(2, rect.height() / 2),
				blend(0x222D4250, 0x553F5560, hoverProgress));
		drawBorder(context, rect, focused ? accent : 0xFF2C3940);
		if (focused && selectAllQuery && !query.isBlank()) {
			int selectRight = Math.min(rect.right() - 6, rect.x() + 7 + client.textRenderer.getWidth(fit(query, rect.width() - 14)));
			context.fill(rect.x() + 5, rect.y() + 4, selectRight, rect.bottom() - 4, 0x7742E8C8);
		}
		String text = query.isBlank() && !focused ? SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.search") : query;
		int color = query.isBlank() && !focused ? 0xFF8494A1 : 0xFFE6F4FF;
		String fitted = fit(text, rect.width() - 14);
		context.drawTextWithShadow(client.textRenderer, Text.literal(fitted), rect.x() + 7,
				rect.y() + Math.max(4, (rect.height() - 8) / 2), color);
		if (focused && !selectAllQuery && (System.currentTimeMillis() / 450L) % 2L == 0L) {
			int cursorX = rect.x() + 8 + client.textRenderer.getWidth(fitted);
			context.fill(cursorX, rect.y() + 4, cursorX + 1, rect.bottom() - 4, accent);
		}
	}

	private static UiRect drawIconToolbarButton(
			DrawContext context,
			String key,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			int accent,
			int background,
			Icon icon
	) {
		UiRect rect = new UiRect(x, y, width, height);
		boolean hover = rect.contains(mouseX, mouseY);
		double hoverProgress = hoverAnimation("button:" + key + ":" + x + ":" + y, hover);
		int lift = (int) Math.round(hoverProgress);
		UiRect drawRect = new UiRect(rect.x(), rect.y() - lift, rect.width(), rect.height());
		context.fill(drawRect.x(), drawRect.y(), drawRect.right(), drawRect.bottom(), blend(background, 0xFF31404A, hoverProgress));
		context.fill(drawRect.x() + 1, drawRect.y() + 1, drawRect.right() - 1, drawRect.y() + Math.max(2, drawRect.height() / 2),
				blend(0x333F5560, 0x665A7078, hoverProgress));
		context.fill(drawRect.x(), drawRect.y(), drawRect.right(), drawRect.y() + 1, accent);
		drawBorder(context, drawRect, blend(0xFF070A0C, accent, hoverProgress * 0.55D));
		drawIcon(context, drawRect, icon, blend(0xFFE6F4FF, accent, hoverProgress * 0.45D));
		return rect;
	}

	private static void drawIcon(DrawContext context, UiRect rect, Icon icon, int color) {
		int cx = rect.centerX();
		int cy = rect.y() + rect.height() / 2;
		if (icon == Icon.SEARCH) {
			context.fill(cx - 5, cy - 4, cx + 2, cy - 3, color);
			context.fill(cx - 5, cy + 2, cx + 2, cy + 3, color);
			context.fill(cx - 5, cy - 4, cx - 4, cy + 3, color);
			context.fill(cx + 1, cy - 4, cx + 2, cy + 3, color);
			context.fill(cx + 3, cy + 4, cx + 7, cy + 5, color);
			context.fill(cx + 4, cy + 5, cx + 8, cy + 6, color);
			return;
		}
		int startX = cx - 6;
		int startY = cy - 6;
		for (int iy = 0; iy < 2; iy++) {
			for (int ix = 0; ix < 2; ix++) {
				context.fill(startX + ix * 7, startY + iy * 7, startX + ix * 7 + 5, startY + iy * 7 + 5, color);
			}
		}
	}

	private static void drawArrowButton(DrawContext context, UiRect rect, int mouseX, int mouseY, boolean up, int accent) {
		double hover = hoverAnimation("arrow:" + up + ":" + rect.x() + ":" + rect.y(), rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(0xFF101820, 0xFF26323A, hover));
		drawBorder(context, rect, blend(0xFF2B3740, accent, hover * 0.75D));
		int centerX = rect.centerX();
		int centerY = rect.y() + rect.height() / 2;
		if (up) {
			context.fill(centerX - 4, centerY + 2, centerX + 5, centerY + 3, 0xFFE6F4FF);
			context.fill(centerX - 2, centerY, centerX + 3, centerY + 1, 0xFFE6F4FF);
			context.fill(centerX, centerY - 2, centerX + 1, centerY - 1, 0xFFE6F4FF);
		} else {
			context.fill(centerX - 4, centerY - 2, centerX + 5, centerY - 1, 0xFFE6F4FF);
			context.fill(centerX - 2, centerY, centerX + 3, centerY + 1, 0xFFE6F4FF);
			context.fill(centerX, centerY + 2, centerX + 1, centerY + 3, 0xFFE6F4FF);
		}
	}

	private static void drawCenteredState(DrawContext context, String text, int color) {
		MinecraftClient client = MinecraftClient.getInstance();
		int maxWidth = sidebar.width() - 18;
		String fitted = fit(text, maxWidth);
		context.drawTextWithShadow(client.textRenderer, Text.literal(fitted),
				sidebar.centerX() - client.textRenderer.getWidth(fitted) / 2,
				sidebar.y() + sidebar.height() / 2 - 4, color);
	}

	private static void drawScrollbar(DrawContext context, int x, int top, int bottom, int scroll, int maxScroll, int accent) {
		context.fill(x, top, x + 3, bottom, 0xFF070A0C);
		if (maxScroll <= 0) {
			return;
		}
		int height = bottom - top;
		int thumbHeight = Math.max(18, height * height / (height + maxScroll));
		int thumbY = top + (height - thumbHeight) * scroll / maxScroll;
		context.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, accent);
	}

	private static void drawItemNameTooltip(DrawContext context, SkyBlockItem item, int mouseX, int mouseY,
			SkyBlockLensConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		String text = item.name == null || item.name.isBlank() ? item.id : item.name;
		int maxWidth = Math.min(260, context.getScaledWindowWidth() - 16);
		String fitted = fit(text, maxWidth - 10);
		int w = client.textRenderer.getWidth(fitted) + 10;
		int x = MathHelper.clamp(mouseX + 10, 4, context.getScaledWindowWidth() - w - 4);
		int y = MathHelper.clamp(mouseY - 18, 4, context.getScaledWindowHeight() - 17);
		context.fill(x + 2, y + 2, x + w + 2, y + 17, 0x66000000);
		context.fill(x, y, x + w, y + 15, 0xCC080E14);
		drawBorder(context, new UiRect(x, y, w, 15), rarityOrAccent(item, config.accentArgb()));
		context.drawTextWithShadow(client.textRenderer, Text.literal(fitted), x + 5, y + 4,
				rarityOrAccent(item, 0xFFE6F4FF));
	}

	private static void renderFeedback(DrawContext context, int toolbarY, SkyBlockLensConfig config) {
		if (feedbackText.isBlank() || System.currentTimeMillis() > feedbackUntilMillis) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		int w = Math.min(240, client.textRenderer.getWidth(feedbackText) + 14);
		int x = searchBox.x();
		int y = toolbarY - 18;
		context.fill(x, y, x + w, y + 14, 0xDD151C23);
		drawBorder(context, new UiRect(x, y, w, 14), config.accentArgb());
		context.drawTextWithShadow(client.textRenderer, Text.literal(fit(feedbackText, w - 10)), x + 5, y + 3, 0xFFF3F2E8);
	}

	private static void renderToolbarTooltip(DrawContext context, int mouseX, int mouseY, SkyBlockLensConfig config) {
		String key = "";
		if (browserToggleButton.contains(mouseX, mouseY)) {
			key = "skyblocklens.toolbar.toggle_browser.tooltip";
		} else if (inventorySearchButton.contains(mouseX, mouseY)) {
			key = "skyblocklens.toolbar.search_inventory.tooltip";
		}
		if (key.isBlank()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		String text = SkyBlockLensClient.i18n().tr(key);
		int w = Math.min(220, client.textRenderer.getWidth(text) + 12);
		int x = MathHelper.clamp(mouseX + 8, 8, context.getScaledWindowWidth() - w - 8);
		int y = MathHelper.clamp(mouseY - 20, 8, context.getScaledWindowHeight() - 18);
		context.fill(x, y, x + w, y + 16, 0xF0101820);
		drawBorder(context, new UiRect(x, y, w, 16), config.accentArgb());
		context.drawTextWithShadow(client.textRenderer, Text.literal(fit(text, w - 10)), x + 5, y + 5, 0xFFF3F2E8);
	}

	private static boolean handleControlShortcut(int key) {
		MinecraftClient client = MinecraftClient.getInstance();
		switch (key) {
			case GLFW.GLFW_KEY_A -> {
				selectAllQuery = true;
				return true;
			}
			case GLFW.GLFW_KEY_C -> {
				client.keyboard.setClipboard(query);
				return true;
			}
			case GLFW.GLFW_KEY_X -> {
				client.keyboard.setClipboard(query);
				setQuery("");
				selectAllQuery = false;
				return true;
			}
			case GLFW.GLFW_KEY_V -> {
				appendText(client.keyboard.getClipboard());
				return true;
			}
			case GLFW.GLFW_KEY_Z -> {
				String previous = undoQuery;
				undoQuery = query;
				query = previous;
				inventorySearchMode = false;
				selectAllQuery = false;
				cachedResultKey = "";
				browserPage = 0;
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static void appendText(String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		String cleaned = value.replace('\n', ' ').replace('\r', ' ');
		if (selectAllQuery) {
			setQuery(cleaned);
			selectAllQuery = false;
			return;
		}
		setQuery(query + cleaned);
	}

	private static void setQuery(String value) {
		String next = value == null ? "" : value;
		if (next.length() > QUERY_LIMIT) {
			next = next.substring(0, QUERY_LIMIT);
		}
		if (query.equals(next)) {
			return;
		}
		undoQuery = query;
		query = next;
		selectedItemId = "";
		inventorySearchMode = false;
		cachedResultKey = "";
		browserPage = 0;
		if (!query.isBlank() && SkyBlockLensClient.configStore().config().featureEnabled("itemlist.overlay_while_typing")) {
			SkyBlockLensClient.configStore().config().itemBrowserOverlayVisible = true;
		}
	}

	private static boolean enabled(SkyBlockLensConfig config) {
		return config.enabled && config.featureEnabled("toolbar.enable");
	}

	private static boolean browserButtonVisible(SkyBlockLensConfig config) {
		return config.featureEnabled("itemlist.local_browser") && config.featureEnabled("itemlist.browser_overlay");
	}

	private static boolean browserOverlayVisible(SkyBlockLensConfig config) {
		return browserButtonVisible(config)
				&& (config.itemBrowserOverlayVisible
						|| (!query.isBlank() && config.featureEnabled("itemlist.overlay_while_typing")));
	}

	private static int toolbarWidth(SkyBlockLensConfig config) {
		int width = 0;
		if (config.featureEnabled("toolbar.search_bar")) {
			width += config.toolbarSearchWidth;
		}
		if (browserButtonVisible(config)) {
			if (width > 0) {
				width += GAP;
			}
			width += ICON_BUTTON_WIDTH;
		}
		if (config.featureEnabled("toolbar.inventory_search_button")) {
			if (width > 0) {
				width += GAP;
			}
			width += ICON_BUTTON_WIDTH;
		}
		return Math.max(1, width);
	}

	private static boolean appendPrintableFromKey(KeyInput input) {
		if ((input.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) != 0) {
			return false;
		}
		String typed;
		if (input.key() == GLFW.GLFW_KEY_SPACE) {
			typed = " ";
		} else {
			typed = GLFW.glfwGetKeyName(input.key(), input.scancode());
			if (typed == null || typed.length() != 1) {
				return false;
			}
			if ((input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) {
				typed = typed.toUpperCase(Locale.ROOT);
			}
		}
		appendText(typed);
		lastFallbackChar = typed;
		lastFallbackMillis = System.currentTimeMillis();
		touch();
		return true;
	}

	private static void tickTimeout(SkyBlockLensConfig config) {
		if (config.featureEnabled("toolbar.auto_turnoff_search")
				&& inventorySearchMode
				&& System.currentTimeMillis() - lastInteractionMillis > SEARCH_TIMEOUT_MILLIS) {
			inventorySearchMode = false;
			focused = false;
			selectAllQuery = false;
		}
	}

	private static void showFeedback(String text) {
		feedbackText = text == null ? "" : text;
		feedbackUntilMillis = System.currentTimeMillis() + 1600L;
	}

	private static void touch() {
		lastInteractionMillis = System.currentTimeMillis();
	}

	private static void clearBounds() {
		searchBox = new UiRect(0, 0, 0, 0);
		browserToggleButton = new UiRect(0, 0, 0, 0);
		inventorySearchButton = new UiRect(0, 0, 0, 0);
		sidebar = new UiRect(0, 0, 0, 0);
		sidebarGrid = new UiRect(0, 0, 0, 0);
		resultHitboxes.clear();
		focused = false;
		selectAllQuery = false;
		inventorySearchMode = false;
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

	private static int rarityOrAccent(SkyBlockItem item, int fallback) {
		return switch (item.rarity == null ? "" : item.rarity) {
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

	private enum Icon {
		GRID,
		SEARCH
	}

	private record ResultHitbox(UiRect bounds, SkyBlockItem item) {
	}
}
