package com.skyblocklens.mixin;

import com.skyblocklens.access.HandledScreenAccess;
import com.skyblocklens.slotlocking.SlotLockController;
import com.skyblocklens.ui.InventoryButtonController;
import com.skyblocklens.ui.ToolbarController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void skyblocklens$renderInventoryOverlay(DrawContext context, int mouseX, int mouseY, float delta,
			CallbackInfo ci) {
		HandledScreenAccess access = (HandledScreenAccess) this;
		SlotLockController.updateBindingPreview(access.skyblocklens$handler(), "", mouseX, mouseY,
				access.skyblocklens$x(), access.skyblocklens$y());
		SlotLockController.renderBindingPreview(context, access.skyblocklens$handler(), "", mouseX, mouseY,
				access.skyblocklens$x(), access.skyblocklens$y());
		ToolbarController.render(context, mouseX, mouseY, access.skyblocklens$x(), access.skyblocklens$y(),
				access.skyblocklens$backgroundWidth(), access.skyblocklens$backgroundHeight());
		InventoryButtonController.render(context, mouseX, mouseY, access.skyblocklens$x(), access.skyblocklens$y());
	}
}
