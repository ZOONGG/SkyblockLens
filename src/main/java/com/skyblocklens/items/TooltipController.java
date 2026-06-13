package com.skyblocklens.items;

import com.skyblocklens.config.SkyBlockLensConfig;
import com.skyblocklens.i18n.SblI18n;
import com.skyblocklens.SkyBlockLensClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TooltipController {
	private static final Pattern PET_LEVEL_PATTERN = Pattern.compile(".*\\[Lvl\\s+\\d+].*");
	private static final Pattern PET_KIND_PATTERN = Pattern.compile(
			".*\\b(Farming|Combat|Fishing|Mining|Foraging|Enchanting|Alchemy)\\s+(Mount|Pet|Morph)\\b.*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PET_XP_PATTERN = Pattern.compile(
			"^(.*?)([0-9][0-9,.]*\\s*[kKmMbB]?)\\s*/\\s*([0-9][0-9,.]*\\s*[kKmMbB]?)(.*)$");
	private static final Pattern COMPACT_NUMBER_PATTERN = Pattern.compile("^([0-9][0-9,.]*)([kKmMbB]?)$");

	private TooltipController() {
	}

	public static void appendTooltip(
			SkyBlockLensConfig config,
			ItemRepository repository,
			SblI18n i18n,
			ItemStack stack,
			List<Text> lines
	) {
		if (!config.enabled || !SkyBlockLensClient.skyBlockFeaturesAllowed() || stack == null || stack.isEmpty()) {
			return;
		}
		String displayName = stack.getName().getString();
		appendPetExpExtension(config, i18n, displayName, lines);
		if (!config.enhancedTooltips || !config.featureEnabled("tooltip_tweaks.enhanced_item_tooltips")) {
			return;
		}
		String minecraftItem = Registries.ITEM.getId(stack.getItem()).toString();
		repository.findForStack(displayName, minecraftItem).ifPresent(item -> {
			lines.add(Text.literal(""));
			lines.add(Text.literal(i18n.tr("skyblocklens.items.tooltip_header")).formatted(Formatting.AQUA, Formatting.BOLD));
			if (config.featureEnabled("tooltip_tweaks.internal_id")) {
				lines.add(Text.literal(i18n.tr("skyblocklens.items.tooltip_internal_id") + item.id).formatted(Formatting.DARK_GRAY));
			}
			if (!item.rarity.isBlank() && config.featureEnabled("tooltip_tweaks.rarity")) {
				lines.add(Text.literal(item.rarity).formatted(Formatting.GRAY));
			}
			if (!item.category.isBlank() && config.featureEnabled("tooltip_tweaks.category")) {
				lines.add(Text.literal(item.category).formatted(Formatting.DARK_GRAY));
			}
			if (!item.description.isBlank()) {
				lines.add(Text.literal(item.description).formatted(Formatting.DARK_AQUA));
			}
			if (!item.recipe.isEmpty() && config.featureEnabled("itemlist.recipe_view")) {
				lines.add(Text.literal(i18n.tr("skyblocklens.items.tooltip_recipe")).formatted(Formatting.GREEN));
			} else if (item.recipe.isEmpty() && config.featureEnabled("tooltip_tweaks.missing_recipe_notice")) {
				lines.add(Text.literal(i18n.tr("skyblocklens.items.tooltip_missing")).formatted(Formatting.DARK_GRAY));
			}
		});
	}

	private static void appendPetExpExtension(
			SkyBlockLensConfig config,
			SblI18n i18n,
			String displayName,
			List<Text> lines
	) {
		if (!config.featureEnabled("tooltip_tweaks.pet_extend_exp") || lines.size() < 2
				|| alreadyHasPetExpExtension(i18n, lines) || !isPetTooltip(displayName, lines)) {
			return;
		}
		for (int index = lines.size() - 1; index >= 0; index--) {
			String rawLine = lines.get(index).getString();
			if (!couldBePetExpLine(rawLine)) {
				continue;
			}
			Matcher matcher = PET_XP_PATTERN.matcher(rawLine);
			if (!matcher.matches()) {
				continue;
			}
			String currentRaw = matcher.group(2);
			String requiredRaw = matcher.group(3);
			if (!isAbbreviatedNumber(currentRaw) && !isAbbreviatedNumber(requiredRaw)) {
				continue;
			}
			String current = expandCompactNumber(currentRaw);
			String required = expandCompactNumber(requiredRaw);
			lines.add(index + 1, Text.literal(formatPetExpLine(i18n, current, required)).formatted(Formatting.GRAY));
			return;
		}
	}

	private static boolean alreadyHasPetExpExtension(SblI18n i18n, List<Text> lines) {
		String template = i18n.tr("skyblocklens.items.tooltip_pet_exp");
		String marker = template.split("\\{current}", 2)[0].trim();
		if (marker.isBlank()) {
			return false;
		}
		for (Text line : lines) {
			if (line.getString().trim().startsWith(marker)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPetTooltip(String displayName, List<Text> lines) {
		String lowerName = displayName.toLowerCase(Locale.ROOT);
		if (lowerName.endsWith(" pet") || lowerName.contains(" pet ")) {
			return true;
		}
		int limit = Math.min(lines.size(), 4);
		for (int index = 0; index < limit; index++) {
			String line = lines.get(index).getString();
			if (PET_LEVEL_PATTERN.matcher(line).matches() || PET_KIND_PATTERN.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}

	private static boolean couldBePetExpLine(String line) {
		if (!line.contains("/")) {
			return false;
		}
		String lower = line.toLowerCase(Locale.ROOT);
		return lower.contains("xp") || lower.contains("exp") || lower.contains("experience")
				|| lower.contains("level") || lower.contains("progress");
	}

	private static boolean isAbbreviatedNumber(String value) {
		String normalized = normalizeCompactNumber(value);
		return normalized.endsWith("k") || normalized.endsWith("m") || normalized.endsWith("b");
	}

	private static String expandCompactNumber(String value) {
		String normalized = normalizeCompactNumber(value);
		Matcher matcher = COMPACT_NUMBER_PATTERN.matcher(normalized);
		if (!matcher.matches()) {
			return value.trim();
		}
		double number;
		try {
			number = Double.parseDouble(matcher.group(1));
		} catch (NumberFormatException error) {
			return value.trim();
		}
		long multiplier = switch (matcher.group(2)) {
			case "k" -> 1_000L;
			case "m" -> 1_000_000L;
			case "b" -> 1_000_000_000L;
			default -> 1L;
		};
		return String.format(Locale.US, "%,d", Math.round(number * multiplier));
	}

	private static String normalizeCompactNumber(String value) {
		return value.replace(",", "").replace(" ", "").trim().toLowerCase(Locale.ROOT);
	}

	private static String formatPetExpLine(SblI18n i18n, String current, String required) {
		return i18n.tr("skyblocklens.items.tooltip_pet_exp")
				.replace("{current}", current)
				.replace("{required}", required);
	}
}
