package com.qwant.qwantdlc.module;

public enum Category {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	RENDER("Render"),
	PLAYER("Player"),
	MISC("Misc"),
	THEMES("Themes");

	private final String display;

	Category(String display) {
		this.display = display;
	}

	public String getDisplay() {
		return display;
	}
}
