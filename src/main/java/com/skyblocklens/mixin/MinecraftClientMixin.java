package com.skyblocklens.mixin;

import com.skyblocklens.slotlocking.SlotLockController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Redirect(method = "handleInputEvents",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/network/ClientPlayerEntity;dropSelectedItem(Z)Z"))
	private boolean skyblocklens$blockLockedHotbarDrop(ClientPlayerEntity player, boolean entireStack) {
		if (SlotLockController.shouldBlockSelectedHotbarDrop()) {
			return false;
		}
		return player.dropSelectedItem(entireStack);
	}
}
