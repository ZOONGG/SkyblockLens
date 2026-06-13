package com.skyblocklens.slotlocking;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.SkyBlockLensConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class SlotLockController {
	private static final String DEFAULT_KEY_TRANSLATION = "key.keyboard.l";
	private static final int HOTBAR_FIRST_SLOT = 0;
	private static final int HOTBAR_LAST_SLOT = 8;
	private static final int MAIN_INVENTORY_FIRST_SLOT = 9;
	private static final int MAIN_INVENTORY_LAST_SLOT = 35;
	private static final int BINDING_COLOR = 0xCC42E8C8;
	private static final int BINDING_MUTED_COLOR = 0x5542E8C8;

	private static KeyBinding slotLockKey;
	private static int bindingSourceIndex = -1;
	private static boolean slotLockKeyDown;

	private SlotLockController() {
	}

	public static KeyBinding registerKeybind() {
		slotLockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.skyblocklens.slot_lock",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_L,
				SkyBlockLensClient.keyCategory()
		));
		applyConfiguredKey();
		return slotLockKey;
	}

	public static KeyBinding keyBinding() {
		return slotLockKey;
	}

	public static boolean handleKeyPress(KeyInput input, Slot focusedSlot, ScreenHandler handler, String screenTitle) {
		if (slotLockKey == null || !slotLockKey.matchesKey(input) || focusedSlot == null || handler == null || !isEnabled()) {
			return false;
		}
		if (slotLockKeyDown) {
			return true;
		}
		slotLockKeyDown = true;
		if (isStorageLockingDisabled(screenTitle)) {
			sendStatus("skyblocklens.slot_locking.disabled_in_storage", Formatting.YELLOW);
			return true;
		}
		if (!canLockSlot(focusedSlot)) {
			sendStatus("skyblocklens.slot_locking.not_lockable", Formatting.RED);
			return true;
		}
		bindingSourceIndex = bindingEnabled() && canBindFrom(focusedSlot) ? focusedSlot.getIndex() : -1;

		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		String key = slotKey(handler, focusedSlot);
		boolean locked;
		if (config.lockedSlots.contains(key)) {
			config.lockedSlots.removeIf(key::equals);
			locked = false;
			sendStatus("skyblocklens.slot_locking.unlocked", Formatting.YELLOW);
		} else {
			removeBindingForIndex(focusedSlot.getIndex());
			config.lockedSlots.add(key);
			locked = true;
			sendStatus("skyblocklens.slot_locking.locked", Formatting.AQUA);
		}
		playLockSound(locked);
		SkyBlockLensClient.configStore().save();
		return true;
	}

	public static boolean shouldBlockKeyPress(KeyInput input, Slot focusedSlot, ScreenHandler handler, String screenTitle) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options == null || !client.options.dropKey.matchesKey(input)) {
			return false;
		}
		if (focusedSlot == null || handler == null || !isEnabled() || !shouldRespectLocksInScreen(screenTitle)
				|| !isSlotProtected(handler, focusedSlot)) {
			return false;
		}
		if (SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.warning")) {
			sendStatus("skyblocklens.slot_locking.blocked", Formatting.RED);
		}
		return true;
	}

	public static void handleKeyRelease(KeyInput input) {
		if (slotLockKey != null && slotLockKey.matchesKey(input)) {
			slotLockKeyDown = false;
			bindingSourceIndex = -1;
		}
	}

	public static void setKey(InputUtil.Key key) {
		if (slotLockKey == null || key == null) {
			return;
		}
		slotLockKey.setBoundKey(key);
		KeyBinding.updateKeysByCode();
		SkyBlockLensClient.configStore().config().slotLockKey = key.getTranslationKey();
		SkyBlockLensClient.configStore().save();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options != null) {
			client.options.write();
		}
	}

	public static void clearKey() {
		setKey(InputUtil.UNKNOWN_KEY);
	}

	public static void resetKey() {
		setKey(defaultKey());
	}

	public static InputUtil.Key defaultKey() {
		return InputUtil.fromTranslationKey(DEFAULT_KEY_TRANSLATION);
	}

	public static boolean shouldBlockClick(Slot slot, ScreenHandler handler, SlotActionType actionType, int button, String screenTitle) {
		if (slot == null || handler == null || actionType == null || !isEnabled() || !shouldRespectLocksInScreen(screenTitle)) {
			return false;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		boolean sourceProtected = isSlotProtected(handler, slot);
		boolean hotbarTargetProtected = actionType == SlotActionType.SWAP && isHotbarButton(button) && isInventoryIndexProtected(button);
		boolean quickMoveMayTargetProtected = actionType == SlotActionType.QUICK_MOVE
				&& !isPlayerInventorySlot(slot)
				&& hasProtectedPlayerSlot(handler);
		if (!sourceProtected && !hotbarTargetProtected && !quickMoveMayTargetProtected) {
			return false;
		}
		if (config.featureEnabled("slot_locking.warning")) {
			sendStatus("skyblocklens.slot_locking.blocked", Formatting.RED);
		}
		return true;
	}

	public static void renderSlotOverlay(DrawContext context, Slot slot, ScreenHandler handler, String screenTitle, int screenX, int screenY) {
		if (slot == null || handler == null || !isEnabled() || !shouldRespectLocksInScreen(screenTitle)
				|| !SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.overlay")) {
			return;
		}
		boolean locked = isSlotLocked(handler, slot);
		int boundTo = bindingTargetForSlot(slot);
		if (!locked && boundTo < 0) {
			return;
		}
		int x = screenX + slot.x;
		int y = screenY + slot.y;
		if (!locked) {
			drawBoundSlotOverlay(context, x, y, boundTo);
			return;
		}
		drawLockedSlotOverlay(context, x, y);
	}

	public static void renderHotbarOverlay(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null || client.player == null || !isEnabled()
				|| !SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.overlay")) {
			return;
		}
		int hotbarLeft = context.getScaledWindowWidth() / 2 - 91;
		int slotY = context.getScaledWindowHeight() - 19;
		for (int index = HOTBAR_FIRST_SLOT; index <= HOTBAR_LAST_SLOT; index++) {
			boolean locked = isInventoryIndexLocked(index);
			int boundTo = bindingTargetForIndex(index);
			if (!locked && boundTo < 0) {
				continue;
			}
			int slotX = hotbarLeft + 3 + index * 20;
			if (locked) {
				drawLockedSlotOverlay(context, slotX, slotY);
			} else {
				drawBoundSlotOverlay(context, slotX, slotY, boundTo);
			}
		}
	}

	public static boolean shouldBlockSelectedHotbarDrop() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || !isEnabled()) {
			return false;
		}
		int selectedSlot = client.player.getInventory().getSelectedSlot();
		if (!isInventoryIndexProtected(selectedSlot)) {
			return false;
		}
		if (SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.warning")) {
			sendStatus("skyblocklens.slot_locking.blocked", Formatting.RED);
		}
		return true;
	}

	public static void updateBindingPreview(ScreenHandler handler, String screenTitle, int mouseX, int mouseY, int screenX, int screenY) {
		if (!bindingEnabled() || bindingSourceIndex < 0 || handler == null || isStorageLockingDisabled(screenTitle)) {
			return;
		}
		Slot hovered = slotAt(handler, mouseX, mouseY, screenX, screenY);
		if (hovered == null) {
			removeBindingForIndex(bindingSourceIndex);
			return;
		}
		if (isPlayerInventorySlot(hovered) && canBindTo(bindingSourceIndex, hovered)
				&& !isInventoryIndexLocked(hovered.getIndex())) {
			putBinding(bindingSourceIndex, hovered.getIndex());
		}
	}

	public static void renderBindingPreview(DrawContext context, ScreenHandler handler, String screenTitle,
			int mouseX, int mouseY, int screenX, int screenY) {
		if (!bindingEnabled() || bindingSourceIndex < 0 || handler == null || isStorageLockingDisabled(screenTitle)) {
			return;
		}
		Slot source = playerSlotByIndex(handler, bindingSourceIndex);
		if (source == null) {
			return;
		}
		int startX = screenX + source.x + 8;
		int startY = screenY + source.y + 8;
		drawLine(context, startX, startY, mouseX, mouseY, BINDING_COLOR);
	}

	public static void resetLockedSlots() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int count = config.lockedSlots.size() + config.slotBindings.size();
		config.lockedSlots.clear();
		config.slotBindings.clear();
		bindingSourceIndex = -1;
		SkyBlockLensClient.configStore().save();
		if (count == 0) {
			sendStatus("skyblocklens.slot_locking.reset_empty", Formatting.YELLOW);
			return;
		}
		sendStatus("skyblocklens.slot_locking.reset_done", Formatting.YELLOW);
		playLockSound(false);
	}

	private static boolean isEnabled() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return config.enabled && config.featureEnabled("slot_locking.enable");
	}

	private static boolean shouldRespectLocksInScreen(String screenTitle) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (isStorageLockingDisabled(screenTitle)) {
			return false;
		}
		return !isTradeScreen(screenTitle) || config.featureEnabled("slot_locking.lock_slots_in_trade");
	}

	private static boolean isStorageLockingDisabled(String screenTitle) {
		return SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.disable_in_storage")
				&& isStorageScreen(screenTitle);
	}

	private static boolean bindingEnabled() {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		return config.featureEnabled("slot_locking.binding") && config.featureEnabled("slot_locking.enable");
	}

	private static boolean isSlotLocked(ScreenHandler handler, Slot slot) {
		if (!isPlayerInventorySlot(slot)) {
			return false;
		}
		return SkyBlockLensClient.configStore().config().lockedSlots.contains(slotKey(handler, slot));
	}

	private static boolean isSlotProtected(ScreenHandler handler, Slot slot) {
		if (isSlotLocked(handler, slot)) {
			return true;
		}
		return bindingEnabled()
				&& SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.binding_also_locks")
				&& isPlayerInventorySlot(slot)
				&& bindingTargetForIndex(slot.getIndex()) >= 0;
	}

	private static boolean canLockSlot(Slot slot) {
		if (!isPlayerInventorySlot(slot)) {
			return false;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int index = slot.getIndex();
		boolean hotbarSlot = index >= HOTBAR_FIRST_SLOT && index <= HOTBAR_LAST_SLOT;
		if (hotbarSlot) {
			return config.featureEnabled("slot_locking.hotbar");
		}
		return index >= MAIN_INVENTORY_FIRST_SLOT
				&& index <= MAIN_INVENTORY_LAST_SLOT
				&& config.featureEnabled("slot_locking.inventory");
	}

	private static String slotKey(ScreenHandler handler, Slot slot) {
		if (isPlayerInventorySlot(slot)) {
			return "player_inventory:" + slot.getIndex();
		}
		return handler.getClass().getName() + ":" + slot.id;
	}

	private static boolean isInventoryIndexLocked(int index) {
		return SkyBlockLensClient.configStore().config().lockedSlots.contains("player_inventory:" + index);
	}

	private static boolean isInventoryIndexProtected(int index) {
		return isInventoryIndexLocked(index)
				|| (bindingEnabled()
				&& SkyBlockLensClient.configStore().config().featureEnabled("slot_locking.binding_also_locks")
				&& bindingTargetForIndex(index) >= 0);
	}

	private static boolean hasProtectedPlayerSlot(ScreenHandler handler) {
		for (Slot candidate : handler.slots) {
			if (isPlayerInventorySlot(candidate) && isInventoryIndexProtected(candidate.getIndex())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHotbarButton(int button) {
		return button >= HOTBAR_FIRST_SLOT && button <= HOTBAR_LAST_SLOT;
	}

	private static boolean canBindFrom(Slot slot) {
		if (!isPlayerInventorySlot(slot)) {
			return false;
		}
		int index = slot.getIndex();
		return index >= MAIN_INVENTORY_FIRST_SLOT && index <= MAIN_INVENTORY_LAST_SLOT;
	}

	private static boolean canBindTo(int sourceIndex, Slot slot) {
		if (!isPlayerInventorySlot(slot)) {
			return false;
		}
		int targetIndex = slot.getIndex();
		return sourceIndex >= MAIN_INVENTORY_FIRST_SLOT
				&& sourceIndex <= MAIN_INVENTORY_LAST_SLOT
				&& targetIndex >= HOTBAR_FIRST_SLOT
				&& targetIndex <= HOTBAR_LAST_SLOT;
	}

	private static int bindingTargetForSlot(Slot slot) {
		if (!bindingEnabled() || !isPlayerInventorySlot(slot)) {
			return -1;
		}
		return bindingTargetForIndex(slot.getIndex());
	}

	private static int bindingTargetForIndex(int index) {
		for (String binding : SkyBlockLensClient.configStore().config().slotBindings) {
			int arrow = binding.indexOf("->");
			if (arrow <= 0) {
				continue;
			}
			int source = slotIndexFromKey(binding.substring(0, arrow));
			int target = slotIndexFromKey(binding.substring(arrow + 2));
			if (source == index) {
				return target;
			}
			if (target == index) {
				return source;
			}
		}
		return -1;
	}

	private static void putBinding(int sourceIndex, int targetIndex) {
		if (bindingTargetForIndex(sourceIndex) == targetIndex) {
			return;
		}
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		removeBindingForIndex(sourceIndex);
		removeBindingForIndex(targetIndex);
		config.lockedSlots.removeIf(("player_inventory:" + sourceIndex)::equals);
		config.slotBindings.add("player_inventory:" + sourceIndex + "->player_inventory:" + targetIndex);
		SkyBlockLensClient.configStore().save();
	}

	private static void removeBindingForIndex(int index) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		boolean removed = config.slotBindings.removeIf(binding -> {
			int arrow = binding.indexOf("->");
			if (arrow <= 0) {
				return false;
			}
			return slotIndexFromKey(binding.substring(0, arrow)) == index
					|| slotIndexFromKey(binding.substring(arrow + 2)) == index;
		});
		if (removed) {
			SkyBlockLensClient.configStore().save();
		}
	}

	private static int slotIndexFromKey(String key) {
		String prefix = "player_inventory:";
		if (key == null || !key.startsWith(prefix)) {
			return -1;
		}
		try {
			return Integer.parseInt(key.substring(prefix.length()));
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	private static Slot playerSlotByIndex(ScreenHandler handler, int index) {
		for (Slot slot : handler.slots) {
			if (isPlayerInventorySlot(slot) && slot.getIndex() == index) {
				return slot;
			}
		}
		return null;
	}

	private static Slot slotAt(ScreenHandler handler, int mouseX, int mouseY, int screenX, int screenY) {
		for (Slot slot : handler.slots) {
			int x = screenX + slot.x;
			int y = screenY + slot.y;
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				return slot;
			}
		}
		return null;
	}

	private static boolean isTradeScreen(String screenTitle) {
		String trimmed = screenTitle == null ? "" : screenTitle.trim();
		if (trimmed.startsWith("You     ")) {
			return true;
		}
		String normalized = normalizedTitle(screenTitle);
		return normalized.contains("trade") || normalized.contains("trading") || normalized.contains("торгов")
				|| normalized.contains("сделк");
	}

	private static boolean isStorageScreen(String screenTitle) {
		String normalized = normalizedTitle(screenTitle);
		return normalized.equals("storage") || normalized.contains("storage") || normalized.contains("backpack")
				|| normalized.contains("ender chest") || normalized.contains("хранилищ") || normalized.contains("рюкзак");
	}

	private static String normalizedTitle(String screenTitle) {
		return screenTitle == null ? "" : screenTitle.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

	private static void drawBoundSlotOverlay(DrawContext context, int x, int y, int boundTo) {
		context.fill(x, y, x + 16, y + 16, BINDING_MUTED_COLOR);
		context.fill(x, y, x + 16, y + 1, BINDING_COLOR);
		context.fill(x, y + 15, x + 16, y + 16, BINDING_COLOR);
		context.fill(x, y, x + 1, y + 16, BINDING_COLOR);
		context.fill(x + 15, y, x + 16, y + 16, BINDING_COLOR);
		context.fill(x + 3, y + 3, x + 13, y + 5, BINDING_COLOR);
		context.fill(x + 11, y + 5, x + 13, y + 9, BINDING_COLOR);
		String label = String.valueOf(boundTo + 1);
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.textRenderer != null) {
			context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 3, y + 7, 0xFFE7FFF9);
		}
	}

	private static void drawLockedSlotOverlay(DrawContext context, int x, int y) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		int strong = config.overlayArgb(255);
		int muted = config.overlayArgb(102);
		context.fill(x, y, x + 16, y + 16, muted);
		context.fill(x, y, x + 16, y + 1, strong);
		context.fill(x, y + 15, x + 16, y + 16, strong);
		context.fill(x, y, x + 1, y + 16, strong);
		context.fill(x + 15, y, x + 16, y + 16, strong);
		context.fill(x + 11, y + 2, x + 14, y + 5, config.accentArgb());
	}

	private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps <= 0) {
			return;
		}
		for (int i = 0; i <= steps; i += 2) {
			int x = x1 + dx * i / steps;
			int y = y1 + dy * i / steps;
			context.fill(x, y, x + 2, y + 2, color);
		}
	}

	private static void playLockSound(boolean locked) {
		SkyBlockLensConfig config = SkyBlockLensClient.configStore().config();
		if (!config.featureEnabled("slot_locking.sound")) {
			return;
		}
		float volume = Math.max(0.0F, Math.min(1.0F, config.slotLockSoundVolume / 100.0F));
		if (volume <= 0.0F) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, volume, locked ? 0.943F : 0.1F);
		}
	}

	private static void applyConfiguredKey() {
		if (slotLockKey == null) {
			return;
		}
		try {
			slotLockKey.setBoundKey(InputUtil.fromTranslationKey(SkyBlockLensClient.configStore().config().slotLockKey));
		} catch (RuntimeException error) {
			SkyBlockLensClient.configStore().config().slotLockKey = DEFAULT_KEY_TRANSLATION;
			slotLockKey.setBoundKey(defaultKey());
		}
		KeyBinding.updateKeysByCode();
	}

	private static boolean isPlayerInventorySlot(Slot slot) {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null && slot.inventory == client.player.getInventory();
	}

	private static void sendStatus(String key, Formatting formatting) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(Text.literal(SkyBlockLensClient.i18n().tr(key)).formatted(formatting), true);
		}
	}
}
