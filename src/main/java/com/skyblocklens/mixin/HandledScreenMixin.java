package com.skyblocklens.mixin;

import com.skyblocklens.access.HandledScreenAccess;
import com.skyblocklens.slotlocking.SlotLockController;
import com.skyblocklens.ui.InventoryButtonController;
import com.skyblocklens.ui.ToolbarController;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements HandledScreenAccess {
	@Shadow
	protected Slot focusedSlot;

	@Shadow
	@Final
	protected T handler;

	@Shadow
	protected int x;

	@Shadow
	protected int y;

	@Shadow
	protected int backgroundHeight;

	@Shadow
	protected int backgroundWidth;

	protected HandledScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void skyblocklens$handleSlotLockKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if (ToolbarController.keyPressed(input)) {
			cir.setReturnValue(true);
			return;
		}
		if (SlotLockController.handleKeyPress(input, focusedSlot, handler, getTitle().getString())) {
			cir.setReturnValue(true);
			return;
		}
		if (SlotLockController.shouldBlockKeyPress(input, focusedSlot, handler, getTitle().getString())) {
			cir.setReturnValue(true);
		}
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		SlotLockController.handleKeyRelease(input);
		return super.keyReleased(input);
	}

	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
			at = @At("HEAD"),
			cancellable = true)
	private void skyblocklens$blockLockedSlot(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (SlotLockController.handleBoundQuickSwap(slot, handler, actionType, button, getTitle().getString())) {
			ci.cancel();
			return;
		}
		if (SlotLockController.shouldBlockClick(slot, handler, actionType, button, getTitle().getString())) {
			ci.cancel();
		}
	}

	@Inject(method = "drawSlot", at = @At("TAIL"))
	private void skyblocklens$renderSlotLockOverlay(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		ToolbarController.renderSlotOverlay(context, slot, 0, 0);
		SlotLockController.renderSlotOverlay(context, slot, handler, getTitle().getString(), 0, 0);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void skyblocklens$renderInventoryButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if ((Object) this instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
			return;
		}
		SlotLockController.updateBindingPreview(handler, getTitle().getString(), mouseX, mouseY, x, y);
		SlotLockController.renderBindingPreview(context, handler, getTitle().getString(), mouseX, mouseY, x, y);
		ToolbarController.render(context, mouseX, mouseY, x, y, backgroundWidth, backgroundHeight);
		InventoryButtonController.render(context, mouseX, mouseY, x, y);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void skyblocklens$handleInventoryButtonClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (ToolbarController.mouseClicked(click, handler, getTitle().getString(), x, y)) {
			cir.setReturnValue(true);
			return;
		}
		if (InventoryButtonController.mouseClicked(click, x, y)) {
			cir.setReturnValue(true);
		}
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (ToolbarController.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (ToolbarController.mouseScrolled(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public int skyblocklens$x() {
		return x;
	}

	@Override
	public int skyblocklens$y() {
		return y;
	}

	@Override
	public int skyblocklens$backgroundWidth() {
		return backgroundWidth;
	}

	@Override
	public int skyblocklens$backgroundHeight() {
		return backgroundHeight;
	}

	@Override
	public ScreenHandler skyblocklens$handler() {
		return handler;
	}
}
