package com.skyblocklens.config;

import java.util.List;

public record SkyBlockSettingGroup(
		String id,
		String titleKey,
		List<SkyBlockSetting> settings,
		boolean expandedByDefault
) {
	public static SkyBlockSettingGroup of(String id, boolean expandedByDefault, SkyBlockSetting... settings) {
		return new SkyBlockSettingGroup(
				id,
				"skyblocklens.group." + id,
				List.of(settings),
				expandedByDefault
		);
	}
}
