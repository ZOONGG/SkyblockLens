package com.skyblocklens.core;

import org.slf4j.Logger;

public final class SecurityGuard {
	private SecurityGuard() {
	}

	public static void logStartupPolicy(Logger logger) {
		logger.info("Network features are disabled by default.");
		logger.info("Automation, macros, packet abuse, and cheat-like features are forbidden.");
	}
}
