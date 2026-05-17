package com.qwant.qwantdlc.render;

import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.render.hud.ArrayListWidget;
import com.qwant.qwantdlc.render.hud.HudUtil;
import com.qwant.qwantdlc.render.hud.InfoHudWidget;
import com.qwant.qwantdlc.render.hud.KeybindsWidget;
import com.qwant.qwantdlc.render.hud.PotionHudWidget;
import com.qwant.qwantdlc.render.hud.TargetHudWidget;
import com.qwant.qwantdlc.render.hud.WatermarkWidget;
import com.qwant.qwantdlc.render.hud.WelcomeWidget;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

import org.joml.Matrix4f;

/**
 * Top-level HUD callback. Delegates each widget to its dedicated class.
 *
 * Render order is important: panels first (so welcome/notifications float
 * above), then arraylist on top of any centered overlay, then welcome at
 * the very top.
 */
public final class HudRenderer {
	private static final WatermarkWidget watermark = new WatermarkWidget();
	private static final ArrayListWidget arrayList = new ArrayListWidget();
	private static final InfoHudWidget   infoHud   = new InfoHudWidget();
	private static final TargetHudWidget targetHud = new TargetHudWidget();
	private static final PotionHudWidget potionHud = new PotionHudWidget();
	private static final KeybindsWidget  keybinds  = new KeybindsWidget();
	private static final WelcomeWidget   welcome   = new WelcomeWidget();

	private HudRenderer() {}

	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			HudModule hud = HudUtil.findHud();
			if (hud == null || !hud.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.options.hudHidden) return;

			boolean inGame = mc.player != null && mc.world != null;
			welcome.update(inGame);

			Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();

			// Always-visible (even on title/menu screens):
			watermark.render(ctx, m);
			arrayList.render(ctx, m);

			// In-game widgets:
			if (inGame) {
				infoHud.render(ctx, m);
				targetHud.render(ctx, m);
				potionHud.render(ctx, m);
				keybinds.render(ctx, m);
				welcome.render(ctx, m);
			}
		});
	}
}
