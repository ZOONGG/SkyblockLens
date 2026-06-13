package com.skyblocklens.items;

import java.util.ArrayList;
import java.util.List;

public final class SkyBlockItem {
	public String id = "";
	public String name = "";
	public String category = "";
	public String rarity = "";
	public String minecraftItem = "";
	public boolean matchVanillaItem;
	public List<String> aliases = new ArrayList<>();
	public String description = "";
	public List<String> lore = new ArrayList<>();
	public List<String> recipe = new ArrayList<>();
	public List<String> recipeItems = new ArrayList<>();
	public List<String> recipeSlots = new ArrayList<>();
	public List<String> sources = new ArrayList<>();
	public String wikiUrl = "";
	public String npcSellPrice = "";
	public String bazaarInfo = "";
	public String auctionInfo = "";
	public boolean missingData;
}
