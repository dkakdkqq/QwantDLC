package com.qwant.qwantdlc.setting;

public class MultiSelectSetting extends Setting {
	private final String[] options;
	private final boolean[] selected;

	public MultiSelectSetting(String name, String[] options, boolean[] defaults) {
		super(name);
		if (options == null || options.length == 0) {
			throw new IllegalArgumentException("MultiSelectSetting needs at least one option");
		}
		this.options = options;
		this.selected = new boolean[options.length];
		if (defaults != null) {
			for (int i = 0; i < Math.min(defaults.length, this.selected.length); i++) {
				this.selected[i] = defaults[i];
			}
		}
	}

	public String[] getOptions() {
		return options;
	}

	public boolean isSelected(int i) {
		return i >= 0 && i < selected.length && selected[i];
	}

	public boolean isSelected(String label) {
		for (int i = 0; i < options.length; i++) {
			if (options[i].equalsIgnoreCase(label)) return selected[i];
		}
		return false;
	}

	public void toggle(int i) {
		if (i >= 0 && i < selected.length) selected[i] = !selected[i];
	}

	public void set(int i, boolean v) {
		if (i >= 0 && i < selected.length) selected[i] = v;
	}
}
