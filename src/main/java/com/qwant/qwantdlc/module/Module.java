package com.qwant.qwantdlc.module;

import org.lwjgl.glfw.GLFW;

/**
 * Base class for all modules.
 * Holds: name, description, category, toggled, key.
 */
public abstract class Module {
	protected final String name;
	protected final String description;
	protected final Category category;
	protected boolean toggled;
	protected int key;

	public Module(String name, Category category) {
		this(name, "", category, GLFW.GLFW_KEY_UNKNOWN);
	}

	public Module(String name, String description, Category category) {
		this(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
	}

	public Module(String name, Category category, int key) {
		this(name, "", category, key);
	}

	public Module(String name, String description, Category category, int key) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.toggled = false;
		this.key = key;
	}

	public final String getName() {
		return name;
	}

	public final String getDescription() {
		return description;
	}

	public final Category getCategory() {
		return category;
	}

	public final boolean isToggled() {
		return toggled;
	}

	public final int getKey() {
		return key;
	}

	public final void setKey(int key) {
		this.key = key;
	}

	public final void toggle() {
		this.toggled = !this.toggled;
		if (this.toggled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public final void setToggled(boolean state) {
		if (state == this.toggled) return;
		toggle();
	}

	// Hooks for subclasses.
	public void onEnable() {}

	public void onDisable() {}

	public void onTick() {}
}
