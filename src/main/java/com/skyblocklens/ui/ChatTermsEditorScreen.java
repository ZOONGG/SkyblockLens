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
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatTermsEditorScreen extends Screen {
	private static final int PANEL_MARGIN = 18;
	private static final int ROW_HEIGHT = 24;
	private static final int MAX_TERMS = 64;
	private static final int MAX_TERM_LENGTH = 80;

	private final Screen parent;
	private final TermList termList;
	private final List<RemoveHitbox> removeHitboxes = new ArrayList<>();
	private TextFieldWidget input;
	private int scroll;
	private int maxScroll;
	private UiRect addButton;
	private UiRect clearButton;
	private UiRect backButton;

	private ChatTermsEditorScreen(Screen parent, TermList termList) {
		super(Text.literal(SkyBlockLensClient.i18n().tr(termList.titleKey)));
		this.parent = parent;
		this.termList = termList;
	}

	public static ChatTermsEditorScreen filters(Screen parent) {
		return new ChatTermsEditorScreen(parent, TermList.FILTERS);
	}

	public static ChatTermsEditorScreen highlights(Screen parent) {
		return new ChatTermsEditorScreen(parent, TermList.HIGHLIGHTS);
	}

	@Override
	protected void init() {
		UiRect inputBounds = inputBounds();
		input = new TextFieldWidget(
				textRenderer,
				inputBounds.x(),
				inputBounds.y(),
				inputBounds.width(),
				inputBounds.height(),
				Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.input"))
		);
		input.setPlaceholder(Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.input")));
		input.setMaxLength(MAX_TERM_LENGTH);
		addDrawableChild(input);
		addButton = new UiRect(inputBounds.right() + 8, inputBounds.y(), 62, inputBounds.height());
		clearButton = new UiRect(inputBounds.right() + 76, inputBounds.y(), 62, inputBounds.height());
		backButton = new UiRect(width - 92, height - 28, 80, 20);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		removeHitboxes.clear();
		context.fill(0, 0, width, height, 0xDD070A10);
		drawShell(context);
		renderTerms(context, mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
		drawSmallButton(context, addButton, SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.add"), mouseX, mouseY);
		drawSmallButton(context, clearButton, SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.clear"), mouseX, mouseY);
		drawSmallButton(context, backButton, SkyBlockLensClient.i18n().tr("skyblocklens.config.back"), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			if (addButton != null && addButton.contains(click.x(), click.y())) {
				addTerm();
				return true;
			}
			if (clearButton != null && clearButton.contains(click.x(), click.y())) {
				clearTerms();
				return true;
			}
			if (backButton != null && backButton.contains(click.x(), click.y())) {
				close();
				return true;
			}
			for (RemoveHitbox hitbox : removeHitboxes) {
				if (hitbox.bounds().contains(click.x(), click.y())) {
					removeTerm(hitbox.index());
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (listBounds().contains(mouseX, mouseY)) {
			scroll = MathHelper.clamp(scroll - (int) Math.round(verticalAmount * ROW_HEIGHT), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (input != null && input.isFocused()
				&& (keyInput.key() == GLFW.GLFW_KEY_ENTER || keyInput.key() == GLFW.GLFW_KEY_KP_ENTER)) {
			addTerm();
			return true;
		}
		if (input != null && input.isFocused() && input.keyPressed(keyInput)) {
			return true;
		}
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (input != null && input.isFocused() && input.charTyped(charInput)) {
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
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr(termList.titleKey)),
				panel.x() + 14, panel.y() + 12, 0xFFF3F2E8);
		context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr(termList.hintKey)),
				panel.x() + 14, panel.y() + 48, 0xFFAAB7C4);
	}

	private void renderTerms(DrawContext context, int mouseX, int mouseY) {
		List<String> terms = terms();
		UiRect list = listBounds();
		int totalHeight = terms.size() * ROW_HEIGHT;
		maxScroll = Math.max(0, totalHeight - list.height());
		scroll = MathHelper.clamp(scroll, 0, maxScroll);
		if (terms.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.empty")),
					list.x() + 8, list.y() + 8, 0xFF8494A1);
			return;
		}
		context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
		for (int index = 0; index < terms.size(); index++) {
			String term = terms.get(index);
			int y = list.y() - scroll + index * ROW_HEIGHT;
			if (y + ROW_HEIGHT < list.y() || y > list.bottom()) {
				continue;
			}
			boolean hovered = mouseX >= list.x() && mouseX <= list.right() && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
			context.fill(list.x(), y, list.right() - 5, y + ROW_HEIGHT - 2, hovered ? 0xFF1F2A31 : 0xFF101720);
			context.drawTextWithShadow(textRenderer, Text.literal(fit(term, list.width() - 92)),
					list.x() + 8, y + 8, 0xFFE6F4FF);
			UiRect remove = new UiRect(list.right() - 76, y + 3, 64, 17);
			drawSmallButton(context, remove, SkyBlockLensClient.i18n().tr("skyblocklens.chat_terms.remove"), mouseX, mouseY);
			removeHitboxes.add(new RemoveHitbox(index, remove));
		}
		context.disableScissor();
		drawScrollbar(context, list.right() - 4, list.y(), list.bottom(), scroll, maxScroll);
	}

	private void addTerm() {
		if (input == null) {
			return;
		}
		String term = normalizeTerm(input.getText());
		if (term.isBlank()) {
			return;
		}
		List<String> terms = terms();
		if (terms.size() >= MAX_TERMS || containsIgnoreCase(terms, term)) {
			input.setText("");
			return;
		}
		terms.add(term);
		input.setText("");
		SkyBlockLensClient.configStore().save();
	}

	private void removeTerm(int index) {
		List<String> terms = terms();
		if (index < 0 || index >= terms.size()) {
			return;
		}
		terms.remove(index);
		SkyBlockLensClient.configStore().save();
	}

	private void clearTerms() {
		terms().clear();
		scroll = 0;
		SkyBlockLensClient.configStore().save();
	}

	private List<String> terms() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return switch (termList) {
			case FILTERS -> config.blockedChatTerms;
			case HIGHLIGHTS -> config.highlightChatTerms;
		};
	}

	private static String normalizeTerm(String raw) {
		String normalized = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
		if (normalized.length() > MAX_TERM_LENGTH) {
			normalized = normalized.substring(0, MAX_TERM_LENGTH);
		}
		return normalized;
	}

	private static boolean containsIgnoreCase(List<String> terms, String term) {
		String normalized = term.toLowerCase(Locale.ROOT);
		for (String existing : terms) {
			if (existing != null && existing.toLowerCase(Locale.ROOT).equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	private UiRect panelBounds() {
		return new UiRect(PANEL_MARGIN, PANEL_MARGIN, width - PANEL_MARGIN * 2, height - 56);
	}

	private UiRect inputBounds() {
		UiRect panel = panelBounds();
		return new UiRect(panel.x() + 14, panel.y() + 66, Math.max(120, panel.width() - 176), 20);
	}

	private UiRect listBounds() {
		UiRect panel = panelBounds();
		return new UiRect(panel.x() + 14, panel.y() + 100, panel.width() - 28, panel.height() - 114);
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
		String fitted = fit(label, rect.width() - 8);
		context.drawTextWithShadow(textRenderer, Text.literal(fitted),
				rect.centerX() - textRenderer.getWidth(fitted) / 2, rect.y() + 5, 0xFFF3F2E8);
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

	private enum TermList {
		FILTERS("skyblocklens.chat_terms.filters.title", "skyblocklens.chat_terms.filters.hint"),
		HIGHLIGHTS("skyblocklens.chat_terms.highlights.title", "skyblocklens.chat_terms.highlights.hint");

		private final String titleKey;
		private final String hintKey;

		TermList(String titleKey, String hintKey) {
			this.titleKey = titleKey;
			this.hintKey = hintKey;
		}
	}

	private record RemoveHitbox(int index, UiRect bounds) {
	}
}
