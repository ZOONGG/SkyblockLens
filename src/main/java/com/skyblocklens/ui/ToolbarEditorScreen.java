package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class ToolbarEditorScreen extends Screen {
	private static final int ICON_BUTTON_WIDTH = 24;
	private static final int GAP = 4;
	private static final int HANDLE_SIZE = 6;

	private final Screen parent;
	private final Map<String, Double> hoverAnimations = new HashMap<>();
	private UiRect backButton = new UiRect(0, 0, 0, 0);
	private DragMode dragMode = DragMode.NONE;
	private int dragOffsetX;
	private int dragOffsetY;
	private int resizeStartMouseX;
	private int resizeStartMouseY;
	private int resizeStartWidth;
	private int resizeStartHeight;
	private int resizeStartBrowserWidth;
	private int resizeStartBrowserHeight;

	public ToolbarEditorScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar_editor.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		backButton = new UiRect(width - 92, height - 28, 80, 20);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xDD070A10);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar_editor.title")),
				12, 10, 0xFFE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar_editor.hint")),
				12, 24, 0xFFAAB7C4);
		if (SkyBlockLensClient.configStore().config().featureEnabled("gui.show_anchor_grid")) {
			drawGrid(context);
		}
		drawToolbarPreview(context, mouseX, mouseY);
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (backButton.contains(click.x(), click.y()) && click.button() == 0) {
			close();
			return true;
		}
		if (click.button() != 0) {
			return super.mouseClicked(click, doubled);
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		UiRect browserPanel = browserBounds(config);
		UiRect browserHandle = resizeHandle(browserPanel);
		if (browserHandle.contains(click.x(), click.y())) {
			dragMode = DragMode.BROWSER_RESIZE;
			resizeStartMouseX = (int) click.x();
			resizeStartMouseY = (int) click.y();
			resizeStartBrowserWidth = config.itemBrowserWidth;
			resizeStartBrowserHeight = browserPanel.height();
			return true;
		}
		if (browserPanel.contains(click.x(), click.y())) {
			dragMode = DragMode.BROWSER_MOVE;
			dragOffsetX = (int) click.x() - browserPanel.x();
			dragOffsetY = (int) click.y() - browserPanel.y();
			return true;
		}
		UiRect toolbar = toolbarBounds(config);
		UiRect handle = resizeHandle(toolbar, config);
		if (handle.contains(click.x(), click.y())) {
			dragMode = DragMode.RESIZE;
			resizeStartMouseX = (int) click.x();
			resizeStartMouseY = (int) click.y();
			resizeStartWidth = config.toolbarSearchWidth;
			resizeStartHeight = config.toolbarSearchHeight;
			return true;
		}
		if (toolbar.contains(click.x(), click.y())) {
			dragMode = DragMode.MOVE;
			dragOffsetX = (int) click.x() - toolbar.x();
			dragOffsetY = (int) click.y() - toolbar.y();
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (dragMode == DragMode.MOVE) {
			int toolbarWidth = toolbarWidth(config);
			int targetX = (int) click.x() - dragOffsetX;
			int targetY = (int) click.y() - dragOffsetY;
			config.toolbarOffsetX = clamp(targetX - (width - toolbarWidth) / 2, -900, 900);
			config.toolbarOffsetY = clamp(targetY - (height - 42), -900, 900);
			return true;
		}
		if (dragMode == DragMode.RESIZE) {
			config.toolbarSearchWidth = clamp(resizeStartWidth + (int) click.x() - resizeStartMouseX, 140, 560);
			config.toolbarSearchHeight = clamp(resizeStartHeight + (int) click.y() - resizeStartMouseY, 16, 34);
			return true;
		}
		if (dragMode == DragMode.BROWSER_MOVE) {
			int targetX = (int) click.x() - dragOffsetX;
			int targetY = (int) click.y() - dragOffsetY;
			config.itemBrowserOffsetX = clamp(targetX - (width - config.itemBrowserWidth - 4), -900, 900);
			config.itemBrowserY = clamp(targetY, 4, Math.max(4, height - 118));
			return true;
		}
		if (dragMode == DragMode.BROWSER_RESIZE) {
			config.itemBrowserWidth = clamp(resizeStartBrowserWidth + (int) click.x() - resizeStartMouseX, 188, 360);
			int maxHeight = Math.max(118, height - config.itemBrowserY - 4);
			config.itemBrowserHeight = clamp(resizeStartBrowserHeight + (int) click.y() - resizeStartMouseY, 118, maxHeight);
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragMode != DragMode.NONE) {
			dragMode = DragMode.NONE;
			SkyBlockLensClient.configStore().save();
			return true;
		}
		return super.mouseReleased(click);
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

	private void drawToolbarPreview(DrawContext context, int mouseX, int mouseY) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		UiRect toolbar = toolbarBounds(config);
		int accent = config.accentArgb();
		int x = toolbar.x();
		if (config.featureEnabled("toolbar.search_bar")) {
			UiRect search = new UiRect(x, toolbar.y(), config.toolbarSearchWidth, config.toolbarSearchHeight);
			context.fill(search.x(), search.y(), search.right(), search.bottom(), config.toolbarBackgroundArgb());
			context.fill(search.x() + 1, search.y() + 1, search.right() - 1, search.y() + Math.max(2, search.height() / 2), 0x332D4250);
			drawBorder(context, search, accent);
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.search")),
					search.x() + 7, search.y() + Math.max(4, (search.height() - 8) / 2), 0xFFE6F4FF);
			drawResizeHandle(context, resizeHandle(toolbar, config), accent);
			x = search.right();
		}
		if (config.featureEnabled("itemlist.local_browser") && config.featureEnabled("itemlist.browser_overlay")) {
			if (x > toolbar.x()) {
				x += GAP;
			}
			UiRect browser = new UiRect(x, toolbar.y(), ICON_BUTTON_WIDTH, config.toolbarSearchHeight);
			drawIconButton(context, browser, mouseX, mouseY, true);
			x = browser.right();
		}
		if (config.featureEnabled("toolbar.inventory_search_button")) {
			if (x > toolbar.x()) {
				x += GAP;
			}
			UiRect find = new UiRect(x, toolbar.y(), ICON_BUTTON_WIDTH, config.toolbarSearchHeight);
			drawIconButton(context, find, mouseX, mouseY, false);
		}
		drawBrowserPreview(context, mouseX, mouseY, config);
	}

	private void drawBrowserPreview(DrawContext context, int mouseX, int mouseY, SkyBlockLensConfig config) {
		UiRect browser = browserBounds(config);
		int accent = config.accentArgb();
		context.fill(browser.x() + 3, browser.y() + 3, browser.right() + 3, browser.bottom() + 3, 0x66000000);
		context.fill(browser.x(), browser.y(), browser.right(), browser.bottom(), 0xD6080E14);
		context.fill(browser.x() + 1, browser.y() + 1, browser.right() - 1, browser.y() + 36, 0xE6151C23);
		drawBorder(context, browser, accent);
		context.drawTextWithShadow(textRenderer, Text.literal(fit(SkyBlockLensClient.i18n().tr("skyblocklens.toolbar.results"),
				browser.width() - 16)), browser.x() + 8, browser.y() + 8, accent);
		int cell = Math.max(20, config.itemBrowserIconSize + 4);
		int gridX = browser.x() + 7;
		int gridY = browser.y() + 42;
		int gridRight = browser.right() - 7;
		int gridBottom = browser.bottom() - 8;
		for (int y = gridY; y + cell - 2 <= gridBottom; y += cell) {
			for (int x = gridX; x + cell - 2 <= gridRight; x += cell) {
				UiRect slot = new UiRect(x, y, cell - 2, cell - 2);
				context.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), 0xCC101720);
				drawBorder(context, slot, 0xFF1D2830);
			}
		}
		drawResizeHandle(context, resizeHandle(browser), accent);
	}

	private UiRect toolbarBounds(SkyBlockLensConfig config) {
		int toolbarWidth = toolbarWidth(config);
		int x = clamp((width - toolbarWidth) / 2 + config.toolbarOffsetX, 8, Math.max(8, width - toolbarWidth - 8));
		int y = clamp(height - 42 + config.toolbarOffsetY, 8, Math.max(8, height - config.toolbarSearchHeight - 8));
		return new UiRect(x, y, toolbarWidth, config.toolbarSearchHeight);
	}

	private UiRect browserBounds(SkyBlockLensConfig config) {
		int panelWidth = Math.min(config.itemBrowserWidth, Math.max(132, width - 12));
		int x = clamp(width - panelWidth - 4 + config.itemBrowserOffsetX, 4, Math.max(4, width - panelWidth - 4));
		int y = clamp(config.itemBrowserY, 4, Math.max(4, height - 118));
		int autoHeight = height - y - 4;
		int h = config.itemBrowserHeight <= 0 ? autoHeight : clamp(config.itemBrowserHeight, 118, Math.max(118, autoHeight));
		return new UiRect(x, y, panelWidth, h);
	}

	private static int toolbarWidth(SkyBlockLensConfig config) {
		int width = 0;
		if (config.featureEnabled("toolbar.search_bar")) {
			width += config.toolbarSearchWidth;
		}
		if (config.featureEnabled("toolbar.inventory_search_button")) {
			if (width > 0) {
				width += GAP;
			}
			width += ICON_BUTTON_WIDTH;
		}
		if (config.featureEnabled("itemlist.local_browser") && config.featureEnabled("itemlist.browser_overlay")) {
			if (width > 0) {
				width += GAP;
			}
			width += ICON_BUTTON_WIDTH;
		}
		return Math.max(1, width);
	}

	private static UiRect resizeHandle(UiRect toolbar, SkyBlockLensConfig config) {
		return new UiRect(toolbar.x() + config.toolbarSearchWidth - HANDLE_SIZE,
				toolbar.y() + config.toolbarSearchHeight - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
	}

	private static UiRect resizeHandle(UiRect rect) {
		return new UiRect(rect.right() - HANDLE_SIZE, rect.bottom() - HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
	}

	private void drawGrid(DrawContext context) {
		int color = 0x222D8C7D;
		for (int x = 16; x < width; x += 24) {
			context.fill(x, 40, x + 1, height - 34, color);
		}
		for (int y = 40; y < height - 34; y += 24) {
			context.fill(0, y, width, y + 1, color);
		}
	}

	private void drawResizeHandle(DrawContext context, UiRect rect, int accent) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xCC050709);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFFE6F4FF);
		context.fill(rect.x() + 2, rect.y() + 2, rect.right() - 1, rect.bottom() - 1, accent);
	}

	private void drawIconButton(DrawContext context, UiRect rect, int mouseX, int mouseY, boolean grid) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		double hover = hoverAnimation("icon:" + grid + ":" + rect.x() + ":" + rect.y(), rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(0xFF26323A, 0xFF35474F, hover));
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + Math.max(2, rect.height() / 2),
				blend(0xFF35464E, 0xFF4C6570, hover));
		drawBorder(context, rect, blend(0xFF070A0C, accent, hover * 0.55D));
		int cx = rect.centerX();
		int cy = rect.y() + rect.height() / 2;
		if (grid) {
			for (int iy = 0; iy < 2; iy++) {
				for (int ix = 0; ix < 2; ix++) {
					context.fill(cx - 6 + ix * 7, cy - 6 + iy * 7, cx - 1 + ix * 7, cy - 1 + iy * 7, 0xFFE6F4FF);
				}
			}
			return;
		}
		context.fill(cx - 5, cy - 4, cx + 2, cy - 3, 0xFFE6F4FF);
		context.fill(cx - 5, cy + 2, cx + 2, cy + 3, 0xFFE6F4FF);
		context.fill(cx - 5, cy - 4, cx - 4, cy + 3, 0xFFE6F4FF);
		context.fill(cx + 1, cy - 4, cx + 2, cy + 3, 0xFFE6F4FF);
		context.fill(cx + 3, cy + 4, cx + 7, cy + 5, 0xFFE6F4FF);
		context.fill(cx + 4, cy + 5, cx + 8, cy + 6, 0xFFE6F4FF);
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		double hover = hoverAnimation("button:" + label + ":" + rect.x() + ":" + rect.y(), rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(0xFF26323A, 0xFF35474F, hover));
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + Math.max(2, rect.height() / 2),
				blend(0xFF35464E, 0xFF4C6570, hover));
		drawBorder(context, rect, blend(0xFF070A0C, accent, hover * 0.55D));
		String fitted = fit(label, rect.width() - 8);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				rect.centerX() - textRenderer.getWidth(fitted) / 2, rect.y() + Math.max(4, (rect.height() - 8) / 2), 0xFFF3F2E8);
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

	private double hoverAnimation(String key, boolean hover) {
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

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private enum DragMode {
		NONE,
		MOVE,
		RESIZE,
		BROWSER_MOVE,
		BROWSER_RESIZE
	}
}
