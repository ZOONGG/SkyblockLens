package com.skyblocklens;

import com.skyblocklens.chat.ChatController;
import com.skyblocklens.config.ConfigStore;
import com.skyblocklens.core.SecurityGuard;
import com.skyblocklens.hud.HudManager;
import com.skyblocklens.i18n.SblI18n;
import com.skyblocklens.items.ItemRepository;
import com.skyblocklens.items.TooltipController;
import com.skyblocklens.notifications.NotificationManager;
import com.skyblocklens.skyblock.SkyBlockContext;
import com.skyblocklens.slotlocking.SlotLockController;
import com.skyblocklens.ui.SettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkyBlockLensClient implements ClientModInitializer {
	public static final String MOD_ID = "skyblocklens";
	public static final Logger LOGGER = LoggerFactory.getLogger("SkyBlock Lens");
	private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

	private static ConfigStore configStore;
	private static SblI18n i18n;
	private static ItemRepository itemRepository;
	private static SkyBlockContext skyBlockContext;
	private static HudManager hudManager;
	private static NotificationManager notificationManager;
	private static ChatController chatController;
	private static KeyBinding openSettingsKey;
	private static KeyBinding slotLockKey;

	@Override
	public void onInitializeClient() {
		configStore = new ConfigStore();
		configStore.load();
		i18n = new SblI18n(configStore.config().language);
		itemRepository = new ItemRepository();
		itemRepository.load();
		skyBlockContext = new SkyBlockContext();
		hudManager = new HudManager(configStore, skyBlockContext, i18n);
		notificationManager = new NotificationManager(configStore, i18n);
		chatController = new ChatController(configStore, i18n, notificationManager);

		SecurityGuard.logStartupPolicy(LOGGER);
		registerKeybinds();
		registerCommands();
		registerEvents();
		LOGGER.info("SkyBlock Lens initialized with {} local items.", itemRepository.items().size());
	}

	public static ConfigStore configStore() {
		return configStore;
	}

	public static SblI18n i18n() {
		return i18n;
	}

	public static ItemRepository itemRepository() {
		return itemRepository;
	}

	public static SkyBlockContext skyBlockContext() {
		return skyBlockContext;
	}

	public static boolean skyBlockFeaturesAllowed() {
		if (configStore == null || skyBlockContext == null) {
			return true;
		}
		return configStore.config().enabled
				&& (!configStore.config().onlyOnSkyBlock || skyBlockContext.inSkyBlock());
	}

	public static HudManager hudManager() {
		return hudManager;
	}

	public static NotificationManager notificationManager() {
		return notificationManager;
	}

	public static KeyBinding slotLockKey() {
		return slotLockKey;
	}

	public static KeyBinding.Category keyCategory() {
		return KEY_CATEGORY;
	}

	public static void reloadLanguage() {
		i18n.setLanguage(configStore.config().language);
	}

	public static void openSettings() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> client.setScreen(new SettingsScreen(null)));
	}

	private static void registerKeybinds() {
		openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.skyblocklens.open_settings",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KEY_CATEGORY
		));
		slotLockKey = SlotLockController.registerKeybind();
	}

	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("sbl")
					.executes(context -> {
						openSettings();
						return 1;
					}));
			dispatcher.register(ClientCommandManager.literal("skyblocklens")
					.executes(context -> {
						openSettings();
						return 1;
					}));
		});
	}

	private static void registerEvents() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettingsKey.wasPressed()) {
				client.setScreen(new SettingsScreen(null));
			}
			skyBlockContext.update(client);
		});

		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			hudManager.render(drawContext);
			SlotLockController.renderHotbarOverlay(drawContext);
			notificationManager.render(drawContext);
		});

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) ->
				TooltipController.appendTooltip(configStore.config(), itemRepository, i18n, stack, lines));

		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
				chatController.allowChatMessage(message));

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			skyBlockContext.observeGameMessage(message, overlay);
			return chatController.modifyGameMessage(message, overlay);
		});
	}

}
