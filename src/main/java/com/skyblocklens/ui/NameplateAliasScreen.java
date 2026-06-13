package com.skyblocklens.ui;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class NameplateAliasScreen extends Screen {
	private static final int MAX_ALIAS_LENGTH = 32;

	private final Screen parent;
	private TextFieldWidget aliasField;
	private UiRect backButton = new UiRect(0, 0, 0, 0);
	private UiRect clearButton = new UiRect(0, 0, 0, 0);

	public NameplateAliasScreen(Screen parent) {
		super(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.title")));
		this.parent = parent;
	}

	@Override
	protected void init() {
		UiRect fieldBounds = fieldBounds();
		aliasField = new TextFieldWidget(
				textRenderer,
				fieldBounds.x(),
				fieldBounds.y(),
				fieldBounds.width(),
				fieldBounds.height(),
				Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.placeholder"))
		);
		aliasField.setPlaceholder(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.placeholder")));
		aliasField.setMaxLength(MAX_ALIAS_LENGTH);
		aliasField.setText(SkyBlockLensClient.configStore().config().nameplateOwnAlias);
		aliasField.setChangedListener(value -> {
			SkyBlockLensClient.configStore().config().nameplateOwnAlias =
					SkyBlockLensConfig.sanitizeText(value, "", MAX_ALIAS_LENGTH);
			SkyBlockLensClient.configStore().save();
		});
		addDrawableChild(aliasField);
		setFocused(aliasField);
		aliasField.setFocused(true);
		UiRect panel = panelBounds();
		clearButton = new UiRect(panel.x() + 14, fieldBounds.bottom() + 16, 96, 22);
		backButton = new UiRect(panel.right() - 110, fieldBounds.bottom() + 16, 96, 22);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xDD070A10);
		drawShell(context);
		super.render(context, mouseX, mouseY, delta);
		drawSmallButton(context, clearButton, SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.clear"), mouseX, mouseY);
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0 && clearButton.contains(click.x(), click.y())) {
			aliasField.setText("");
			SkyBlockLensClient.configStore().config().nameplateOwnAlias = "";
			SkyBlockLensClient.configStore().save();
			return true;
		}
		if (click.button() == 0 && backButton.contains(click.x(), click.y())) {
			close();
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE || keyInput.key() == GLFW.GLFW_KEY_ENTER
				|| keyInput.key() == GLFW.GLFW_KEY_KP_ENTER) {
			close();
			return true;
		}
		if (aliasField != null && aliasField.isFocused() && aliasField.keyPressed(keyInput)) {
			return true;
		}
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (aliasField != null && aliasField.isFocused() && aliasField.charTyped(charInput)) {
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
		UiRect panel = panelBounds();
		int accent = SkyBlockLensClient.configStore().config().accentArgb();
		context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xFF111821);
		context.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + 36, 0xFF1D2830);
		drawBorder(context, panel, 0xFF2F3E46);
		context.fill(panel.x() + 1, panel.y() + 36, panel.right() - 1, panel.y() + 38, accent);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.title")),
				panel.x() + 14, panel.y() + 12, 0xFFF3F2E8);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.nameplates.alias.desc")),
				panel.x() + 14, panel.y() + 50, 0xFFAAB7C4);
	}

	private UiRect panelBounds() {
		int panelW = Math.min(520, Math.max(260, width - 48));
		int panelH = 150;
		return new UiRect((width - panelW) / 2, Math.max(24, (height - panelH) / 2), panelW, panelH);
	}

	private UiRect fieldBounds() {
		UiRect panel = panelBounds();
		return new UiRect(panel.x() + 14, panel.y() + 74, panel.width() - 28, 22);
	}

	private void drawSmallButton(DrawContext context, UiRect rect, String label, int mouseX, int mouseY) {
		boolean hovered = rect.contains(mouseX, mouseY);
		context.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hovered ? 0xFF31424C : 0xFF26323A);
		context.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + rect.height() / 2,
				hovered ? 0xFF3E5660 : 0xFF35464E);
		drawBorder(context, rect, hovered ? SkyBlockLensClient.configStore().config().accentArgb() : 0xFF070A0C);
		String fitted = fit(label, rect.width() - 8);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				rect.centerX() - textRenderer.getWidth(fitted) / 2, rect.y() + 7, 0xFFF3F2E8);
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
}
