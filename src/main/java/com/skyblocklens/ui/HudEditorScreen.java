package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class HudEditorScreen extends Screen {
	private static final int MIN_MODULE_WIDTH = 72;
	private static final int MIN_MODULE_HEIGHT = 22;
	private static final int HANDLE_SIZE = 5;

	private final Screen parent;
	private String dragging;
	private String resizing;
	private int dragOffsetX;
	private int dragOffsetY;
	private int resizeStartMouseX;
	private int resizeStartMouseY;
	private int resizeStartWidth;
	private int resizeStartHeight;
	private UiRect backButton = new UiRect(0, 0, 0, 0);
	private final Map<String, Double> hoverAnimations = new HashMap<>();

	public HudEditorScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.hud.editor.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		backButton = new UiRect(width - 92, height - 28, 80, 20);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xDD070A10);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.hud.editor.title")), 12, 10, 0xE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.hud.editor.hint")), 12, 24, 0xAAB7C4);

		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int accent = config.accentArgb();
		if (config.featureEnabled("gui.show_anchor_grid")) {
			drawGrid(context, accent);
		}
		for (Map.Entry<String, String> entry : SkyBlockLensClient.hudManager().previewLines().entrySet()) {
			SkyBlockLensConfig.HudModuleConfig module = config.hudModules.get(entry.getKey());
			if (module == null || !module.enabled) {
				continue;
			}
			double baseScale = module.scale <= 0.0D ? 1.0D : module.scale;
			int moduleWidth = moduleWidth(module, entry.getValue(), baseScale);
			int moduleHeight = moduleHeight(module, baseScale);
			double scale = displayScale(module, moduleWidth, moduleHeight, entry.getValue());
			context.fill(module.x - 4, module.y - 4, module.x + moduleWidth, module.y + moduleHeight, config.hudBackgroundArgb());
			drawBorder(context, new UiRect(module.x - 4, module.y - 4, moduleWidth + 4, moduleHeight + 4), 0xAA050709);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(module.x + 5,
					module.y + Math.max(5, (int) Math.round((moduleHeight - 8 * scale) / 2.0D)));
			context.getMatrices().scale((float) scale, (float) scale);
			context.drawTextWithShadow(textRenderer, Text.literal(entry.getValue()), 0, 0, 0xFFFFFFFF);
			context.getMatrices().popMatrix();
			drawResizeHandle(context, resizeHandle(module, moduleWidth, moduleHeight), accent);
		}
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (backButton.contains(click.x(), click.y()) && click.button() == 0) {
			close();
			return true;
		}
		if (SkyBlockLensClient.configStore().config().featureEnabled("gui.lock_layout")) {
			return super.mouseClicked(click, doubled);
		}
		if (click.button() == 0) {
			for (Map.Entry<String, String> entry : SkyBlockLensClient.hudManager().previewLines().entrySet()) {
				SkyBlockLensConfig.HudModuleConfig module = SkyBlockLensClient.configStore().config().hudModules.get(entry.getKey());
				if (module == null || !module.enabled) {
					continue;
				}
				double scale = module.scale <= 0.0D ? 1.0D : module.scale;
				int moduleWidth = moduleWidth(module, entry.getValue(), scale);
				int moduleHeight = moduleHeight(module, scale);
				if (resizeHandle(module, moduleWidth, moduleHeight).contains(click.x(), click.y())) {
					resizing = entry.getKey();
					resizeStartMouseX = (int) click.x();
					resizeStartMouseY = (int) click.y();
					resizeStartWidth = moduleWidth;
					resizeStartHeight = moduleHeight;
					return true;
				}
				if (click.x() >= module.x - 4 && click.x() <= module.x + moduleWidth
						&& click.y() >= module.y - 4 && click.y() <= module.y + moduleHeight) {
					dragging = entry.getKey();
					dragOffsetX = (int) click.x() - module.x;
					dragOffsetY = (int) click.y() - module.y;
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (resizing != null) {
			SkyBlockLensConfig.HudModuleConfig module = SkyBlockLensClient.configStore().config().hudModules.get(resizing);
			if (module != null) {
				module.width = clamp(resizeStartWidth + (int) click.x() - resizeStartMouseX, MIN_MODULE_WIDTH, Math.max(MIN_MODULE_WIDTH, width - module.x - 8));
				module.height = clamp(resizeStartHeight + (int) click.y() - resizeStartMouseY, MIN_MODULE_HEIGHT, Math.max(MIN_MODULE_HEIGHT, height - module.y - 8));
			}
			return true;
		}
		if (dragging != null) {
			SkyBlockLensConfig.HudModuleConfig module = SkyBlockLensClient.configStore().config().hudModules.get(dragging);
			if (module != null) {
				int x = clamp((int) click.x() - dragOffsetX, 4, width - 40);
				int y = clamp((int) click.y() - dragOffsetY, 40, height - 40);
				if (SkyBlockLensClient.configStore().config().featureEnabled("gui.snap_to_edges")) {
					x = snap(x, 4, width - 40);
					y = snap(y, 40, height - 40);
				}
				module.x = x;
				module.y = y;
			}
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragging != null || resizing != null) {
			dragging = null;
			resizing = null;
			SkyBlockLensClient.hudManager().save();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void close() {
		SkyBlockLensClient.hudManager().save();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int snap(int value, int min, int max) {
		if (Math.abs(value - min) <= 8) {
			return min;
		}
		if (Math.abs(value - max) <= 8) {
			return max;
		}
		return value;
	}

	private void drawGrid(DrawContext context, int accent) {
		int color = 0x33000000 | (accent & 0x00FFFFFF);
		for (int x = 16; x < width; x += 24) {
			context.fill(x, 40, x + 1, height - 36, color);
		}
		for (int y = 40; y < height - 36; y += 24) {
			context.fill(0, y, width, y + 1, color);
		}
	}

	private int moduleWidth(SkyBlockLensConfig.HudModuleConfig module, String label, double scale) {
		int natural = Math.max(MIN_MODULE_WIDTH, (int) Math.round((textRenderer.getWidth(label) + 16) * scale));
		return module.width > 0 ? module.width : natural;
	}

	private static int moduleHeight(SkyBlockLensConfig.HudModuleConfig module, double scale) {
		int natural = Math.max(MIN_MODULE_HEIGHT, (int) Math.round(18 * scale));
		return module.height > 0 ? module.height : natural;
	}

	private double displayScale(SkyBlockLensConfig.HudModuleConfig module, int width, int height, String label) {
		if (module.width > 0 || module.height > 0) {
			double heightScale = (height - 10.0D) / 8.0D;
			double textWidth = Math.max(1.0D, textRenderer.getWidth(label));
			double widthScale = (width - 14.0D) / textWidth;
			return Math.max(0.55D, Math.min(2.5D, Math.min(widthScale, heightScale)));
		}
		return module.scale <= 0.0D ? 1.0D : module.scale;
	}

	private static UiRect resizeHandle(SkyBlockLensConfig.HudModuleConfig module, int moduleWidth, int moduleHeight) {
		return new UiRect(module.x + moduleWidth - HANDLE_SIZE, module.y + moduleHeight - HANDLE_SIZE,
				HANDLE_SIZE, HANDLE_SIZE);
	}

	private void drawResizeHandle(DrawContext context, UiRect rect, int accent) {
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xCC050709);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFFE6F4FF);
		context.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, accent);
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		double hover = hoverAnimation("button:" + label + ":" + rect.x() + ":" + rect.y(), rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(0xFF26323A, 0xFF35474F, hover));
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + rect.height() / 2,
				blend(0xFF35464E, 0xFF4C6570, hover));
		drawBorder(context, rect, blend(0xFF070A0C, accent, hover * 0.55D));
		String fitted = fit(label, rect.width() - 8);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted), rect.centerX() - textRenderer.getWidth(fitted) / 2,
				rect.y() + 6, 0xFFF3F2E8);
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
}
