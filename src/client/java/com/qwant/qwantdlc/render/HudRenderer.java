package com.qwant.qwantdlc.render;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * On-screen HUD: watermark + active-modules list.
 * Drawn entirely with our Render2D (no vanilla rounded-rect helpers).
 */
public final class HudRenderer {
	public static final Identifier HUD_LAYER = Identifier.of(QwantDLC.MOD_ID, "hud");

	private HudRenderer() {}

	public static void register() {
		HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
			layeredDrawer.attachLayerAfter(IdentifiedLayer.MISC, HUD_LAYER, (ctx, tickCounter) -> {
				HudModule hud = findHudModule();
				if (hud == null || !hud.isToggled()) return;

				MinecraftClient mc = MinecraftClient.getInstance();
				if (mc.options.hudHidden) return;

				renderWatermark(ctx);
				renderModuleList(ctx);
			})
		);
	}

	private static HudModule findHudModule() {
		for (Module m : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (m instanceof HudModule h) return h;
		}
		return null;
	}

	private static void renderWatermark(net.minecraft.client.gui.DrawContext ctx) {
		var tr = MinecraftClient.getInstance().textRenderer;
		String text = QwantDLC.MOD_NAME;
		int textWidth = tr.getWidth(text);

		float x = 4f, y = 4f, padX = 6f, padY = 4f;
		float w = textWidth + padX * 2;
		float h = tr.fontHeight + padY * 2;

		Render2D.roundedRectWithOutline(
			ctx.getMatrices().peek().getPositionMatrix(),
			x, y, w, h, 4f,
			Theme.PANEL_BG, Theme.PANEL_BORDER
		);
		ctx.drawText(tr, text,
			(int) (x + padX),
			(int) (y + padY),
			Theme.TEXT_PRIMARY, false);
	}

	private static void renderModuleList(net.minecraft.client.gui.DrawContext ctx) {
		var mc = MinecraftClient.getInstance();
		var tr = mc.textRenderer;

		List<Module> active = ModuleManager.getInstance().getModules().stream()
			.filter(Module::isToggled)
			.toList();
		if (active.isEmpty()) return;

		int screenW = ctx.getScaledWindowWidth();
		float right = screenW - 4f;
		float y = 4f;

		for (Module m : active) {
			String text = m.getName();
			int tw = tr.getWidth(text);
			float padX = 5f, padY = 3f;
			float w = tw + padX * 2;
			float h = tr.fontHeight + padY * 2;
			float x = right - w;

			Render2D.roundedRectWithOutline(
				ctx.getMatrices().peek().getPositionMatrix(),
				x, y, w, h, 3f,
				Theme.PANEL_BG, Theme.PANEL_BORDER
			);
			ctx.drawText(tr, text,
				(int) (x + padX),
				(int) (y + padY),
				Theme.TEXT_ACCENT, false);

			y += h + 2f;
		}
	}
}
