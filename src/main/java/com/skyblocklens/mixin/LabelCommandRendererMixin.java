package com.skyblocklens.mixin;

import com.skyblocklens.ui.NameplateController;
import net.minecraft.client.render.command.LabelCommandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LabelCommandRenderer.class)
public abstract class LabelCommandRendererMixin {
	@ModifyArg(
			method = "render(Lnet/minecraft/client/render/command/BatchingRenderCommandQueue;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/font/TextRenderer;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"
			),
			index = 3
	)
	private int skyblocklens$nameplateTextColor(int color) {
		return NameplateController.adjustTextColor(color);
	}

	@ModifyArg(
			method = "render(Lnet/minecraft/client/render/command/BatchingRenderCommandQueue;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/font/TextRenderer;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"
			),
			index = 8
	)
	private int skyblocklens$nameplateBackgroundColor(int color) {
		return NameplateController.adjustBackgroundColor(color);
	}
}
