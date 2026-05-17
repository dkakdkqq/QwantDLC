package com.qwant.qwantdlc.setting;

public class BoolSetting extends Setting {
	private boolean value;

	public BoolSetting(String name, boolean defaultValue) {
		super(name);
		this.value = defaultValue;
	}

	public boolean get() {
		return value;
	}

	public void set(boolean v) {
		this.value = v;
	}

	public void toggle() {
		this.value = !this.value;
	}
}
