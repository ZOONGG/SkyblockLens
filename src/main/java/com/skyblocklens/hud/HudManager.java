package com.skyblocklens.hud;

import com.skyblocklens.SkyBlockLensClient;
import com.skyblocklens.config.ConfigStore;
import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.i18n.SblI18n;
import com.skyblocklens.skyblock.SkyBlockContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudManager {
	private final ConfigStore configStore;
	private final SkyBlockContext context;
	private final SblI18n i18n;

	public HudManager(ConfigStore configStore, SkyBlockContext context, SblI18n i18n) {
		this.configStore = configStore;
		this.context = context;
		this.i18n = i18n;
	}

	public Map<String, String> previewLines() {
		Map<String, String> lines = new LinkedHashMap<>();
		lines.put("skyblock_status", i18n.tr("skyblocklens.hud.skyblock_status") + ": " +
				(context.inSkyBlock() ? i18n.tr("skyblocklens.hud.context.skyblock") : i18n.tr("skyblocklens.hud.context.not_skyblock")));
		lines.put("location", i18n.tr("skyblocklens.hud.location") + ": " + valueOrUnknown(context.location()));
		lines.put("purse", i18n.tr("skyblocklens.hud.purse") + ": " + valueOrUnknown(context.purse()));
		lines.put("bits", i18n.tr("skyblocklens.hud.bits") + ": " + valueOrUnknown(context.bits()));
		return lines;
	}

	public void render(DrawContext drawContext) {
		SkyBlockLensConfig config = configStore.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (!config.enabled || client.options.hudHidden) {
			return;
		}
		if (!SkyBlockLensClient.skyBlockFeaturesAllowed()) {
			return;
		}

		for (Map.Entry<String, String> entry : previewLines().entrySet()) {
			SkyBlockLensConfig.HudModuleConfig module = config.hudModules.get(entry.getKey());
			if (module == null || !module.enabled) {
				continue;
			}
			double baseScale = module.scale <= 0.0D ? 1.0D : module.scale;
			int naturalWidth = (int) Math.round((client.textRenderer.getWidth(entry.getValue()) + 12) * baseScale);
			int naturalHeight = Math.max(22, (int) Math.round(18 * baseScale));
			int width = module.width > 0 ? module.width : naturalWidth;
			int height = module.height > 0 ? module.height : naturalHeight;
			double scale = displayScale(module, width, height, client.textRenderer.getWidth(entry.getValue()));
			drawContext.fill(module.x - 3, module.y - 3, module.x + width, module.y + height, config.hudBackgroundArgb());
			drawContext.getMatrices().pushMatrix();
			drawContext.getMatrices().translate(module.x + 5, module.y + Math.max(5, (int) Math.round((height - 8 * scale) / 2.0D)));
			drawContext.getMatrices().scale((float) scale, (float) scale);
			drawContext.drawTextWithShadow(client.textRenderer, Text.literal(entry.getValue()), 0, 0, 0xFFFFFFFF);
			drawContext.getMatrices().popMatrix();
		}
	}

	private static double displayScale(SkyBlockLensConfig.HudModuleConfig module, int width, int height, int textWidth) {
		if (module.width > 0 || module.height > 0) {
			double heightScale = (height - 10.0D) / 8.0D;
			double widthScale = (width - 12.0D) / Math.max(1.0D, textWidth);
			return Math.max(0.55D, Math.min(2.5D, Math.min(widthScale, heightScale)));
		}
		return module.scale <= 0.0D ? 1.0D : module.scale;
	}

	public void save() {
		configStore.save();
	}

	private String valueOrUnknown(String value) {
		if (value == null || value.isBlank()) {
			return i18n.tr("skyblocklens.hud.context.unknown");
		}
		return value;
	}

}
