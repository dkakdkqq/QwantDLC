package com.qwant.qwantdlc.util;

import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

/** Human-readable names for GLFW key codes (used in HUD + GUI). */
public final class KeyNames {
	private KeyNames() {}

	public static String of(int key) {
		if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) return "—";
		try {
			InputUtil.Key k = InputUtil.fromKeyCode(key, 0);
			String s = k.getLocalizedText().getString();
			if (s == null || s.isEmpty()) return fallback(key);
			return s.toUpperCase();
		} catch (Throwable t) {
			return fallback(key);
		}
	}

	private static String fallback(int key) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
			return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
		}
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
			return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
		}
		return "K" + key;
	}
}
