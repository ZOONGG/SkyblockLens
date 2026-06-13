package com.skyblocklens.config;

import java.util.List;

public record SkyBlockSettingPage(
		SkyBlockSettingCategory category,
		List<SkyBlockSettingGroup> groups
) {
	public static SkyBlockSettingPage of(SkyBlockSettingCategory category, SkyBlockSettingGroup... groups) {
		return new SkyBlockSettingPage(category, List.of(groups));
	}
}
