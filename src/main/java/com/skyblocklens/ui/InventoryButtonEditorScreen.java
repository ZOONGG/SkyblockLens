package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InventoryButtonEditorScreen extends Screen {
	private static final int MOCK_WIDTH = 176;
	private static final int MOCK_HEIGHT = 166;
	private static final String[] ICON_OPTIONS = {
			"minecraft:emerald", "minecraft:compass", "minecraft:chest", "minecraft:ender_chest",
			"minecraft:crafting_table", "minecraft:book", "minecraft:paper", "minecraft:gold_ingot",
			"minecraft:diamond_sword", "minecraft:bow", "minecraft:clock", "minecraft:nether_star"
	};
	private static final String[] COLOR_OPTIONS = {
			"#42E8C8", "#E7A743", "#8FD7FF", "#65E6A2", "#E06A4D", "#D46BFF", "#FFFFFF", "#333A35"
	};

	private final Screen parent;
	private final List<ButtonHitbox> buttonHitboxes = new ArrayList<>();
	private final List<ValueHitbox> valueHitboxes = new ArrayList<>();
	private final Map<String, Double> hoverAnimations = new HashMap<>();

	private TextFieldWidget labelField;
	private TextFieldWidget commandField;
	private TextFieldWidget iconField;
	private TextFieldWidget textColorField;
	private TextFieldWidget backgroundField;
	private UiRect addButton = new UiRect(0, 0, 0, 0);
	private UiRect removeButton = new UiRect(0, 0, 0, 0);
	private UiRect resetButton = new UiRect(0, 0, 0, 0);
	private UiRect backButton = new UiRect(0, 0, 0, 0);
	private int selectedCommand = -1;
	private boolean selectedItemButton = true;
	private DragMode dragMode = DragMode.NONE;
	private String dragTarget = "";
	private int dragOffsetX;
	private int dragOffsetY;
	private int resizeStartMouseX;
	private int resizeStartMouseY;
	private int resizeStartWidth;
	private int resizeStartHeight;

	public InventoryButtonEditorScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		UiRect side = sidePanelBounds();
		int fieldX = side.x() + 12;
		int fieldWidth = side.width() - 24;
		labelField = field(fieldX, side.y() + 64, fieldWidth);
		commandField = field(fieldX, side.y() + 102, fieldWidth);
		iconField = field(fieldX, side.y() + 140, fieldWidth);
		textColorField = field(fieldX, side.y() + 250, fieldWidth);
		backgroundField = field(fieldX, side.y() + 292, fieldWidth);
		labelField.setChangedListener(value -> {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
			if (button != null) {
				button.label = value;
			}
		});
		commandField.setChangedListener(value -> {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
			if (button != null) {
				button.command = value;
			}
		});
		iconField.setChangedListener(value -> {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
			if (button != null) {
				button.icon = value;
			}
		});
		textColorField.setChangedListener(value -> {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
			if (button != null) {
				button.textColor = normalizeDraftColor(value, button.textColor);
			}
		});
		backgroundField.setChangedListener(value -> {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
			if (button != null) {
				button.backgroundColor = normalizeDraftColor(value, button.backgroundColor);
			}
		});
		addDrawableChild(labelField);
		addDrawableChild(commandField);
		addDrawableChild(iconField);
		addDrawableChild(textColorField);
		addDrawableChild(backgroundField);
		backButton = new UiRect(width - 92, height - 28, 80, 20);
		syncFields();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xDD070A10);
		drawShell(context);
		drawMockInventory(context);
		drawEditableButtons(context, mouseX, mouseY);
		drawSidePanel(context, mouseX, mouseY);
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (addButton.contains(click.x(), click.y()) && click.button() == 0) {
			addCommandButton();
			return true;
		}
		if (removeButton.contains(click.x(), click.y()) && click.button() == 0) {
			removeSelectedCommand();
			return true;
		}
		if (resetButton.contains(click.x(), click.y()) && click.button() == 0) {
			resetItemButton();
			return true;
		}
		if (backButton.contains(click.x(), click.y()) && click.button() == 0) {
			close();
			return true;
		}
		if (click.button() == 0) {
			for (ValueHitbox hitbox : valueHitboxes) {
				if (hitbox.bounds().contains(click.x(), click.y())) {
					applyValueHitbox(hitbox);
					return true;
				}
			}
			for (int i = buttonHitboxes.size() - 1; i >= 0; i--) {
				ButtonHitbox hitbox = buttonHitboxes.get(i);
				if (hitbox.resizeHandle().contains(click.x(), click.y())) {
					selectHitbox(hitbox);
					dragMode = DragMode.RESIZE;
					dragTarget = hitbox.id();
					resizeStartMouseX = (int) click.x();
					resizeStartMouseY = (int) click.y();
					resizeStartWidth = hitbox.bounds().width();
					resizeStartHeight = hitbox.bounds().height();
					return true;
				}
				if (hitbox.bounds().contains(click.x(), click.y())) {
					selectHitbox(hitbox);
					dragMode = DragMode.MOVE;
					dragTarget = hitbox.id();
					dragOffsetX = (int) click.x() - hitbox.bounds().x();
					dragOffsetY = (int) click.y() - hitbox.bounds().y();
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (dragMode == DragMode.NONE || dragTarget.isBlank()) {
			return super.mouseDragged(click, offsetX, offsetY);
		}
		UiRect mock = mockBounds();
		if ("item".equals(dragTarget)) {
			SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
			if (dragMode == DragMode.MOVE) {
				config.inventoryItemButtonX = clamp((int) click.x() - mock.x() - dragOffsetX, -220, 280);
				config.inventoryItemButtonY = clamp((int) click.y() - mock.y() - dragOffsetY, -120, 220);
			} else {
				config.inventoryItemButtonWidth = clamp(resizeStartWidth + (int) click.x() - resizeStartMouseX, 34, 160);
				config.inventoryItemButtonHeight = clamp(resizeStartHeight + (int) click.y() - resizeStartMouseY, 16, 42);
			}
			return true;
		}
		SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
		if (button == null) {
			return true;
		}
		if (dragMode == DragMode.MOVE) {
			button.x = clamp((int) click.x() - mock.x() - dragOffsetX, -220, 280);
			button.y = clamp((int) click.y() - mock.y() - dragOffsetY, -120, 220);
		} else {
			button.width = clamp(resizeStartWidth + (int) click.x() - resizeStartMouseX, 34, 160);
			button.height = clamp(resizeStartHeight + (int) click.y() - resizeStartMouseY, 16, 42);
		}
		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragMode != DragMode.NONE) {
			dragMode = DragMode.NONE;
			dragTarget = "";
			SkyBlockLensClient.configStore().save();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (labelField.keyPressed(keyInput) || commandField.keyPressed(keyInput) || iconField.keyPressed(keyInput)
				|| textColorField.keyPressed(keyInput) || backgroundField.keyPressed(keyInput)) {
			SkyBlockLensClient.configStore().save();
			return true;
		}
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (labelField.charTyped(charInput) || commandField.charTyped(charInput) || iconField.charTyped(charInput)
				|| textColorField.charTyped(charInput) || backgroundField.charTyped(charInput)) {
			SkyBlockLensClient.configStore().save();
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

	private TextFieldWidget field(int x, int y, int width) {
		return new TextFieldWidget(textRenderer, x, y, width, 20, Text.empty());
	}

	private void drawShell(DrawContext context) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(12, 12, width - 12, height - 38, 0xFF111821);
		drawBorder(context, new UiRect(12, 12, width - 24, height - 50), 0xFF2D3A3A);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.title")),
				24, 24, accent);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.hint")),
				24, 38, 0xFFAAB7C4);
	}

	private void drawMockInventory(DrawContext context) {
		UiRect mock = mockBounds();
		context.fill(mock.x() - 6, mock.y() - 6, mock.right() + 6, mock.bottom() + 6, 0xFF0B1016);
		context.fill(mock.x(), mock.y(), mock.right(), mock.bottom(), 0xFFE0E0E0);
		drawBorder(context, mock, 0xFF050709);
		for (int row = 0; row < 5; row++) {
			for (int column = 0; column < 9; column++) {
				int x = mock.x() + 7 + column * 18;
				int y = mock.y() + 71 + row * 18;
				drawSlot(context, x, y);
			}
		}
		context.fill(mock.x() + 62, mock.y() + 16, mock.x() + 114, mock.y() + 68, 0xFF070A0C);
		drawBorder(context, new UiRect(mock.x() + 62, mock.y() + 16, 52, 52), 0xFF3E3E3E);
	}

	private void drawEditableButtons(DrawContext context, int mouseX, int mouseY) {
		buttonHitboxes.clear();
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		UiRect mock = mockBounds();
		if (config.featureEnabled("inventory_buttons.item_browser")) {
			UiRect item = new UiRect(mock.x() + config.inventoryItemButtonX, mock.y() + config.inventoryItemButtonY,
					config.inventoryItemButtonWidth, config.inventoryItemButtonHeight);
			drawEditorButton(context, item, SkyBlockLensClient.i18n().tr("skyblocklens.inventory_buttons.items"),
					"minecraft:emerald", config.inventoryButtonBackgroundArgb(), 0xFFF3F2E8, "item", selectedItemButton,
					mouseX, mouseY);
			buttonHitboxes.add(hitbox("item", -1, item));
		}
		for (int index = 0; index < config.inventoryCommandButtons.size(); index++) {
			SkyBlockLensConfig.InventoryCommandButtonConfig button = config.inventoryCommandButtons.get(index);
			UiRect bounds = new UiRect(mock.x() + button.x, mock.y() + button.y, button.width, button.height);
			int background = SkyBlockLensConfig.colorWithAlpha(button.backgroundColor, button.backgroundAlpha,
					config.inventoryCommandBackgroundArgb());
			int textColor = SkyBlockLensConfig.parseHexColor(button.textColor, 0xFFFFFFFF);
			drawEditorButton(context, bounds, button.label, button.icon, background, textColor, button.id,
					!selectedItemButton && selectedCommand == index, mouseX, mouseY);
			buttonHitboxes.add(hitbox(button.id, index, bounds));
		}
	}

	private void drawEditorButton(
			DrawContext context,
			UiRect rect,
			String label,
			String icon,
			int background,
			int textColor,
			String id,
			boolean selected,
			int mouseX,
			int mouseY
	) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		double hover = hoverAnimation(id, rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(background, 0xFF3A4A54, hover));
		context.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, accent);
		drawBorder(context, rect, selected ? accent : 0xFF050709);
		ItemStack iconStack = InventoryButtonController.iconStack(icon);
		int textX = rect.x() + 4;
		int textWidth = rect.width() - 8;
		if (!iconStack.isEmpty() && rect.width() >= 32 && rect.height() >= 16) {
			context.drawItem(iconStack, rect.x() + 3, rect.y() + Math.max(1, (rect.height() - 16) / 2));
			textX += 18;
			textWidth -= 18;
		} else if (icon != null && !icon.isBlank()) {
			label = icon + " " + label;
		}
		String fitted = fit(label, textWidth);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				textX + Math.max(0, (textWidth - textRenderer.getWidth(fitted)) / 2),
				rect.y() + Math.max(4, (rect.height() - 8) / 2), textColor);
		drawResizeHandle(context, rect);
	}

	private void drawSidePanel(DrawContext context, int mouseX, int mouseY) {
		valueHitboxes.clear();
		UiRect side = sidePanelBounds();
		context.fill(side.x(), side.y(), side.right(), side.bottom(), 0xFF0A1018);
		drawBorder(context, side, 0xFF2F3E46);
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		String selected = selectedItemButton
				? SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.builtin_item")
				: SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.command_button");
		context.drawTextWithShadow(textRenderer, Text.literal(fit(selected, side.width() - 24)), side.x() + 12, side.y() + 12, accent);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.label")),
				side.x() + 12, labelField.getY() - 11, 0xFFE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.command")),
				side.x() + 12, commandField.getY() - 11, 0xFFE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.icon")),
				side.x() + 12, iconField.getY() - 11, 0xFFE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.text_color")),
				side.x() + 12, textColorField.getY() - 11, 0xFFE6F4FF);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.background")),
				side.x() + 12, backgroundField.getY() - 11, 0xFFE6F4FF);
		drawIconSelector(context, side.x() + 12, iconField.getY() + 24);
		drawColorSwatches(context, side.x() + 12, textColorField.getY() + 24, "text");
		drawColorSwatches(context, side.x() + 12, backgroundField.getY() + 24, "background");

		addButton = new UiRect(side.x() + 12, side.bottom() - 74, 76, 20);
		removeButton = new UiRect(addButton.right() + 8, addButton.y(), 76, 20);
		resetButton = new UiRect(side.x() + 12, side.bottom() - 46, 160, 20);
		drawSmallButton(context, addButton, SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.add"), mouseX, mouseY);
		drawSmallButton(context, removeButton, SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.remove"), mouseX, mouseY);
		drawSmallButton(context, resetButton, SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.reset_item"), mouseX, mouseY);
	}

	private void addCommandButton() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int index = config.inventoryCommandButtons.size() + 1;
		SkyBlockLensConfig.InventoryCommandButtonConfig button =
				new SkyBlockLensConfig.InventoryCommandButtonConfig("command_" + index, 178, 30 + index * 22);
		button.label = SkyBlockLensClient.i18n().tr("skyblocklens.inventory_editor.new_command");
		button.command = "/";
		button.icon = "minecraft:emerald";
		config.inventoryCommandButtons.add(button);
		selectedItemButton = false;
		selectedCommand = config.inventoryCommandButtons.size() - 1;
		syncFields();
		SkyBlockLensClient.configStore().save();
	}

	private void removeSelectedCommand() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (selectedItemButton || selectedCommand < 0 || selectedCommand >= config.inventoryCommandButtons.size()) {
			return;
		}
		config.inventoryCommandButtons.remove(selectedCommand);
		selectedCommand = Math.min(selectedCommand, config.inventoryCommandButtons.size() - 1);
		selectedItemButton = selectedCommand < 0;
		syncFields();
		SkyBlockLensClient.configStore().save();
	}

	private void resetItemButton() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		config.inventoryItemButtonX = 178;
		config.inventoryItemButtonY = 6;
		config.inventoryItemButtonWidth = 74;
		config.inventoryItemButtonHeight = 18;
		selectedItemButton = true;
		selectedCommand = -1;
		syncFields();
		SkyBlockLensClient.configStore().save();
	}

	private void selectHitbox(ButtonHitbox hitbox) {
		selectedItemButton = hitbox.index() < 0;
		selectedCommand = hitbox.index();
		syncFields();
	}

	private void syncFields() {
		if (labelField == null) {
			return;
		}
		SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
		if (button == null) {
			labelField.setText(SkyBlockLensClient.i18n().tr("skyblocklens.inventory_buttons.items"));
			commandField.setText("");
			iconField.setText("");
			textColorField.setText("#FFFFFF");
			backgroundField.setText(SkyBlockLensClient.configStore().config().inventoryButtonBackgroundColor);
			return;
		}
		labelField.setText(button.label);
		commandField.setText(button.command);
		iconField.setText(button.icon);
		textColorField.setText(button.textColor);
		backgroundField.setText(button.backgroundColor);
	}

	private SkyBlockLensConfig.InventoryCommandButtonConfig selectedCommand() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (selectedItemButton || selectedCommand < 0 || selectedCommand >= config.inventoryCommandButtons.size()) {
			return null;
		}
		return config.inventoryCommandButtons.get(selectedCommand);
	}

	private void drawIconSelector(DrawContext context, int x, int y) {
		SkyBlockLensConfig.InventoryCommandButtonConfig selected = selectedCommand();
		for (int i = 0; i < ICON_OPTIONS.length; i++) {
			String icon = ICON_OPTIONS[i];
			int column = i % 6;
			int row = i / 6;
			UiRect rect = new UiRect(x + column * 24, y + row * 24, 20, 20);
			context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF151C23);
			drawBorder(context, rect, selected != null && icon.equals(selected.icon)
					? SkyBlockLensClient.configStore().config().accentArgb()
					: 0xFF303A40);
			ItemStack stack = InventoryButtonController.iconStack(icon);
			if (!stack.isEmpty()) {
				context.drawItem(stack, rect.x() + 2, rect.y() + 2);
			}
			valueHitboxes.add(new ValueHitbox(rect, "icon", icon));
		}
	}

	private void drawColorSwatches(DrawContext context, int x, int y, String target) {
		SkyBlockLensConfig.InventoryCommandButtonConfig selected = selectedCommand();
		String selectedColor = selected == null ? "" : "text".equals(target) ? selected.textColor : selected.backgroundColor;
		for (int i = 0; i < COLOR_OPTIONS.length; i++) {
			String color = COLOR_OPTIONS[i];
			UiRect rect = new UiRect(x + i * 20, y, 16, 16);
			context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), SkyBlockLensConfig.parseHexColor(color, 0xFFFFFFFF));
			drawBorder(context, rect, color.equalsIgnoreCase(selectedColor)
					? SkyBlockLensClient.configStore().config().accentArgb()
					: 0xFF050709);
			valueHitboxes.add(new ValueHitbox(rect, target, color));
		}
	}

	private void applyValueHitbox(ValueHitbox hitbox) {
		SkyBlockLensConfig.InventoryCommandButtonConfig button = selectedCommand();
		if (button == null) {
			return;
		}
		switch (hitbox.target()) {
			case "icon" -> {
				button.icon = hitbox.value();
				iconField.setText(button.icon);
			}
			case "text" -> {
				button.textColor = hitbox.value();
				textColorField.setText(button.textColor);
			}
			case "background" -> {
				button.backgroundColor = hitbox.value();
				backgroundField.setText(button.backgroundColor);
			}
			default -> {
				return;
			}
		}
		SkyBlockLensClient.configStore().save();
	}

	private ButtonHitbox hitbox(String id, int index, UiRect bounds) {
		return new ButtonHitbox(id, index, bounds, new UiRect(bounds.right() - 8, bounds.bottom() - 8, 10, 10));
	}

	private UiRect mockBounds() {
		UiRect side = sidePanelBounds();
		int availableRight = side.x() - 18;
		int x = Math.max(28, (availableRight - MOCK_WIDTH) / 2);
		int y = Math.max(74, (height - MOCK_HEIGHT) / 2);
		return new UiRect(x, y, MOCK_WIDTH, MOCK_HEIGHT);
	}

	private UiRect sidePanelBounds() {
		int sideWidth = Math.min(270, Math.max(226, width / 3));
		return new UiRect(width - sideWidth - 18, 54, sideWidth, height - 104);
	}

	private void drawSlot(DrawContext context, int x, int y) {
		context.fill(x, y, x + 16, y + 16, 0xFF9C9C9C);
		context.fill(x + 1, y + 1, x + 15, y + 15, 0xFFB7B7B7);
		drawBorder(context, new UiRect(x, y, 16, 16), 0xFF4B4B4B);
	}

	private void drawResizeHandle(DrawContext context, UiRect rect) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(rect.right() - 8, rect.bottom() - 8, rect.right(), rect.bottom(), 0xAA050709);
		context.fill(rect.right() - 6, rect.bottom() - 3, rect.right() - 1, rect.bottom() - 1, accent);
		context.fill(rect.right() - 3, rect.bottom() - 6, rect.right() - 1, rect.bottom() - 4, accent);
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY) {
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		double hover = hoverAnimation("small:" + label + ":" + rect.x() + ":" + rect.y(), rect.contains(mouseX, mouseY));
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), blend(0xFF26323A, 0xFF35474F, hover));
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + rect.height() / 2, blend(0xFF35464E, 0xFF4C6570, hover));
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

	private static String normalizeDraftColor(String value, String fallback) {
		if (value == null) {
			return fallback;
		}
		String trimmed = value.trim();
		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.length() != 6) {
			return value;
		}
		for (int i = 0; i < trimmed.length(); i++) {
			if (Character.digit(trimmed.charAt(i), 16) < 0) {
				return value;
			}
		}
		return "#" + trimmed.toUpperCase(java.util.Locale.ROOT);
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
		RESIZE
	}

	private record ButtonHitbox(String id, int index, UiRect bounds, UiRect resizeHandle) {
	}

	private record ValueHitbox(UiRect bounds, String target, String value) {
	}
}
