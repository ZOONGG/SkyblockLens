package com.skyblocklens.items;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skyblocklens.SkyBlockLensClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ItemRepository {
	private static final Gson GSON = new Gson();
	private static final Type LIST_TYPE = new TypeToken<List<SkyBlockItem>>() {
	}.getType();

	private final List<SkyBlockItem> items = new ArrayList<>();
	private final Map<String, List<SkyBlockItem>> byNormalizedName = new HashMap<>();
	private final Map<String, SkyBlockItem> byNormalizedId = new HashMap<>();
	private final Map<String, List<SkyBlockItem>> byVanillaItem = new HashMap<>();

	public List<SkyBlockItem> items() {
		return items;
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public void load() {
		items.clear();
		String path = "/assets/" + SkyBlockLensClient.MOD_ID + "/data/items.json";
		try (InputStream stream = ItemRepository.class.getResourceAsStream(path)) {
			if (stream == null) {
				SkyBlockLensClient.LOGGER.warn("Local item data missing.");
				return;
			}
			try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<SkyBlockItem> loaded = GSON.fromJson(reader, LIST_TYPE);
				if (loaded != null) {
					items.addAll(loaded);
				}
			}
		} catch (Exception error) {
			SkyBlockLensClient.LOGGER.warn("Failed to load local item data.", error);
		}
		items.sort(Comparator.comparing(item -> item.name.toLowerCase(Locale.ROOT)));
		rebuildIndexes();
	}

	public List<SkyBlockItem> search(String query, int limit) {
		return search(query, limit, true, false);
	}

	public List<SkyBlockItem> search(
			String query,
			int limit,
			boolean includeAliases,
			boolean hideMissingData
	) {
		String normalized = normalize(query);
		if (normalized.isEmpty()) {
			return items.stream()
					.filter(item -> !hideMissingData || !item.missingData)
					.limit(limit)
					.toList();
		}
		return items.stream()
				.filter(item -> !hideMissingData || !item.missingData)
				.filter(item -> normalized.isEmpty() || matches(item, normalized, includeAliases))
				.sorted(Comparator
						.comparingInt((SkyBlockItem item) -> searchScore(item, normalized, includeAliases))
						.thenComparing(item -> item.name.toLowerCase(Locale.ROOT)))
				.limit(limit)
				.toList();
	}

	public Optional<SkyBlockItem> findForStack(String displayName, String minecraftItem) {
		return candidatesForStack(displayName, minecraftItem).stream().findFirst();
	}

	public boolean stackMatchesQuery(String displayName, String minecraftItem, String query, boolean includeAliases) {
		String normalizedQuery = normalize(query);
		if (normalizedQuery.isBlank()) {
			return true;
		}
		return candidatesForStack(displayName, minecraftItem).stream()
				.anyMatch(item -> matches(item, normalizedQuery, includeAliases));
	}

	public Optional<SkyBlockItem> findById(String id) {
		return Optional.ofNullable(byNormalizedId.get(normalize(id)));
	}

	public ItemStack iconStack(SkyBlockItem item) {
		if (item == null || item.minecraftItem == null || item.minecraftItem.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			Item itemType = Registries.ITEM.get(Identifier.of(modernItemId(item.minecraftItem)));
			return itemType == Items.AIR ? ItemStack.EMPTY : new ItemStack(itemType);
		} catch (RuntimeException ignored) {
			return ItemStack.EMPTY;
		}
	}

	public ItemStack iconStackForToken(String token) {
		if (token == null || token.isBlank()) {
			return ItemStack.EMPTY;
		}
		String id = tokenId(token);
		SkyBlockItem item = findById(id).orElse(null);
		if (item != null) {
			return iconStack(item);
		}
		return ItemStack.EMPTY;
	}

	public String displayNameForToken(String token) {
		if (token == null || token.isBlank()) {
			return "";
		}
		String id = tokenId(token);
		return findById(id).map(item -> item.name).orElse(id.replace('_', ' '));
	}

	public List<SkyBlockItem> itemsUsing(SkyBlockItem ingredient, int limit) {
		if (ingredient == null) {
			return List.of();
		}
		Set<String> tokens = lookupTokens(ingredient);
		return items.stream()
				.filter(item -> item != ingredient)
				.filter(item -> usesAnyToken(item, tokens))
				.limit(limit)
				.toList();
	}

	private static boolean matches(SkyBlockItem item, String normalized, boolean includeAliases) {
		if (normalize(item.id).contains(normalized) || normalize(item.name).contains(normalized)
				|| normalize(item.rarity).contains(normalized)
				|| normalize(item.category).contains(normalized)
				|| normalize(item.description).contains(normalized)) {
			return true;
		}
		for (String line : item.lore) {
			if (normalize(line).contains(normalized)) {
				return true;
			}
		}
		if (!includeAliases) {
			return false;
		}
		for (String alias : item.aliases) {
			if (normalize(alias).contains(normalized)) {
				return true;
			}
		}
		return false;
	}

	private static int searchScore(SkyBlockItem item, String normalized, boolean includeAliases) {
		int score = 100;
		if (normalized.isBlank()) {
			return score;
		}
		String id = normalize(item.id);
		String name = normalize(item.name);
		if (id.equals(normalized) || name.equals(normalized)) {
			return score;
		}
		if (includeAliases && item.aliases.stream().map(ItemRepository::normalize).anyMatch(normalized::equals)) {
			return score + 2;
		}
		if (id.startsWith(normalized) || name.startsWith(normalized)) {
			return score + 8;
		}
		if (includeAliases && item.aliases.stream().map(ItemRepository::normalize).anyMatch(alias -> alias.startsWith(normalized))) {
			return score + 12;
		}
		return score + 24;
	}

	private void rebuildIndexes() {
		byNormalizedName.clear();
		byNormalizedId.clear();
		byVanillaItem.clear();
		for (SkyBlockItem item : items) {
			String name = normalize(item.name);
			if (!name.isBlank()) {
				byNormalizedName.computeIfAbsent(name, ignored -> new ArrayList<>()).add(item);
			}
			String id = normalize(item.id);
			if (!id.isBlank()) {
				byNormalizedId.putIfAbsent(id, item);
			}
			if (item.matchVanillaItem && item.minecraftItem != null && !item.minecraftItem.isBlank()) {
				byVanillaItem.computeIfAbsent(modernItemId(item.minecraftItem), ignored -> new ArrayList<>()).add(item);
			}
		}
	}

	private List<SkyBlockItem> candidatesForStack(String displayName, String minecraftItem) {
		String normalizedName = normalize(displayName);
		String normalizedItem = modernItemId(minecraftItem == null ? "" : minecraftItem);
		LinkedHashSet<SkyBlockItem> result = new LinkedHashSet<>();
		List<SkyBlockItem> nameMatches = byNormalizedName.get(normalizedName);
		if (nameMatches != null) {
			result.addAll(nameMatches);
		}
		SkyBlockItem idMatch = byNormalizedId.get(normalizedName);
		if (idMatch != null) {
			result.add(idMatch);
		}
		List<SkyBlockItem> vanillaMatches = byVanillaItem.get(normalizedItem);
		if (vanillaMatches != null) {
			result.addAll(vanillaMatches);
		}
		return new ArrayList<>(result);
	}

	private static String modernItemId(String itemId) {
		return switch (itemId == null ? "" : itemId.trim()) {
			case "minecraft:skull", "minecraft:skull_item" -> "minecraft:player_head";
			case "minecraft:golden_rail" -> "minecraft:powered_rail";
			case "minecraft:wooden_button" -> "minecraft:oak_button";
			case "minecraft:wooden_pressure_plate" -> "minecraft:oak_pressure_plate";
			case "minecraft:sign" -> "minecraft:oak_sign";
			case "minecraft:boat" -> "minecraft:oak_boat";
			case "minecraft:bed" -> "minecraft:white_bed";
			case "minecraft:banner" -> "minecraft:white_banner";
			case "minecraft:stained_glass" -> "minecraft:white_stained_glass";
			case "minecraft:stained_glass_pane" -> "minecraft:white_stained_glass_pane";
			case "minecraft:wool" -> "minecraft:white_wool";
			case "minecraft:carpet" -> "minecraft:white_carpet";
			case "minecraft:dye" -> "minecraft:white_dye";
			case "minecraft:fish" -> "minecraft:cod";
			case "minecraft:cooked_fish" -> "minecraft:cooked_cod";
			case "minecraft:snowball" -> "minecraft:snowball";
			case "minecraft:record_13" -> "minecraft:music_disc_13";
			case "minecraft:record_cat" -> "minecraft:music_disc_cat";
			case "minecraft:fireworks" -> "minecraft:firework_rocket";
			case "minecraft:firework_charge" -> "minecraft:firework_star";
			case "minecraft:spawn_egg" -> "minecraft:pig_spawn_egg";
			case "minecraft:monster_egg" -> "minecraft:stone";
			case "minecraft:double_plant" -> "minecraft:sunflower";
			case "minecraft:red_flower" -> "minecraft:poppy";
			case "minecraft:yellow_flower" -> "minecraft:dandelion";
			case "minecraft:reeds" -> "minecraft:sugar_cane";
			case "minecraft:netherbrick" -> "minecraft:nether_brick";
			case "minecraft:clay" -> "minecraft:clay_ball";
			case "minecraft:brick" -> "minecraft:brick";
			case "minecraft:ender_eye" -> "minecraft:ender_eye";
			case "minecraft:glowstone_dust" -> "minecraft:glowstone_dust";
			default -> itemId == null ? "" : itemId.trim();
		};
	}

	private static Set<String> lookupTokens(SkyBlockItem item) {
		Set<String> tokens = new LinkedHashSet<>();
		tokens.add(normalize(item.id));
		tokens.add(normalize(item.name));
		for (String alias : item.aliases) {
			tokens.add(normalize(alias));
		}
		tokens.remove("");
		return tokens;
	}

	public static String tokenId(String token) {
		if (token == null) {
			return "";
		}
		String value = token.trim();
		int separator = value.lastIndexOf(':');
		if (separator <= 0 || separator == value.length() - 1) {
			return value;
		}
		String amount = value.substring(separator + 1);
		for (int i = 0; i < amount.length(); i++) {
			if (!Character.isDigit(amount.charAt(i))) {
				return value;
			}
		}
		return value.substring(0, separator);
	}

	private static boolean usesAnyToken(SkyBlockItem item, Set<String> tokens) {
		for (String recipeItem : item.recipeItems) {
			if (tokens.contains(normalize(recipeItem))) {
				return true;
			}
		}
		for (String recipeLine : item.recipe) {
			String normalizedLine = normalize(recipeLine);
			for (String token : tokens) {
				if (normalizedLine.contains(token)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String stripped = value.replaceAll("(?i)\\u00A7[0-9A-FK-OR]", "");
		return stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9а-яё]+", "");
	}
}
