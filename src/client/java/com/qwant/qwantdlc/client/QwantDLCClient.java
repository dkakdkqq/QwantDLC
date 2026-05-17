package com.qwant.qwantdlc.client;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.gui.ClickGuiScreen;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.render.EspRenderer;
import com.qwant.qwantdlc.render.HudRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

public class QwantDLCClient implements ClientModInitializer {
	public static KeyBinding openGuiKey;

	@Override
	public void onInitializeClient() {
		QwantDLC.LOGGER.info("[{}] Initializing client entrypoint...", QwantDLC.MOD_NAME);

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.qwantdlc.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			"category.qwantdlc.main"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new ClickGuiScreen());
				}
			}
			ModuleManager.getInstance().onTick();
		});

		HudRenderer.register();
		EspRenderer.register();

		QwantDLC.LOGGER.info("[{}] Client ready.", QwantDLC.MOD_NAME);
	}
}
