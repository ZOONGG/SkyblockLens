package com.skyblocklens.ui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class PercentSlider extends SliderWidget {
	private final double min;
	private final double max;
	private final DoubleConsumer consumer;
	private final DoubleFunction<String> label;

	public PercentSlider(
			int x,
			int y,
			int width,
			int height,
			double min,
			double max,
			double current,
			DoubleConsumer consumer,
			DoubleFunction<String> label
	) {
		super(x, y, width, height, Text.literal(label.apply(current)), normalize(min, max, current));
		this.min = min;
		this.max = max;
		this.consumer = consumer;
		this.label = label;
	}

	@Override
	protected void updateMessage() {
		setMessage(Text.literal(label.apply(actualValue())));
	}

	@Override
	protected void applyValue() {
		consumer.accept(actualValue());
	}

	private double actualValue() {
		return min + (max - min) * value;
	}

	private static double normalize(double min, double max, double current) {
		if (max <= min) {
			return 0.0D;
		}
		return Math.max(0.0D, Math.min(1.0D, (current - min) / (max - min)));
	}
}
