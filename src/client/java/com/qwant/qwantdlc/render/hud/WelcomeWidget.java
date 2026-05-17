package com.qwant.qwantdlc.render.hud;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import org.joml.Matrix4f;

/**
 * Welcome popup that slides in once when the player joins a world. Auto-
 * hides after ~3.5 seconds.
 */
public final class WelcomeWidget {
	private final Animation anim = new Animation(450f, Easing.EASE_OUT_QUART, 0f);
	private long startMs = -1L;
	private boolean shown = false;

	public void update(boolean inGame) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (inGame && !shown && mc.currentScreen == null) {
			startMs = System.currentTimeMillis();
			shown = true;
		}
		if (!inGame) {
			shown = false;
			startMs = -1L;
			anim.setTargetBool(false);
		}
		if (startMs < 0) return;
		long elapsed = System.currentTimeMillis() - startMs;
		anim.setTargetBool(elapsed < 3500L);
	}

	public void render(DrawContext ctx, Matrix4f m) {
		HudModule hud = HudUtil.findHud();
		if (hud == null || !hud.isToggled()) return;
		if (hud.welcomePopup != null && !hud.welcomePopup.get()) return;

		float a = anim.update();
		if (a <= 0.01f) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer tr = mc.textRenderer;
		String name = mc.player != null ? mc.player.getGameProfile().getName() : "";
		String text = "Добро пожаловать, " + name;

		int textW = tr.getWidth(text);
		float padX = 14f, padY = 8f;
		float w = textW + padX * 2;
		float h = tr.fontHeight + padY * 2;

		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();

		float startY = -h - 4f;
		float endY   = sh * 0.18f;
		float y = startY + (endY - startY) * a;
		float x = (sw - w) / 2f;

		int accentA = HudUtil.accentA();
		int accentB = HudUtil.accentB();
		Render2D.glow(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB, 0.45f * a), 10f);
		Render2D.fillRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(0xEE111118, a));
		Render2D.fillGradientH(m, x + padX / 2f, y + h - 3f,
			w - padX, 1.5f,
			ColorUtil.withAlpha(accentA, a),
			ColorUtil.withAlpha(accentB, a));
		Render2D.strokeRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB, 0.5f * a));

		ctx.drawText(tr, text,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, a), false);

		String subtitle = QwantDLC.MOD_NAME + " готов к работе";
		int sw2 = tr.getWidth(subtitle);
		ctx.drawText(tr, subtitle,
			(int) (x + (w - sw2) / 2f),
			(int) (y + h + 4f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, a), false);
	}
}
