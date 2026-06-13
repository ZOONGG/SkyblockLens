package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.ConfigStore;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.config.SkyBlockSetting;
import com.skyblocklens.config.SkyBlockSettingCategory;
import com.skyblocklens.config.SkyBlockSettingControl;
import com.skyblocklens.config.SkyBlockSettingGroup;
import com.skyblocklens.config.SkyBlockSettingPage;
import com.skyblocklens.config.SkyBlockSettingsCatalog;
import com.skyblocklens.i18n.SblI18n;
import com.skyblocklens.slotlocking.SlotLockController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SettingsScreen extends Screen {
	private static final int PANEL_PADDING = 12;
	private static final int HEADER_HEIGHT = 46;
	private static final int CATEGORY_ROW_HEIGHT = 24;
	private static final int GROUP_ROW_HEIGHT = 34;
	private static final int SETTING_ROW_HEIGHT = 72;
	private static final int FOOTER_HEIGHT = 44;
	private static final int TOGGLE_WIDTH = 62;
	private static final int TOGGLE_HEIGHT = 18;
	private static final int SLIDER_WIDTH = 148;
	private static final int SLIDER_HEIGHT = 14;

	private final Screen parent;
	private final Set<String> expandedGroups = new HashSet<>();
	private final Set<String> collapsedGroups = new HashSet<>();
	private final List<Hitbox> hitboxes = new ArrayList<>();
	private final Map<String, Double> toggleAnimations = new HashMap<>();
	private final Map<String, Double> hoverAnimations = new HashMap<>();
	private final Map<String, String> colorDrafts = new HashMap<>();
	private final Map<String, Float> colorHues = new HashMap<>();

	private SkyBlockSettingCategory selectedCategory = SkyBlockSettingCategory.ABOUT;
	private TextFieldWidget search;
	private String searchValue = "";
	private String openDropdownSetting = "";
	private String openColorSetting = "";
	private String activeKeybindSetting = "";
	private String activeHexSetting = "";
	private boolean replaceHexDraftOnInput;
	private int openColorAnchorY;
	private boolean confirmingReset;
	private int categoryScroll;
	private int contentScroll;
	private int maxCategoryScroll;
	private int maxContentScroll;
	private Hitbox draggingSlider;
	private Hitbox draggingColor;
	private long openedAtMillis = System.currentTimeMillis();
	private int renderMouseX;
	private int renderMouseY;
	private int lastWidth;
	private int lastHeight;
	private double lastScale;

	public SettingsScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.config.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		lastWidth = width;
		lastHeight = height;
		lastScale = uiScale();
		Layout layout = layout();
		search = new TextFieldWidget(
				textRenderer,
				layout.contentX(),
				layout.searchY(),
				layout.contentWidth(),
				22,
				Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.config.search"))
		);
		search.setPlaceholder(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.config.search")));
		search.setText(searchValue);
		search.setChangedListener(value -> {
			searchValue = value;
			contentScroll = 0;
		});
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		double scale = uiScale();
		if (width != lastWidth || height != lastHeight || Double.compare(scale, lastScale) != 0) {
			init();
		}
		hitboxes.clear();
		SblI18n i18n = SkyBlockLensClient.i18n();
		Layout layout = layout();
		clampScroll();

		context.fill(0, 0, width, height, 0xB404070B);
		context.getMatrices().pushMatrix();
		context.getMatrices().scale((float) scale, (float) scale);
		int uiMouseX = (int) Math.round(mouseX / scale);
		int uiMouseY = (int) Math.round(mouseY / scale);
		renderMouseX = uiMouseX;
		renderMouseY = uiMouseY;
		float openOffset = (float) ((1.0D - openingProgress()) * -12.0D);
		context.getMatrices().translate(0.0F, openOffset);
		drawPanel(context, layout);
		drawHeader(context, layout, i18n);
		drawCategories(context, layout, i18n);
		drawContent(context, layout, i18n, uiMouseX, uiMouseY);
		drawFooter(context, layout, i18n);
		search.render(context, uiMouseX, uiMouseY, delta);
		drawColorPopup(context, layout);
		if (confirmingReset) {
			drawResetConfirmation(context, layout, i18n);
		}
		context.getMatrices().popMatrix();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		Click uiClick = uiClick(click);
		double mouseX = uiClick.x();
		double mouseY = uiClick.y();
		if (confirmingReset) {
			for (int i = hitboxes.size() - 1; i >= 0; i--) {
				Hitbox hitbox = hitboxes.get(i);
				if (!hitbox.contains(mouseX, mouseY)) {
					continue;
				}
				if (hitbox.type() == HitboxType.RESET_CONFIRM) {
					confirmingReset = false;
					resetConfig();
					return true;
				}
				if (hitbox.type() == HitboxType.RESET_CANCEL || hitbox.type() == HitboxType.BACK) {
					confirmingReset = false;
					return true;
				}
			}
			return true;
		}
		if (!activeKeybindSetting.isBlank()) {
			activeKeybindSetting = "";
		}
		if (search.mouseClicked(uiClick, doubleClick)) {
			setFocused(search);
			activeHexSetting = "";
			replaceHexDraftOnInput = false;
			return true;
		}
		search.setFocused(false);
		setFocused(null);
		for (int i = hitboxes.size() - 1; i >= 0; i--) {
			Hitbox hitbox = hitboxes.get(i);
			if (!hitbox.contains(mouseX, mouseY)) {
				continue;
			}
			switch (hitbox.type()) {
				case CATEGORY -> {
					selectedCategory = hitbox.category();
					contentScroll = 0;
					return true;
				}
				case GROUP -> {
					toggleGroup(hitbox.group());
					return true;
				}
				case TOGGLE -> {
					toggleSetting(hitbox.setting());
					return true;
				}
				case ACTION -> {
					performAction(hitbox.setting());
					return true;
				}
				case DROPDOWN -> {
					openDropdownSetting = hitbox.setting().id().equals(openDropdownSetting) ? "" : hitbox.setting().id();
					return true;
				}
				case DROPDOWN_OPTION -> {
					selectDropdownOption(hitbox.setting(), hitbox.value());
					return true;
				}
				case SLIDER -> {
					draggingSlider = hitbox;
					setSliderFromMouse(mouseX, hitbox);
					return true;
				}
				case KEYBIND -> {
					activeKeybindSetting = hitbox.setting().id();
					activeHexSetting = "";
					replaceHexDraftOnInput = false;
					return true;
				}
				case KEYBIND_CLEAR -> {
					activeKeybindSetting = hitbox.setting().id();
					setKeybind(InputUtil.UNKNOWN_KEY);
					activeKeybindSetting = "";
					return true;
				}
				case COLOR_SWATCH -> {
					String id = hitbox.setting().id();
					if (id.equals(openColorSetting)) {
						openColorSetting = "";
						activeHexSetting = "";
						replaceHexDraftOnInput = false;
						return true;
					}
					openColorSetting = id;
					activeHexSetting = "";
					replaceHexDraftOnInput = false;
					openColorAnchorY = hitbox.y();
					colorDrafts.put(id, colorValue(hitbox.setting()));
					return true;
				}
				case COLOR_PICKER -> {
					draggingColor = hitbox;
					activeHexSetting = "";
					replaceHexDraftOnInput = false;
					setColorFromPicker(mouseX, mouseY, hitbox);
					return true;
				}
				case COLOR_HUE -> {
					draggingColor = hitbox;
					activeHexSetting = "";
					replaceHexDraftOnInput = false;
					setColorFromHue(mouseY, hitbox);
					return true;
				}
				case COLOR_HEX -> {
					String id = hitbox.setting().id();
					activeHexSetting = id;
					openColorSetting = "";
					replaceHexDraftOnInput = true;
					openColorAnchorY = hitbox.y();
					colorDrafts.put(id, colorValue(hitbox.setting()));
					return true;
				}
				case RESET -> {
					confirmingReset = true;
					return true;
				}
				case BACK -> {
					close();
					return true;
				}
			}
		}
		if (!openDropdownSetting.isBlank()) {
			openDropdownSetting = "";
			return true;
		}
		if (!openColorSetting.isBlank()) {
			openColorSetting = "";
			activeHexSetting = "";
			replaceHexDraftOnInput = false;
			return true;
		}
		activeHexSetting = "";
		return super.mouseClicked(uiClick, doubleClick);
	}

	@Override
	public boolean mouseReleased(Click click) {
		draggingSlider = null;
		draggingColor = null;
		return super.mouseReleased(uiClick(click));
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		Click uiClick = uiClick(click);
		if (draggingSlider != null) {
			setSliderFromMouse(uiClick.x(), draggingSlider);
			return true;
		}
		if (draggingColor != null) {
			if (draggingColor.type() == HitboxType.COLOR_HUE) {
				setColorFromHue(uiClick.y(), draggingColor);
			} else {
				setColorFromPicker(uiClick.x(), uiClick.y(), draggingColor);
			}
			return true;
		}
		double scale = uiScale();
		return super.mouseDragged(uiClick, deltaX / scale, deltaY / scale);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		Layout layout = layout();
		double scale = uiScale();
		mouseX /= scale;
		mouseY /= scale;
		int delta = (int) Math.round(verticalAmount * 28.0D);
		if (mouseX >= layout.categoryX() && mouseX <= layout.categoryX() + layout.categoryWidth()
				&& mouseY >= layout.listTop() && mouseY <= layout.contentBottom()) {
			categoryScroll = MathHelper.clamp(categoryScroll - delta, 0, maxCategoryScroll);
			return true;
		}
		if (mouseX >= layout.contentX() && mouseX <= layout.contentX() + layout.contentWidth()
				&& mouseY >= layout.contentTop() && mouseY <= layout.contentBottom()) {
			contentScroll = MathHelper.clamp(contentScroll - delta, 0, maxContentScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (!activeKeybindSetting.isBlank()) {
			handleKeybindInput(keyInput);
			return true;
		}
		if (!activeHexSetting.isBlank() && handleHexKey(keyInput)) {
			return true;
		}
		if (search != null && search.isFocused() && search.keyPressed(keyInput)) {
			return true;
		}
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (!activeHexSetting.isBlank()) {
			handleHexChar(charInput);
			return true;
		}
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

	private void drawPanel(DrawContext context, Layout layout) {
		context.fill(layout.panelX() - 2, layout.panelY() - 2, layout.panelX() + layout.panelWidth() + 2,
				layout.panelY() + layout.panelHeight() + 2, 0xFF030609);
		context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
				layout.panelY() + layout.panelHeight(), 0xFF11161B);
		context.fill(layout.panelX() + 3, layout.panelY() + 3, layout.panelX() + layout.panelWidth() - 3,
				layout.panelY() + layout.panelHeight() - 3, 0xFF1B2027);
		context.fillGradient(layout.panelX() + 4, layout.panelY() + 4, layout.panelX() + layout.panelWidth() - 4,
				layout.panelY() + 38, 0x662E6B65, 0x221E2330);
		context.fill(layout.categoryX(), layout.listTop(), layout.categoryX() + layout.categoryWidth(),
				layout.contentBottom(), 0xFF090D12);
		context.fill(layout.contentX() - 8, layout.listTop(), layout.contentX() + layout.contentWidth() + 8,
				layout.contentBottom(), 0xFF11151B);
		drawBorder(context, layout.categoryX(), layout.listTop(), layout.categoryWidth(), layout.contentBottom() - layout.listTop(), 0xFF2D3A3A);
		drawBorder(context, layout.contentX() - 8, layout.listTop(), layout.contentWidth() + 16,
				layout.contentBottom() - layout.listTop(), 0xFF2D3A3A);
	}

	private void drawHeader(DrawContext context, Layout layout, SblI18n i18n) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(layout.panelX() + 8, layout.panelY() + 8, layout.panelX() + layout.panelWidth() - 8,
				layout.panelY() + HEADER_HEIGHT - 2, 0xFF151A20);
		drawBorder(context, layout.panelX() + 8, layout.panelY() + 8, layout.panelWidth() - 16, HEADER_HEIGHT - 10, 0xFF31403F);
		String title = i18n.tr("skyblocklens.config.header").replace("{version}", modVersion());
		drawCentered(context, title, layout.panelX() + layout.panelWidth() / 2, layout.panelY() + 20, 0xFFF3F2E8);
		int accentWidth = 160 + (int) (Math.sin((System.currentTimeMillis() - openedAtMillis) / 430.0D) * 24.0D);
		int accentX = layout.panelX() + layout.panelWidth() / 2 - accentWidth / 2;
		context.fill(accentX, layout.panelY() + HEADER_HEIGHT - 6, accentX + accentWidth, layout.panelY() + HEADER_HEIGHT - 4, accent);
		int titleY = layout.panelY() + HEADER_HEIGHT + 12;
		drawFittedText(context, i18n.tr(selectedCategory.key()), layout.contentX(), titleY,
				layout.contentWidth(), accent);
		drawFittedText(context, i18n.tr("skyblocklens.category_desc." + selectedCategory.id()), layout.contentX(),
				titleY + 13, layout.contentWidth(), 0xFFB7C9D1);
		int categoriesY = layout.listTop() - 28;
		drawFittedText(context, i18n.tr("skyblocklens.config.categories"), layout.categoryX() + 14,
				categoriesY, layout.categoryWidth() - 28, accent);
		drawFittedText(context, i18n.tr("skyblocklens.config.categories"), layout.categoryX() + 15,
				categoriesY + 1, layout.categoryWidth() - 28, accent);
		context.fill(layout.categoryX() + 14, categoriesY + 13,
				layout.categoryX() + Math.min(layout.categoryWidth() - 20, 104), categoriesY + 15, accent);
	}

	private void drawCategories(DrawContext context, Layout layout, SblI18n i18n) {
		List<SkyBlockSettingPage> pages = SkyBlockSettingsCatalog.pages();
		int totalHeight = pages.size() * CATEGORY_ROW_HEIGHT;
		maxCategoryScroll = Math.max(0, totalHeight - (layout.contentBottom() - layout.listTop() - 8));
		categoryScroll = MathHelper.clamp(categoryScroll, 0, maxCategoryScroll);

		enableUiScissor(context, layout.categoryX() + 2, layout.listTop() + 2,
				layout.categoryX() + layout.categoryWidth() - 7, layout.contentBottom() - 2);
		int y = layout.listTop() + 8 - categoryScroll;
		for (SkyBlockSettingPage page : pages) {
			SkyBlockSettingCategory category = page.category();
			if (y + CATEGORY_ROW_HEIGHT >= layout.listTop() && y <= layout.contentBottom()) {
				boolean selected = category == selectedCategory;
				int rowColor = selected ? 0xFF17252A : 0x00000000;
				context.fill(layout.categoryX() + 6, y, layout.categoryX() + layout.categoryWidth() - 6,
						y + CATEGORY_ROW_HEIGHT - 2, rowColor);
				int color = selected ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFFE5E1D3;
				drawFittedText(context, i18n.tr(category.key()), layout.categoryX() + 14, y + 7,
						layout.categoryWidth() - 28, color);
				if (selected) {
					context.fill(layout.categoryX() + 10, y + CATEGORY_ROW_HEIGHT - 4,
							layout.categoryX() + Math.min(layout.categoryWidth() - 14, 142), y + CATEGORY_ROW_HEIGHT - 2,
							SkyBlockLensClient.configStore().config().accentArgb());
				}
				hitboxes.add(Hitbox.category(layout.categoryX() + 6, y, layout.categoryWidth() - 12,
						CATEGORY_ROW_HEIGHT - 2, category));
			}
			y += CATEGORY_ROW_HEIGHT;
		}
		context.disableScissor();
		drawScrollbar(context, layout.categoryX() + layout.categoryWidth() - 6, layout.listTop() + 8,
				layout.contentBottom() - 8, categoryScroll, maxCategoryScroll);
	}

	private void drawContent(DrawContext context, Layout layout, SblI18n i18n, int mouseX, int mouseY) {
		SkyBlockSettingPage page = SkyBlockSettingsCatalog.page(selectedCategory);
		if (page == null) {
			return;
		}
		String query = normalized(searchValue);
		int y = layout.contentTop() - contentScroll;
		int totalHeight = 0;
		enableUiScissor(context, layout.contentX() - 4, layout.contentTop(),
				layout.contentX() + layout.contentWidth() + 2, layout.contentBottom() - 4);
		for (SkyBlockSettingGroup group : page.groups()) {
			List<SkyBlockSetting> settings = filteredSettings(i18n, group, query);
			if (!query.isBlank() && settings.isEmpty() && !matches(query, i18n.tr(group.titleKey()))) {
				continue;
			}
			boolean groupHeaderVisible = shouldDrawGroupHeader(page, group);
			if (groupHeaderVisible) {
				drawGroupRow(context, layout, i18n, group, y);
				y += GROUP_ROW_HEIGHT;
				totalHeight += GROUP_ROW_HEIGHT;
			}
			if (isExpanded(group, query)) {
				for (SkyBlockSetting setting : settings) {
					drawSettingRow(context, layout, i18n, setting, y, mouseX, mouseY);
					int rowHeight = settingRowHeight(setting);
					y += rowHeight;
					totalHeight += rowHeight;
				}
			}
		}
		context.disableScissor();
		maxContentScroll = Math.max(0, totalHeight - (layout.contentBottom() - layout.contentTop() - 4));
		contentScroll = MathHelper.clamp(contentScroll, 0, maxContentScroll);
		drawScrollbar(context, layout.contentX() + layout.contentWidth() + 3, layout.contentTop(),
				layout.contentBottom() - 5, contentScroll, maxContentScroll);
	}

	private void drawGroupRow(DrawContext context, Layout layout, SblI18n i18n, SkyBlockSettingGroup group, int y) {
		if (y + GROUP_ROW_HEIGHT < layout.contentTop() || y > layout.contentBottom()) {
			return;
		}
		int x = layout.contentX();
		int w = layout.contentWidth();
		context.fill(x, y + 2, x + w, y + GROUP_ROW_HEIGHT - 3, 0xFF202932);
		context.fill(x + 3, y + GROUP_ROW_HEIGHT - 5, x + w - 3, y + GROUP_ROW_HEIGHT - 3,
				SkyBlockLensClient.configStore().config().accentArgb());
		String prefix = canCollapse(group) ? (isExpanded(group, normalized(searchValue)) ? "- " : "+ ") : "";
		drawFittedText(context, prefix + i18n.tr(group.titleKey()), x + 12, y + 11, w - 24, 0xFFF3F2E8);
		if (canCollapse(group)) {
			hitboxes.add(Hitbox.group(x, y + 2, w, GROUP_ROW_HEIGHT - 5, group));
		}
	}

	private void drawSettingRow(DrawContext context, Layout layout, SblI18n i18n, SkyBlockSetting setting, int y, int mouseX, int mouseY) {
		int rowHeight = settingRowHeight(setting);
		if (y + rowHeight < layout.contentTop() || y > layout.contentBottom()) {
			return;
		}
		int x = layout.contentX();
		int w = layout.contentWidth();
		boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y + 3 && mouseY <= y + rowHeight - 5;
		context.fill(x, y + 3, x + w, y + rowHeight - 5, hover ? 0xFF222B31 : 0xFF1C2028);
		drawBorder(context, x, y + 3, w, rowHeight - 8, hover ? 0xFF4A5B55 : 0xFF303A40);

		int controlWidth = controlWidth(setting);
		int titleWidth = Math.min(360, Math.max(220, w / 3 + 24));
		int descriptionX = x + titleWidth + 14;
		int descriptionWidth = Math.max(96, w - titleWidth - controlWidth - 40);
		drawWrapped(context, i18n.tr(setting.titleKey()), x + 12, y + 11, titleWidth - 22, 0xFFF0F0F6, 3);
		drawWrapped(context, i18n.tr(setting.descriptionKey()), descriptionX, y + 12,
				descriptionWidth, 0xFFD8D3C6, 3);

		switch (setting.control()) {
			case TOGGLE -> {
				UiRect toggle = rightControl(layout, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, 27);
				drawToggle(context, toggle, setting.id(), isSettingEnabled(setting));
				hitboxes.add(Hitbox.setting(toggle.expand(4), HitboxType.TOGGLE, setting));
			}
			case ACTION -> drawAction(context, layout, i18n, setting, y);
			case DROPDOWN -> drawDropdown(context, layout, i18n, setting, y);
			case SLIDER -> drawSlider(context, layout, y, setting);
			case KEYBIND -> drawKeybind(context, layout, i18n, setting, y);
			case COLOR -> drawColor(context, layout, setting, y);
		}
	}

	private void drawFooter(DrawContext context, Layout layout, SblI18n i18n) {
		int y = layout.panelY() + layout.panelHeight() - FOOTER_HEIGHT + 8;
		UiRect reset = new UiRect(layout.contentX(), y, 106, 22);
		UiRect back = new UiRect(reset.right() + 10, y, 90, 22);
		drawSmallButton(context, reset, i18n.tr("skyblocklens.config.reset"));
		drawSmallButton(context, back, i18n.tr("skyblocklens.config.back"));
		hitboxes.add(Hitbox.simple(reset, HitboxType.RESET));
		hitboxes.add(Hitbox.simple(back, HitboxType.BACK));
	}

	private void drawResetConfirmation(DrawContext context, Layout layout, SblI18n i18n) {
		int modalW = Math.min(360, layout.panelWidth() - 80);
		int modalH = 118;
		int x = layout.panelX() + (layout.panelWidth() - modalW) / 2;
		int y = layout.panelY() + (layout.panelHeight() - modalH) / 2;
		context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
				layout.panelY() + layout.panelHeight(), 0xAA000000);
		context.fill(x, y, x + modalW, y + modalH, 0xFF111821);
		context.fill(x + 1, y + 1, x + modalW - 1, y + 30, 0xFF1D2830);
		drawBorder(context, x, y, modalW, modalH, 0xFF3D4E52);
		drawFittedText(context, i18n.tr("skyblocklens.config.reset_confirm.title"), x + 14, y + 12, modalW - 28,
				SkyBlockLensClient.configStore().config().accentArgb());
		drawWrapped(context, i18n.tr("skyblocklens.config.reset_confirm.desc"), x + 14, y + 42, modalW - 28,
				0xFFE5E1D3, 2);
		UiRect confirm = new UiRect(x + modalW - 188, y + modalH - 30, 82, 20);
		UiRect cancel = new UiRect(x + modalW - 96, y + modalH - 30, 82, 20);
		drawSmallButton(context, confirm, i18n.tr("skyblocklens.config.reset_confirm.confirm"));
		drawSmallButton(context, cancel, i18n.tr("skyblocklens.config.reset_confirm.cancel"));
		hitboxes.add(Hitbox.simple(confirm, HitboxType.RESET_CONFIRM));
		hitboxes.add(Hitbox.simple(cancel, HitboxType.RESET_CANCEL));
	}

	private void drawToggle(DrawContext context, UiRect rect, String id, boolean enabled) {
		double current = toggleAnimations.getOrDefault(id, enabled ? 1.0D : 0.0D);
		double target = enabled ? 1.0D : 0.0D;
		current += (target - current) * 0.28D;
		if (Math.abs(target - current) < 0.01D) {
			current = target;
		}
		toggleAnimations.put(id, current);
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF0B0F13);
		context.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2,
				enabled ? 0x5522C8A8 : 0x553E2222);
		drawBorder(context, rect, enabled ? accent : 0xFF334246);
		for (int tick = 14; tick <= 48; tick += 11) {
			context.fill(rect.x() + tick, rect.y() + 5, rect.x() + tick + 1, rect.y() + 13, 0xFF2C393B);
		}
		int knobX = rect.x() + 5 + (int) Math.round(current * 40.0D);
		int knobColor = enabled ? accent : 0xFFE06A4D;
		context.fill(knobX, rect.y() - 2, knobX + 10, rect.y() + TOGGLE_HEIGHT + 2, 0xFF050708);
		context.fill(knobX + 2, rect.y(), knobX + 8, rect.y() + TOGGLE_HEIGHT, knobColor);
	}

	private void drawAction(DrawContext context, Layout layout, SblI18n i18n, SkyBlockSetting setting, int y) {
		String label = actionLabel(i18n, setting);
		int buttonWidth = Math.min(104, Math.max(66, textRenderer.getWidth(label) + 18));
		UiRect button = rightControl(layout, y, buttonWidth, 22, 26);
		drawSmallButton(context, button, label);
		hitboxes.add(Hitbox.setting(button, HitboxType.ACTION, setting));
	}

	private void drawDropdown(DrawContext context, Layout layout, SblI18n i18n, SkyBlockSetting setting, int y) {
		int buttonW = 132;
		UiRect button = rightControl(layout, y, buttonW, 24, 24);
		String current = dropdownValue(setting);
		drawSmallButton(context, button, dropdownLabel(i18n, setting, current));
		int arrowX = button.right() - 16;
		context.fill(arrowX, button.y() + 9, arrowX + 9, button.y() + 11, SkyBlockLensClient.configStore().config().accentArgb());
		context.fill(arrowX + 2, button.y() + 12, arrowX + 7, button.y() + 14, SkyBlockLensClient.configStore().config().accentArgb());
		hitboxes.add(Hitbox.setting(button, HitboxType.DROPDOWN, setting));
		if (!setting.id().equals(openDropdownSetting)) {
			return;
		}
		String[] options = dropdownOptions(setting);
		int optionY = button.bottom() + 2;
		for (String option : options) {
			boolean selected = option.equals(current);
			UiRect optionBounds = new UiRect(button.x(), optionY, button.width(), 22);
			context.fill(optionBounds.x(), optionBounds.y(), optionBounds.right(), optionBounds.bottom(), selected ? 0xFF24443F : 0xFF121820);
			drawBorder(context, optionBounds, selected ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF2B3740);
			drawFittedText(context, dropdownLabel(i18n, setting, option), optionBounds.x() + 10, optionBounds.y() + 7, optionBounds.width() - 20,
					selected ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFFE5E1D3);
			hitboxes.add(Hitbox.option(optionBounds, setting, option));
			optionY = optionBounds.bottom();
		}
	}

	private void drawSlider(DrawContext context, Layout layout, int y, SkyBlockSetting setting) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		UiRect track = rightControl(layout, y, SLIDER_WIDTH, SLIDER_HEIGHT, 38);
		int trackLineY = track.y() + 3;
		double progress = sliderProgress(setting, config);
		context.fill(track.x(), trackLineY, track.right(), trackLineY + 6, 0xFF0B0F13);
		drawBorder(context, track, 0xFF334246);
		int knobX = track.x() + (int) Math.round(progress * track.width());
		context.fill(knobX - 4, trackLineY - 10, knobX + 4, trackLineY + 16, 0xFFF3F2E8);
		context.fill(knobX - 2, trackLineY - 8, knobX + 2, trackLineY + 14, config.accentArgb());
		drawCentered(context, sliderLabel(setting, config), track.centerX(), y + 18, 0xFFE5E1D3);
		hitboxes.add(Hitbox.setting(track.expand(8), HitboxType.SLIDER, setting));
	}

	private void drawKeybind(DrawContext context, Layout layout, SblI18n i18n, SkyBlockSetting setting, int y) {
		KeyBinding keyBinding = keyBinding(setting);
		if (keyBinding == null) {
			return;
		}
		boolean active = setting.id().equals(activeKeybindSetting);
		String label = active ? i18n.tr("skyblocklens.config.keybind.press_any") : keyBinding.getBoundKeyLocalizedText().getString();
		UiRect group = rightControl(layout, y, 158, 22, 24);
		UiRect button = new UiRect(group.x(), group.y(), 126, group.height());
		UiRect clear = new UiRect(group.right() - 24, group.y(), 24, group.height());
		drawSmallButton(context, button, label);
		drawIconButton(context, clear, IconButton.TRASH);
		hitboxes.add(Hitbox.setting(button, HitboxType.KEYBIND, setting));
		hitboxes.add(Hitbox.setting(clear, HitboxType.KEYBIND_CLEAR, setting));
	}

	private void drawColor(DrawContext context, Layout layout, SkyBlockSetting setting, int y) {
		UiRect group = rightControl(layout, y, 190, 38, 17);
		int current = colorArgb(setting);
		UiRect swatch = new UiRect(group.x(), group.y(), 34, 34);
		UiRect hex = new UiRect(swatch.right() + 10, group.y() + 6, 138, 22);
		boolean popupOpen = setting.id().equals(openColorSetting);
		boolean hexActive = setting.id().equals(activeHexSetting);
		context.fill(swatch.x(), swatch.y(), swatch.right(), swatch.bottom(), current);
		drawBorder(context, swatch, popupOpen ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF070A0C);
		hitboxes.add(Hitbox.setting(swatch.expand(2), HitboxType.COLOR_SWATCH, setting));
		context.fill(hex.x(), hex.y(), hex.right(), hex.bottom(), hexActive ? 0xFF05090E : 0xFF101820);
		drawBorder(context, hex, hexActive ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF334246);
		String value = hexActive ? colorDrafts.getOrDefault(setting.id(), colorValue(setting)) : colorValue(setting);
		drawFittedText(context, value, hex.x() + 7, hex.y() + 7, hex.width() - 14, 0xFFE6F4FF);
		if (hexActive && (System.currentTimeMillis() / 450L) % 2L == 0L) {
			int cursorX = Math.min(hex.right() - 7, hex.x() + 7 + textRenderer.getWidth(value));
			context.fill(cursorX, hex.y() + 5, cursorX + 1, hex.bottom() - 5, 0xFFE6F4FF);
		}
		hitboxes.add(Hitbox.setting(hex, HitboxType.COLOR_HEX, setting));
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label) {
		String key = "button:" + label + ":" + rect.x() + ":" + rect.y() + ":" + rect.width() + ":" + rect.height();
		double hover = hoverAnimation(key, rect.contains(renderMouseX, renderMouseY));
		int lift = (int) Math.round(hover);
		UiRect drawRect = new UiRect(rect.x(), rect.y() - lift, rect.width(), rect.height());
		int bg = blend(0xFF26323A, 0xFF35474F, hover);
		int top = blend(0xFF35464E, 0xFF4C6570, hover);
		int border = blend(0xFF070A0C, SkyBlockLensClient.configStore().config().accentArgb(), hover * 0.65D);
		context.fill(drawRect.x(), drawRect.y(), drawRect.right(), drawRect.bottom(), bg);
		context.fill(drawRect.x() + 1, drawRect.y() + 1, drawRect.right() - 1, drawRect.y() + drawRect.height() / 2, top);
		drawBorder(context, drawRect, border);
		drawCentered(context, fit(label, drawRect.width() - 8), drawRect.centerX(), drawRect.y() + 7, 0xFFF3F2E8);
	}

	private void drawIconButton(DrawContext context, UiRect rect, IconButton icon) {
		String key = "icon:" + icon + ":" + rect.x() + ":" + rect.y();
		double hover = hoverAnimation(key, rect.contains(renderMouseX, renderMouseY));
		int bg = blend(0xFF26323A, 0xFF35474F, hover);
		int border = blend(0xFF070A0C, SkyBlockLensClient.configStore().config().accentArgb(), hover * 0.65D);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), bg);
		drawBorder(context, rect, border);
		if (icon == IconButton.TRASH) {
			int color = blend(0xFFE6F4FF, 0xFFFFFFFF, hover);
			context.fill(rect.x() + 7, rect.y() + 7, rect.right() - 7, rect.y() + 8, color);
			context.fill(rect.x() + 9, rect.y() + 5, rect.right() - 9, rect.y() + 6, color);
			context.fill(rect.x() + 8, rect.y() + 9, rect.right() - 8, rect.bottom() - 4, color);
			context.fill(rect.x() + 10, rect.y() + 11, rect.x() + 11, rect.bottom() - 6, bg);
			context.fill(rect.right() - 11, rect.y() + 11, rect.right() - 10, rect.bottom() - 6, bg);
		}
	}

	private void drawColorPicker(DrawContext context, UiRect rect, float hue, float saturation, float brightness) {
		for (int py = 0; py < rect.height(); py += 4) {
			float value = 1.0F - py / (float) Math.max(1, rect.height() - 1);
			for (int px = 0; px < rect.width(); px += 4) {
				float sat = px / (float) Math.max(1, rect.width() - 1);
				int color = 0xFF000000 | hsbToRgb(hue, sat, value);
				context.fill(rect.x() + px, rect.y() + py,
						Math.min(rect.right(), rect.x() + px + 4), Math.min(rect.bottom(), rect.y() + py + 4), color);
			}
		}
		drawBorder(context, rect, 0xFF070A0C);
		int cursorX = MathHelper.clamp(rect.x() + Math.round(saturation * rect.width()), rect.x(), rect.right() - 1);
		int cursorY = MathHelper.clamp(rect.y() + Math.round((1.0F - brightness) * rect.height()), rect.y(), rect.bottom() - 1);
		context.fill(cursorX - 3, cursorY, cursorX + 4, cursorY + 1, 0xFF000000);
		context.fill(cursorX, cursorY - 3, cursorX + 1, cursorY + 4, 0xFF000000);
		context.fill(cursorX - 2, cursorY, cursorX + 3, cursorY + 1, 0xFFFFFFFF);
		context.fill(cursorX, cursorY - 2, cursorX + 1, cursorY + 3, 0xFFFFFFFF);
	}

	private void drawHueBar(DrawContext context, UiRect rect, float hue) {
		for (int py = 0; py < rect.height(); py += 4) {
			float nextHue = py / (float) Math.max(1, rect.height() - 1);
			int color = 0xFF000000 | hsbToRgb(nextHue, 1.0F, 1.0F);
			context.fill(rect.x(), rect.y() + py, rect.right(), Math.min(rect.bottom(), rect.y() + py + 4), color);
		}
		drawBorder(context, rect, 0xFF070A0C);
		int markerY = MathHelper.clamp(rect.y() + Math.round(hue * rect.height()), rect.y(), rect.bottom() - 1);
		context.fill(rect.x() - 2, markerY - 1, rect.right() + 2, markerY + 2, 0xFFFFFFFF);
		context.fill(rect.x() - 1, markerY, rect.right() + 1, markerY + 1, 0xFF050709);
	}

	private void drawColorPopup(DrawContext context, Layout layout) {
		if (openColorSetting.isBlank()) {
			return;
		}
		SkyBlockSetting setting = findSetting(openColorSetting).orElse(null);
		if (setting == null) {
			openColorSetting = "";
			activeHexSetting = "";
			return;
		}
		int popupW = 226;
		int popupH = 104;
		int x = Math.min(layout.contentX() + layout.contentWidth() - popupW - 18, layout.panelX() + layout.panelWidth() - popupW - 22);
		int anchorY = openColorAnchorY > 0 ? openColorAnchorY : renderMouseY;
		int y = Math.max(layout.panelY() + 56, Math.min(anchorY - 20, layout.contentBottom() - popupH - 8));
		UiRect popup = new UiRect(Math.max(layout.panelX() + 22, x), y, popupW, popupH);
		int current = colorArgb(setting);
		float[] hsv = rgbToHsv(current);
		float hue = colorHues.getOrDefault(setting.id(), hsv[0]);
		colorHues.putIfAbsent(setting.id(), hue);
		context.fill(popup.x(), popup.y(), popup.right(), popup.bottom(), 0xFF111820);
		context.fill(popup.x() + 1, popup.y() + 1, popup.right() - 1, popup.bottom() - 1, 0xFF1A232A);
		drawBorder(context, popup, SkyBlockLensClient.configStore().config().accentArgb());
		UiRect swatch = new UiRect(popup.x() + 12, popup.y() + 14, 36, 36);
		UiRect picker = new UiRect(popup.x() + 62, popup.y() + 12, 120, 72);
		UiRect hueBar = new UiRect(picker.right() + 12, picker.y(), 12, picker.height());
		context.fill(swatch.x(), swatch.y(), swatch.right(), swatch.bottom(), current);
		drawBorder(context, swatch, 0xFF070A0C);
		drawColorPicker(context, picker, hue, hsv[1], hsv[2]);
		drawHueBar(context, hueBar, hue);
		hitboxes.add(Hitbox.setting(picker, HitboxType.COLOR_PICKER, setting));
		hitboxes.add(Hitbox.setting(hueBar.expand(3), HitboxType.COLOR_HUE, setting));
	}

	private void drawScrollbar(DrawContext context, int x, int top, int bottom, int scroll, int maxScroll) {
		context.fill(x, top, x + 4, bottom, 0xFF070A0C);
		if (maxScroll <= 0) {
			return;
		}
		int height = bottom - top;
		int thumbHeight = Math.max(24, height * height / (height + maxScroll));
		int thumbY = top + (height - thumbHeight) * scroll / maxScroll;
		context.fill(x + 1, thumbY, x + 3, thumbY + thumbHeight, 0xFF42E8C8);
	}

	private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	private void drawBorder(DrawContext context, UiRect rect, int color) {
		drawBorder(context, rect.x(), rect.y(), rect.width(), rect.height(), color);
	}

	private void drawCentered(DrawContext context, String text, int centerX, int y, int color) {
		context.drawTextWithShadow(textRenderer, Text.literal(text), centerX - textRenderer.getWidth(text) / 2, y, color);
	}

	private void drawFittedText(DrawContext context, String text, int x, int y, int maxWidth, int color) {
		context.drawTextWithShadow(textRenderer, Text.literal(fit(text, maxWidth)), x, y, color);
	}

	private void drawWrapped(DrawContext context, String text, int x, int y, int maxWidth, int color, int maxLines) {
		int line = 0;
		for (String part : wrap(text, maxWidth)) {
			if (line >= maxLines) {
				break;
			}
			if (line == maxLines - 1 && wrap(text, maxWidth).size() > maxLines) {
				part = fit(part + "...", maxWidth);
			}
			context.drawTextWithShadow(textRenderer, Text.literal(part), x, y + line * 10, color);
			line++;
		}
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

	private List<SkyBlockSetting> filteredSettings(SblI18n i18n, SkyBlockSettingGroup group, String query) {
		if (query.isBlank()) {
			return group.settings().stream()
					.filter(this::dependencyVisible)
					.toList();
		}
		return group.settings().stream()
				.filter(this::dependencyVisible)
				.filter(setting -> matches(query, i18n.tr(setting.titleKey()))
						|| matches(query, i18n.tr(setting.descriptionKey()))
						|| matches(query, i18n.tr(group.titleKey())))
				.toList();
	}

	private Optional<SkyBlockSetting> findSetting(String id) {
		for (SkyBlockSettingPage page : SkyBlockSettingsCatalog.pages()) {
			for (SkyBlockSettingGroup group : page.groups()) {
				for (SkyBlockSetting setting : group.settings()) {
					if (setting.id().equals(id)) {
						return Optional.of(setting);
					}
				}
			}
		}
		return Optional.empty();
	}

	private boolean dependencyVisible(SkyBlockSetting setting) {
		return dependencyVisible(setting.id(), new HashSet<>());
	}

	private boolean dependencyVisible(String id, Set<String> seen) {
		String parent = parentSetting(id);
		if (parent.isBlank()) {
			return true;
		}
		if (!seen.add(parent)) {
			return true;
		}
		return SkyBlockLensClient.configStore().config().featureEnabled(parent) && dependencyVisible(parent, seen);
	}

	private static String parentSetting(String id) {
		return switch (id) {
			case "notifications.edit_filters" -> "notifications.chat_filters";
			case "notifications.edit_highlights" -> "notifications.chat_highlights";
			case "itemlist.recipe_view", "itemlist.usage_view", "itemlist.search_aliases",
					"itemlist.hide_missing_data", "itemlist.browser_overlay",
					"itemlist.toggle_browser_keybind", "itemlist.hide_in_dungeons",
					"itemlist.overlay_while_typing", "itemlist.inventory_search_highlight_color" -> "itemlist.local_browser";
			case "toolbar.search_bar", "toolbar.background_color", "toolbar.background_alpha" -> "toolbar.enable";
			case "toolbar.inventory_search_button", "toolbar.ctrl_f", "toolbar.auto_turnoff_search",
					"toolbar.search_width", "toolbar.search_height" -> "toolbar.search_bar";
			case "inventory_buttons.item_browser", "inventory_buttons.background_color", "inventory_buttons.background_alpha",
					"inventory_buttons.command_background_color", "inventory_buttons.command_background_alpha" -> "inventory_buttons.enable";
			case "misc.nameplates.show_self", "misc.nameplates_background_color", "misc.nameplates_background_alpha",
					"misc.nameplates_text_color", "misc.nameplates_text_alpha",
					"misc.nameplates_own_alias", "misc.nameplates_own_name_color" -> "misc.nameplates.enable";
			case "quick_swap.binding", "quick_swap.keybind", "quick_swap.overlay" -> "quick_swap.enable";
			case "slot_locking.binding", "slot_locking.keybind", "slot_locking.sound", "slot_locking.lock_slots_in_trade",
					"slot_locking.disable_in_storage", "slot_locking.hotbar", "slot_locking.inventory", "slot_locking.warning",
					"slot_locking.overlay", "slot_locking.overlay_color", "slot_locking.reset" -> "slot_locking.enable";
			case "slot_locking.binding_also_locks" -> "slot_locking.binding";
			case "slot_locking.sound_volume" -> "slot_locking.sound";
			case "tooltip_tweaks.internal_id", "tooltip_tweaks.rarity", "tooltip_tweaks.category",
					"tooltip_tweaks.pet_extend_exp", "tooltip_tweaks.missing_recipe_notice" -> "tooltip_tweaks.enhanced_item_tooltips";
			default -> "";
		};
	}

	private boolean isExpanded(SkyBlockSettingGroup group, String query) {
		if (!query.isBlank()) {
			return true;
		}
		if (!canCollapse(group)) {
			return true;
		}
		String key = groupKey(group);
		if (group.expandedByDefault()) {
			return !collapsedGroups.contains(key);
		}
		return expandedGroups.contains(key);
	}

	private void toggleGroup(SkyBlockSettingGroup group) {
		if (!canCollapse(group)) {
			return;
		}
		String key = groupKey(group);
		if (group.expandedByDefault()) {
			if (!collapsedGroups.remove(key)) {
				collapsedGroups.add(key);
			}
			return;
		}
		if (!expandedGroups.remove(key)) {
			expandedGroups.add(key);
		}
	}

	private String groupKey(SkyBlockSettingGroup group) {
		return selectedCategory.id() + "." + group.id();
	}

	private boolean canCollapse(SkyBlockSettingGroup group) {
		return !group.expandedByDefault() && group.settings().size() > 5;
	}

	private boolean shouldDrawGroupHeader(SkyBlockSettingPage page, SkyBlockSettingGroup group) {
		return canCollapse(group) || page.groups().size() > 1;
	}

	private boolean isSettingEnabled(SkyBlockSetting setting) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return config.featureEnabled(setting.id());
	}

	private void toggleSetting(SkyBlockSetting setting) {
		if (setting.control() != SkyBlockSettingControl.TOGGLE) {
			return;
		}
		ConfigStore store = SkyBlockLensClient.configStore();
		SkyBlockLensConfig config = store.config();
		config.setFeatureEnabled(setting.id(), !config.featureEnabled(setting.id()));
		store.save();
	}

	private void performAction(SkyBlockSetting setting) {
		if (client == null) {
			return;
		}
		switch (setting.id()) {
			case "hud.open_editor" -> client.setScreen(new HudEditorScreen(this));
			case "toolbar.open_editor" -> client.setScreen(new ToolbarEditorScreen(this));
			case "notifications.edit_filters" -> client.setScreen(ChatTermsEditorScreen.filters(this));
			case "notifications.edit_highlights" -> client.setScreen(ChatTermsEditorScreen.highlights(this));
			case "itemlist.open_browser" -> client.setScreen(new ItemBrowserScreen(this));
			case "inventory_buttons.open_editor" -> client.setScreen(new InventoryButtonEditorScreen(this));
			case "misc.nameplates_own_alias" -> client.setScreen(new NameplateAliasScreen(this));
			case "slot_locking.reset" -> SlotLockController.resetLockedSlots();
			default -> {
			}
		}
	}

	private void selectDropdownOption(SkyBlockSetting setting, String value) {
		ConfigStore store = SkyBlockLensClient.configStore();
		switch (setting.id()) {
			case "core.language" -> {
				store.config().language = "ru_ru".equals(value) ? "ru_ru" : "en_us";
				SkyBlockLensClient.reloadLanguage();
			}
			case "core.menu_ui_scale" -> store.config().menuUiScale = switch (value) {
				case "small", "large" -> value;
				default -> "medium";
			};
			default -> {
				return;
			}
		}
		store.save();
		openDropdownSetting = "";
		init();
	}

	private String actionLabel(SblI18n i18n, SkyBlockSetting setting) {
		if ("slot_locking.reset".equals(setting.id())) {
			return i18n.tr("skyblocklens.config.reset");
		}
		if ("notifications.edit_filters".equals(setting.id()) || "notifications.edit_highlights".equals(setting.id())) {
			return i18n.tr("skyblocklens.config.edit");
		}
		if ("misc.nameplates_own_alias".equals(setting.id())) {
			return i18n.tr("skyblocklens.config.edit");
		}
		return i18n.tr("skyblocklens.config.open");
	}

	private void setSliderFromMouse(double mouseX, Hitbox hitbox) {
		double progress = MathHelper.clamp((mouseX - hitbox.x()) / hitbox.width(), 0.0D, 1.0D);
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		switch (hitbox.setting().id()) {
			case "gui.hud_background_alpha" -> config.hudBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "gui.scoreboard_background_alpha" -> config.scoreboardBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "notifications.duration" -> config.notificationDurationSeconds = 2 + (int) Math.round(progress * 8.0D);
			case "toolbar.background_alpha" -> config.toolbarBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "inventory_buttons.background_alpha" -> config.inventoryButtonBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "inventory_buttons.command_background_alpha" -> config.inventoryCommandBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "misc.nameplates_background_alpha" -> config.nameplateBackgroundAlpha = (int) Math.round(progress * 255.0D);
			case "misc.nameplates_text_alpha" -> config.nameplateTextAlpha = (int) Math.round(progress * 255.0D);
			case "slot_locking.sound_volume" -> config.slotLockSoundVolume = (int) Math.round(progress * 100.0D);
			default -> {
				return;
			}
		}
		SkyBlockLensClient.configStore().save();
	}

	private void handleKeybindInput(KeyInput keyInput) {
		if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
			activeKeybindSetting = "";
			return;
		}
		if (keyInput.key() == GLFW.GLFW_KEY_BACKSPACE || keyInput.key() == GLFW.GLFW_KEY_DELETE) {
			setKeybind(InputUtil.UNKNOWN_KEY);
			activeKeybindSetting = "";
			return;
		}
		setKeybind(InputUtil.fromKeyCode(keyInput));
		activeKeybindSetting = "";
	}

	private boolean handleHexKey(KeyInput keyInput) {
		if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
			activeHexSetting = "";
			replaceHexDraftOnInput = false;
			return true;
		}
		if (keyInput.key() == GLFW.GLFW_KEY_ENTER || keyInput.key() == GLFW.GLFW_KEY_KP_ENTER) {
			commitHexDraft();
			activeHexSetting = "";
			replaceHexDraftOnInput = false;
			return true;
		}
		if (keyInput.key() == GLFW.GLFW_KEY_BACKSPACE) {
			if (replaceHexDraftOnInput) {
				replaceHexDraftOnInput = false;
				updateHexDraft("");
				return true;
			}
			String value = colorDrafts.getOrDefault(activeHexSetting, colorValue(activeHexSetting));
			if (value.startsWith("#")) {
				value = value.substring(1);
			}
			if (!value.isEmpty()) {
				value = value.substring(0, value.length() - 1);
			}
			updateHexDraft(value);
			return true;
		}
		return false;
	}

	private void handleHexChar(CharInput charInput) {
		if (!charInput.isValidChar()) {
			return;
		}
		String typed = charInput.asString();
		if (typed.length() != 1 || Character.digit(typed.charAt(0), 16) < 0) {
			return;
		}
		String value = replaceHexDraftOnInput ? "" : colorDrafts.getOrDefault(activeHexSetting, colorValue(activeHexSetting));
		replaceHexDraftOnInput = false;
		if (value.startsWith("#")) {
			value = value.substring(1);
		}
		if (value.length() >= 6) {
			return;
		}
		updateHexDraft(value + typed);
	}

	private void updateHexDraft(String rawHex) {
		String value = "#" + rawHex.toUpperCase(Locale.ROOT);
		colorDrafts.put(activeHexSetting, value);
		if (rawHex.length() == 6) {
			SkyBlockLensClient.configStore().config().setColorValue(activeHexSetting, value);
			SkyBlockLensClient.configStore().save();
		}
	}

	private void commitHexDraft() {
		String value = colorDrafts.getOrDefault(activeHexSetting, colorValue(activeHexSetting));
		if (value.startsWith("#")) {
			value = value.substring(1);
		}
		if (value.length() == 6) {
			updateHexDraft(value);
		}
	}

	private void setKeybind(InputUtil.Key key) {
		if ("slot_locking.keybind".equals(activeKeybindSetting) || "quick_swap.keybind".equals(activeKeybindSetting)) {
			SlotLockController.setKey(key);
		} else if ("itemlist.toggle_browser_keybind".equals(activeKeybindSetting)) {
			ToolbarController.setToggleKey(key);
		}
	}

	private KeyBinding keyBinding(SkyBlockSetting setting) {
		if ("slot_locking.keybind".equals(setting.id()) || "quick_swap.keybind".equals(setting.id())) {
			return SkyBlockLensClient.slotLockKey();
		}
		if ("itemlist.toggle_browser_keybind".equals(setting.id())) {
			return SkyBlockLensClient.itemBrowserToggleKey();
		}
		return null;
	}

	private String keybindConflict(KeyBinding current) {
		if (client == null || client.options == null || current == null || current.isUnbound()) {
			return "";
		}
		String key = current.getBoundKeyTranslationKey();
		for (KeyBinding other : client.options.allKeys) {
			if (other == current || other.isUnbound()) {
				continue;
			}
			if (key.equals(other.getBoundKeyTranslationKey())) {
				return Text.translatable(other.getId()).getString();
			}
		}
		return "";
	}

	private String dropdownValue(SkyBlockSetting setting) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return switch (setting.id()) {
			case "core.language" -> config.language;
			case "core.menu_ui_scale" -> config.menuUiScale;
			default -> "";
		};
	}

	private String dropdownLabel(SblI18n i18n, SkyBlockSetting setting, String value) {
		return switch (setting.id()) {
			case "core.language" -> i18n.tr("skyblocklens.config.language." + value);
			case "core.menu_ui_scale" -> i18n.tr("skyblocklens.config.menu_scale." + value);
			default -> value;
		};
	}

	private String[] dropdownOptions(SkyBlockSetting setting) {
		return switch (setting.id()) {
			case "core.language" -> new String[]{"en_us", "ru_ru"};
			case "core.menu_ui_scale" -> new String[]{"small", "medium", "large"};
			default -> new String[0];
		};
	}

	private double sliderProgress(SkyBlockSetting setting, SkyBlockLensConfig config) {
		return switch (setting.id()) {
			case "gui.hud_background_alpha" -> config.hudBackgroundAlpha / 255.0D;
			case "gui.scoreboard_background_alpha" -> config.scoreboardBackgroundAlpha / 255.0D;
			case "notifications.duration" -> (config.notificationDurationSeconds - 2.0D) / 8.0D;
			case "toolbar.background_alpha" -> config.toolbarBackgroundAlpha / 255.0D;
			case "inventory_buttons.background_alpha" -> config.inventoryButtonBackgroundAlpha / 255.0D;
			case "inventory_buttons.command_background_alpha" -> config.inventoryCommandBackgroundAlpha / 255.0D;
			case "misc.nameplates_background_alpha" -> config.nameplateBackgroundAlpha / 255.0D;
			case "misc.nameplates_text_alpha" -> config.nameplateTextAlpha / 255.0D;
			case "slot_locking.sound_volume" -> config.slotLockSoundVolume / 100.0D;
			default -> 0.0D;
		};
	}

	private String sliderLabel(SkyBlockSetting setting, SkyBlockLensConfig config) {
		return switch (setting.id()) {
			case "gui.hud_background_alpha" -> Math.round(config.hudBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "gui.scoreboard_background_alpha" -> Math.round(config.scoreboardBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "notifications.duration" -> config.notificationDurationSeconds + "s";
			case "toolbar.background_alpha" -> Math.round(config.toolbarBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "inventory_buttons.background_alpha" -> Math.round(config.inventoryButtonBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "inventory_buttons.command_background_alpha" -> Math.round(config.inventoryCommandBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "misc.nameplates_background_alpha" -> Math.round(config.nameplateBackgroundAlpha / 255.0D * 100.0D) + "%";
			case "misc.nameplates_text_alpha" -> Math.round(config.nameplateTextAlpha / 255.0D * 100.0D) + "%";
			case "slot_locking.sound_volume" -> config.slotLockSoundVolume + "%";
			default -> "";
		};
	}

	private int colorArgb(SkyBlockSetting setting) {
		return parseHexColor(colorValue(setting), SkyBlockLensClient.configStore().config().accentArgb());
	}

	private String colorValue(SkyBlockSetting setting) {
		return colorValue(setting.id());
	}

	private String colorValue(String settingId) {
		return SkyBlockLensClient.configStore().config().colorValue(settingId);
	}

	private void setColorValue(SkyBlockSetting setting, String value) {
		SkyBlockLensClient.configStore().config().setColorValue(setting.id(), value);
		SkyBlockLensClient.configStore().save();
		colorDrafts.put(setting.id(), colorValue(setting));
		colorHues.put(setting.id(), rgbToHsv(parseHexColor(colorValue(setting), 0xFFFFFFFF))[0]);
	}

	private static int parseHexColor(String value, int fallback) {
		return SkyBlockLensConfig.parseHexColor(value, fallback);
	}

	private void setColorFromPicker(double mouseX, double mouseY, Hitbox hitbox) {
		float hue = colorHues.getOrDefault(hitbox.setting().id(), rgbToHsv(colorArgb(hitbox.setting()))[0]);
		float saturation = (float) MathHelper.clamp((mouseX - hitbox.x()) / hitbox.width(), 0.0D, 1.0D);
		float brightness = 1.0F - (float) MathHelper.clamp((mouseY - hitbox.y()) / hitbox.height(), 0.0D, 1.0D);
		setColorValue(hitbox.setting(), hexColor(0xFF000000 | hsbToRgb(hue, saturation, brightness)));
		colorHues.put(hitbox.setting().id(), hue);
	}

	private void setColorFromHue(double mouseY, Hitbox hitbox) {
		float hue = (float) MathHelper.clamp((mouseY - hitbox.y()) / hitbox.height(), 0.0D, 1.0D);
		float[] hsv = rgbToHsv(colorArgb(hitbox.setting()));
		float saturation = hsv[1] <= 0.01F ? 1.0F : hsv[1];
		float brightness = hsv[2] <= 0.01F ? 1.0F : hsv[2];
		colorHues.put(hitbox.setting().id(), hue);
		setColorValue(hitbox.setting(), hexColor(0xFF000000 | hsbToRgb(hue, saturation, brightness)));
	}

	private static int controlWidth(SkyBlockSetting setting) {
		return switch (setting.control()) {
			case COLOR -> 212;
			case SLIDER -> 176;
			case KEYBIND -> 176;
			case DROPDOWN -> 150;
			case ACTION -> 128;
			case TOGGLE -> 84;
		};
	}

	private static float[] rgbToHsv(int argb) {
		float r = ((argb >>> 16) & 0xFF) / 255.0F;
		float g = ((argb >>> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue;
		if (delta <= 0.0001F) {
			hue = 0.0F;
		} else if (max == r) {
			hue = ((g - b) / delta) % 6.0F;
		} else if (max == g) {
			hue = (b - r) / delta + 2.0F;
		} else {
			hue = (r - g) / delta + 4.0F;
		}
		hue /= 6.0F;
		if (hue < 0.0F) {
			hue += 1.0F;
		}
		float saturation = max <= 0.0001F ? 0.0F : delta / max;
		return new float[]{hue, saturation, max};
	}

	private static int hsbToRgb(float hue, float saturation, float brightness) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) Math.floor(h);
		float fraction = h - sector;
		float p = brightness * (1.0F - saturation);
		float q = brightness * (1.0F - saturation * fraction);
		float t = brightness * (1.0F - saturation * (1.0F - fraction));
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> {
				r = brightness;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = brightness;
				b = p;
			}
			case 2 -> {
				r = p;
				g = brightness;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = brightness;
			}
			case 4 -> {
				r = t;
				g = p;
				b = brightness;
			}
			default -> {
				r = brightness;
				g = p;
				b = q;
			}
		}
		return (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	private static String hexColor(int argb) {
		return String.format(Locale.ROOT, "#%06X", argb & 0x00FFFFFF);
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

	private double openingProgress() {
		double age = System.currentTimeMillis() - openedAtMillis;
		double progress = MathHelper.clamp(age / 180.0D, 0.0D, 1.0D);
		return 1.0D - Math.pow(1.0D - progress, 3.0D);
	}

	private static int blend(int from, int to, double progress) {
		double t = MathHelper.clamp(progress, 0.0D, 1.0D);
		int a = (int) Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = (int) Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
		int g = (int) Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
		int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private int settingRowHeight(SkyBlockSetting setting) {
		if (setting.control() == SkyBlockSettingControl.DROPDOWN && setting.id().equals(openDropdownSetting)) {
			return SETTING_ROW_HEIGHT + dropdownOptions(setting).length * 22 + 4;
		}
		if (setting.control() == SkyBlockSettingControl.KEYBIND) {
			return SETTING_ROW_HEIGHT;
		}
		if (setting.control() == SkyBlockSettingControl.COLOR) {
			return SETTING_ROW_HEIGHT;
		}
		return SETTING_ROW_HEIGHT;
	}

	private UiRect rightControl(Layout layout, int rowY, int width, int height, int offsetY) {
		return new UiRect(layout.contentX() + layout.contentWidth() - width - 14, rowY + offsetY, width, height);
	}

	private void resetConfig() {
		SkyBlockLensClient.configStore().reset();
		searchValue = "";
		contentScroll = 0;
		categoryScroll = 0;
		expandedGroups.clear();
		collapsedGroups.clear();
		init();
	}

	private void clampScroll() {
		categoryScroll = MathHelper.clamp(categoryScroll, 0, Math.max(0, maxCategoryScroll));
		contentScroll = MathHelper.clamp(contentScroll, 0, Math.max(0, maxContentScroll));
	}

	private Layout layout() {
		int uiWidth = uiWidth();
		int uiHeight = uiHeight();
		int panelWidth = Math.min(Math.max(uiWidth - 44, 360), 1030);
		int panelHeight = Math.min(Math.max(uiHeight - 44, 300), 630);
		int panelX = (uiWidth - panelWidth) / 2;
		int panelY = (uiHeight - panelHeight) / 2;
		int categoryWidth = Math.min(220, Math.max(170, panelWidth / 4));
		int categoryX = panelX + PANEL_PADDING + 8;
		int contentX = categoryX + categoryWidth + 28;
		int contentWidth = panelX + panelWidth - contentX - PANEL_PADDING - 18;
		int searchY = panelY + HEADER_HEIGHT + 48;
		int contentTop = searchY + 34;
		int listTop = contentTop;
		int contentBottom = panelY + panelHeight - FOOTER_HEIGHT - 6;
		return new Layout(panelX, panelY, panelWidth, panelHeight, categoryX, categoryWidth, contentX,
				contentWidth, listTop, searchY, contentTop, contentBottom);
	}

	private double uiScale() {
		return SkyBlockLensClient.configStore().config().menuUiScaleFactor();
	}

	private int uiWidth() {
		return Math.max(320, (int) Math.floor(width / uiScale()));
	}

	private int uiHeight() {
		return Math.max(240, (int) Math.floor(height / uiScale()));
	}

	private Click uiClick(Click click) {
		double scale = uiScale();
		return new Click(click.x() / scale, click.y() / scale, click.buttonInfo());
	}

	private void enableUiScissor(DrawContext context, int x1, int y1, int x2, int y2) {
		double scale = uiScale();
		context.enableScissor(
				(int) Math.floor(x1 * scale),
				(int) Math.floor(y1 * scale),
				(int) Math.ceil(x2 * scale),
				(int) Math.ceil(y2 * scale)
		);
	}

	private String modVersion() {
		Optional<String> version = FabricLoader.getInstance().getModContainer(SkyBlockLensClient.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString());
		return version.orElse("dev");
	}

	private static boolean matches(String query, String label) {
		return normalized(label).contains(query);
	}

	private static String normalized(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
	}

	private enum HitboxType {
		CATEGORY,
		GROUP,
		TOGGLE,
		ACTION,
		DROPDOWN,
		DROPDOWN_OPTION,
		SLIDER,
		KEYBIND,
		KEYBIND_CLEAR,
		COLOR_SWATCH,
		COLOR_PICKER,
		COLOR_HUE,
		COLOR_HEX,
		RESET,
		RESET_CONFIRM,
		RESET_CANCEL,
		BACK
	}

	private enum IconButton {
		TRASH
	}

	private record Hitbox(
			int x,
			int y,
			int width,
			int height,
			HitboxType type,
			SkyBlockSettingCategory category,
			SkyBlockSettingGroup group,
			SkyBlockSetting setting,
			String value
	) {
		static Hitbox category(int x, int y, int width, int height, SkyBlockSettingCategory category) {
			return new Hitbox(x, y, width, height, HitboxType.CATEGORY, category, null, null, "");
		}

		static Hitbox group(int x, int y, int width, int height, SkyBlockSettingGroup group) {
			return new Hitbox(x, y, width, height, HitboxType.GROUP, null, group, null, "");
		}

		static Hitbox setting(int x, int y, int width, int height, HitboxType type, SkyBlockSetting setting) {
			return new Hitbox(x, y, width, height, type, null, null, setting, "");
		}

		static Hitbox setting(UiRect bounds, HitboxType type, SkyBlockSetting setting) {
			return setting(bounds.x(), bounds.y(), bounds.width(), bounds.height(), type, setting);
		}

		static Hitbox option(int x, int y, int width, int height, SkyBlockSetting setting, String value) {
			return new Hitbox(x, y, width, height, HitboxType.DROPDOWN_OPTION, null, null, setting, value);
		}

		static Hitbox option(UiRect bounds, SkyBlockSetting setting, String value) {
			return option(bounds.x(), bounds.y(), bounds.width(), bounds.height(), setting, value);
		}

		static Hitbox simple(int x, int y, int width, int height, HitboxType type) {
			return new Hitbox(x, y, width, height, type, null, null, null, "");
		}

		static Hitbox simple(UiRect bounds, HitboxType type) {
			return simple(bounds.x(), bounds.y(), bounds.width(), bounds.height(), type);
		}

		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		}
	}

	private record Layout(
			int panelX,
			int panelY,
			int panelWidth,
			int panelHeight,
			int categoryX,
			int categoryWidth,
			int contentX,
			int contentWidth,
			int listTop,
			int searchY,
			int contentTop,
			int contentBottom
	) {
	}
}
