package com.skyblocklens.hud;

import com.skyblocklens.SkyBlockLensClient;

public final class ScoreboardBackgroundController {
	private ScoreboardBackgroundController() {
	}

	public static int adjustBackgroundColor(int original) {
		if (SkyBlockLensClient.configStore() == null
				|| !SkyBlockLensClient.configStore().config().enabled) {
			return original;
		}
		return SkyBlockLensClient.configStore().config().scoreboardBackgroundArgb();
	}
}
