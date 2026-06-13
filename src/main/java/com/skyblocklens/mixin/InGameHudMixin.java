package com.skyblocklens.mixin;

import com.skyblocklens.hud.ScoreboardBackgroundController;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@ModifyArg(
			method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
			),
			index = 4
	)
	private int skyblocklens$scoreboardBackground(int color) {
		return ScoreboardBackgroundController.adjustBackgroundColor(color);
	}
}
