package com.qwant.qwantdlc.setting;

public class ModeSetting extends Setting {
	private final String[] options;
	private int index;

	public ModeSetting(String name, int defaultIndex, String... options) {
		super(name);
		if (options == null || options.length == 0) {
			throw new IllegalArgumentException("ModeSetting needs at least one option");
		}
		this.options = options;
		this.index = clamp(defaultIndex);
	}

	public String[] getOptions() {
		return options;
	}

	public int getIndex() {
		return index;
	}

	public String getCurrent() {
		return options[index];
	}

	public void setIndex(int i) {
		this.index = clamp(i);
	}

	public boolean is(String name) {
		return options[index].equalsIgnoreCase(name);
	}

	private int clamp(int i) {
		if (i < 0) return 0;
		if (i >= options.length) return options.length - 1;
		return i;
	}
}
