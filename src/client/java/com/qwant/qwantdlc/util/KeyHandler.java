package com.qwant.qwantdlc.util;

import java.util.HashMap;
import java.util.Map;

import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

/**
 * Edge-detected global key listener. Toggles modules whose keybind matches
 * the just-pressed key. Skipped while a Screen is open so we don't fire
 * while the user is rebinding inside the ClickGUI.
 */
public final class KeyHandler {
	private static final Map<Integer, Boolean> previous = new HashMap<>();

	private KeyHandler() {}

	public static void onTick(MinecraftClient client) {
		if (client == null || client.getWindow() == null) return;
		if (client.currentScreen != null) return;

		long handle = client.getWindow().getHandle();
		for (Module m : ModuleManager.getInstance().getModules()) {
			int k = m.getKey();
			if (k <= 0 || k == GLFW.GLFW_KEY_UNKNOWN) continue;
			boolean nowPressed;
			try {
				nowPressed = InputUtil.isKeyPressed(handle, k);
			} catch (Throwable t) {
				continue;
			}
			boolean prevPressed = previous.getOrDefault(k, false);
			if (nowPressed && !prevPressed) {
				m.toggle();
			}
			previous.put(k, nowPressed);
		}
	}
}
