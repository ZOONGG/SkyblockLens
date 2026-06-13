package com.skyblocklens.ui;

record UiRect(int x, int y, int width, int height) {
	int right() {
		return x + width;
	}

	int bottom() {
		return y + height;
	}

	int centerX() {
		return x + width / 2;
	}

	boolean contains(double mouseX, double mouseY) {
		if (width <= 0 || height <= 0) {
			return false;
		}
		return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
	}

	UiRect expand(int amount) {
		return new UiRect(x - amount, y - amount, width + amount * 2, height + amount * 2);
	}
}
