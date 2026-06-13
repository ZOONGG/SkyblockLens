package com.skyblocklens.skyblock;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Locale;

public final class SkyBlockContext {
	private boolean onHypixel;
	private boolean inSkyBlock;
	private String location = "";
	private String purse = "";
	private String bits = "";
	private String lastActionBar = "";
	private int ticks;

	public boolean onHypixel() {
		return onHypixel;
	}

	public boolean inSkyBlock() {
		return inSkyBlock;
	}

	public String location() {
		return location;
	}

	public String purse() {
		return purse;
	}

	public String bits() {
		return bits;
	}

	public String lastActionBar() {
		return lastActionBar;
	}

	public void update(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			onHypixel = false;
			inSkyBlock = false;
			return;
		}
		ticks++;
		if (ticks % 20 != 0) {
			return;
		}
		onHypixel = detectHypixel(client);
		if (client.getNetworkHandler() != null) {
			for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
				Text displayName = entry.getDisplayName();
				if (displayName != null) {
					observeLine(displayName.getString());
				}
			}
		}
		inSkyBlock = onHypixel && (inSkyBlock || hasSkyBlockText(location) || hasSkyBlockText(lastActionBar));
	}

	public void observeGameMessage(Text message, boolean overlay) {
		if (!overlay || message == null) {
			return;
		}
		lastActionBar = message.getString();
		observeLine(lastActionBar);
	}

	private static boolean detectHypixel(MinecraftClient client) {
		if (client.isInSingleplayer()) {
			return false;
		}
		if (client.getCurrentServerEntry() == null || client.getCurrentServerEntry().address == null) {
			return false;
		}
		String address = client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
		return address.contains("hypixel.net") || address.contains("hypixel.io");
	}

	private void observeLine(String rawLine) {
		String line = rawLine == null ? "" : rawLine.trim();
		String lower = line.toLowerCase(Locale.ROOT);
		if (lower.contains("skyblock")) {
			inSkyBlock = true;
		}
		if (lower.startsWith("area:") || lower.startsWith("location:")) {
			location = afterColon(line);
			inSkyBlock = true;
		}
		if (lower.startsWith("purse:")) {
			purse = afterColon(line);
			inSkyBlock = true;
		}
		if (lower.startsWith("bits:")) {
			bits = afterColon(line);
			inSkyBlock = true;
		}
	}

	private static boolean hasSkyBlockText(String value) {
		return value != null && value.toLowerCase(Locale.ROOT).contains("skyblock");
	}

	private static String afterColon(String value) {
		int colon = value.indexOf(':');
		if (colon < 0 || colon + 1 >= value.length()) {
			return value;
		}
		return value.substring(colon + 1).trim();
	}
}
