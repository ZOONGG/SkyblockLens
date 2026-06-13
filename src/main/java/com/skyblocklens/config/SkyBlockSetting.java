package com.skyblocklens.config;

public record SkyBlockSetting(
		String id,
		String titleKey,
		String descriptionKey,
		SkyBlockSettingControl control,
		boolean defaultEnabled
) {
	public static SkyBlockSetting toggle(String id, boolean defaultEnabled) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.TOGGLE,
				defaultEnabled
		);
	}

	public static SkyBlockSetting action(String id) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.ACTION,
				false
		);
	}

	public static SkyBlockSetting dropdown(String id) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.DROPDOWN,
				false
		);
	}

	public static SkyBlockSetting slider(String id) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.SLIDER,
				false
		);
	}

	public static SkyBlockSetting keybind(String id) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.KEYBIND,
				false
		);
	}

	public static SkyBlockSetting color(String id) {
		return new SkyBlockSetting(
				id,
				"skyblocklens.setting." + id + ".title",
				"skyblocklens.setting." + id + ".desc",
				SkyBlockSettingControl.COLOR,
				false
		);
	}
}
