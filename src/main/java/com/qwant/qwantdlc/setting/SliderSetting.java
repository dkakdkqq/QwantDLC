package com.qwant.qwantdlc.setting;

import java.util.Locale;

public class SliderSetting extends Setting {
	private final float min;
	private final float max;
	private final int decimals;
	private float value;

	public SliderSetting(String name, float defaultValue, float min, float max, int decimals) {
		super(name);
		this.min = min;
		this.max = max;
		this.decimals = decimals;
		this.value = clamp(defaultValue);
	}

	public float get() {
		return value;
	}

	public void set(float v) {
		this.value = clamp(v);
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}

	public int getDecimals() {
		return decimals;
	}

	public float getRatio() {
		if (max - min < 1e-6f) return 0f;
		return (value - min) / (max - min);
	}

	public void setRatio(float t) {
		t = Math.max(0f, Math.min(1f, t));
		set(min + t * (max - min));
	}

	public String formatValue() {
		return String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	private float clamp(float v) {
		return v < min ? min : (v > max ? max : v);
	}
}
