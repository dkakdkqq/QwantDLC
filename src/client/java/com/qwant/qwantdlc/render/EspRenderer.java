package com.qwant.qwantdlc.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.EspModule;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

/**
 * 3D ESP rendering: outline + tracer to other players.
 * All draw calls go through our Render3D class.
 */
public final class EspRenderer {
	private EspRenderer() {}

	public static void register() {
		WorldRenderEvents.LAST.register(context -> {
			EspModule esp = findEspModule();
			if (esp == null || !esp.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.world == null || mc.player == null) return;

			int color = 0xFF1F6FEB;

			for (Entity e : mc.world.getEntities()) {
				if (!(e instanceof PlayerEntity)) continue;
				if (e == mc.player) continue;
				if (e.isRemoved()) continue;

				Box bb = e.getBoundingBox();
				Render3D.outlineBox(context.matrixStack(), bb, color, 1.5f);
				Render3D.tracer(context.matrixStack(),
					bb.getCenter(),
					(color & 0x00FFFFFF) | 0x80000000,
					1.0f);
			}
		});
	}

	private static EspModule findEspModule() {
		for (Module m : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (m instanceof EspModule e) return e;
		}
		return null;
	}
}
