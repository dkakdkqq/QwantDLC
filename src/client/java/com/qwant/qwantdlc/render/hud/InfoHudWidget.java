package com.qwant.qwantdlc.render.hud;

import java.util.Locale;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.modules.render.HudModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import org.joml.Matrix4f;

/**
 * Bottom-left info pill: username, FPS sample, XYZ. Cached FPS reading to
 * avoid jitter (samples every ~250 ms).
 */
public final class InfoHudWidget {
	private final Animation slide = new Animation(420f, Easing.EASE_OUT_QUART, 0f);

	private long lastFpsSampleMs = 0L;
	private int  lastFps = 0;

	public void render(DrawContext ctx, Matrix4f m) {
		HudModule hud = HudUtil.findHud();
		if (hud == null || !hud.isToggled()) return;
		if (hud.infoPill != null && !hud.infoPill.get()) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) return;

		slide.setTargetBool(true);
		float anim = slide.update();
		if (anim <= 0.01f) return;

		TextRenderer tr = mc.textRenderer;
		String user = p.getGameProfile().getName();
		int fps = sampleFps();
		String coords = String.format(Locale.ROOT, "%.0f, %.0f, %.0f",
			p.getX(), p.getY(), p.getZ());

		String line1 = user + "  •  " + fps + " FPS";
		String line2 = "XYZ: " + coords;
		int w1 = tr.getWidth(line1), w2 = tr.getWidth(line2);
		int contentW = Math.max(w1, w2);
		float padX = 8f, padY = 5f;
		float w = contentW + padX * 2;
		float h = tr.fontHeight * 2 + padY * 2 + 2f;

		int sh = ctx.getScaledWindowHeight();
		float slideY = (1f - anim) * 16f;
		float x = 6f;
		float y = sh - 6f - h + slideY;

		int accent = HudUtil.accentA();
		HudUtil.panel(m, x, y, w, h, 8f, accent, anim);

		ctx.drawText(tr, line1,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);
		ctx.drawText(tr, line2,
			(int) (x + padX), (int) (y + padY + tr.fontHeight + 2f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, anim), false);
	}

	private int sampleFps() {
		long now = System.currentTimeMillis();
		if (now - lastFpsSampleMs > 250L) {
			lastFps = MinecraftClient.getInstance().getCurrentFps();
			lastFpsSampleMs = now;
		}
		return lastFps;
	}
}
