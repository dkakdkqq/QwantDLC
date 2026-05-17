package com.qwant.qwantdlc.setting;

/**
 * Base class for module settings shown in the right-side settings panel of
 * the ClickGUI.
 */
public abstract class Setting {
	protected final String name;

	protected Setting(String name) {
		this.name = name;
	}

	public final String getName() {
		return name;
	}
}
