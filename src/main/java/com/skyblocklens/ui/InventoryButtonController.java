package com.skyblocklens.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class InventoryButtonController {
	private InventoryButtonController() {
	}

	public static void render(DrawContext context, int mouseX, int mouseY, int screenX, int screenY) {
	}

	public static boolean mouseClicked(Click click, int screenX, int screenY) {
		return false;
	}

	public static ItemStack iconStack(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			Item item = Registries.ITEM.get(Identifier.of(itemId));
			return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		} catch (RuntimeException ignored) {
			return ItemStack.EMPTY;
		}
	}
}
