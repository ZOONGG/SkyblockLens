package com.skyblocklens.mixin;

import com.skyblocklens.ui.NameplateController;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
	private void skyblocklens$showOwnName(PlayerLikeEntity player, double distance,
			CallbackInfoReturnable<Boolean> cir) {
		if (NameplateController.shouldShowOwnName(player)) {
			cir.setReturnValue(true);
		}
	}

	@ModifyArg(
			method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"
			),
			index = 3
	)
	private Text skyblocklens$replaceOwnName(Text label) {
		return NameplateController.overrideOwnNameLabel(label);
	}
}
