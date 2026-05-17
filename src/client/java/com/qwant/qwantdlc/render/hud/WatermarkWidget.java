package com.qwant.qwantdlc.render.hud;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import org.joml.Matrix4f;

/**
 * Watermark widget — top-left brand pill. Multiple style modes:
 *
 *   - Pill      — chroma dot + brand + version, soft glow (default)
 *   - Bracket   — "[ Qwant ]" Minced/Nix style, no panel
 *   - Minimal   — plain "Qwant" text with accent gradient
 *   - Nursultan — wide accent stripe + brand + version separator
 */
public final class WatermarkWidget {
	private final Animation slide = new Animation(700f, Easing.EASE_OUT_BACK, 0f);

	public void render(DrawContext ctx, Matrix4f m) {
		HudModule hud = HudUtil.findHud();
		if (hud == null || !hud.isToggled()) return;
		if (hud.watermark != null && !hud.watermark.get()) return;

		slide.setTargetBool(true);
		float anim = slide.update();
		if (anim <= 0.001f) return;

		String mode = HudUtil.mode(hud.watermarkMode, "Pill");
		switch (mode) {
			case "Bracket":   drawBracket(ctx, m, anim);   break;
			case "Minimal":   drawMinimal(ctx, m, anim);   break;
			case "Nursultan": drawNursultan(ctx, m, anim); break;
			case "Pill":
			default:          drawPill(ctx, m, anim);      break;
		}
	}

	public float getAnim() {
		return slide.getValue();
	}

	// ---------- Pill (default) ---------------------------------------------

	private void drawPill(DrawContext ctx, Matrix4f m, float anim) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String brand = QwantDLC.MOD_NAME;
		String tag   = "v1.0";
		int brandW = tr.getWidth(brand);
		int tagW   = tr.getWidth(tag);

		float padX = 8f, padY = 5f, gap = 6f, dot = 6f;
		float w = padX + dot + gap + brandW + 6f + tagW + padX;
		float h = tr.fontHeight + padY * 2;
		float x = 6f + (1f - anim) * -16f;
		float y = 6f;

		int accent = HudUtil.accentA();
		HudUtil.panel(m, x, y, w, h, 8f, accent, anim);

		float dotX = x + padX;
		float dotY = y + (h - dot) / 2f;
		Render2D.fillRoundedRect(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(accent, anim));
		Render2D.glow(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(accent, 0.6f * anim), 3f);

		float textY = y + (h - tr.fontHeight) / 2f + 1f;
		float textX = dotX + dot + gap;
		ctx.drawText(tr, brand, (int) textX, (int) textY,
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);
		ctx.drawText(tr, tag, (int) (textX + brandW + 6f), (int) textY,
			ColorUtil.withAlpha(0xFFB0B0B5, anim), false);
	}

	// ---------- Bracket (Minced / Nix) -------------------------------------

	private void drawBracket(DrawContext ctx, Matrix4f m, float anim) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		int accent = HudUtil.accentA();
		String left = "[";
		String right = "]";
		String brand = QwantDLC.MOD_NAME;

		float x = 6f + (1f - anim) * -16f;
		float y = 6f;
		ctx.drawText(tr, left, (int) x, (int) y,
			ColorUtil.withAlpha(accent, anim), true);
		x += tr.getWidth(left) + 2f;
		ctx.drawText(tr, brand, (int) x, (int) y,
			ColorUtil.withAlpha(0xFFFFFFFF, anim), true);
		x += tr.getWidth(brand) + 2f;
		ctx.drawText(tr, right, (int) x, (int) y,
			ColorUtil.withAlpha(accent, anim), true);
	}

	// ---------- Minimal ----------------------------------------------------

	private void drawMinimal(DrawContext ctx, Matrix4f m, float anim) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String brand = QwantDLC.MOD_NAME;
		float x = 6f + (1f - anim) * -16f;
		float y = 6f;
		ctx.drawText(tr, brand, (int) x, (int) y,
			ColorUtil.withAlpha(0xFFFFFFFF, anim), true);
		// Underline gradient.
		Render2D.fillGradientH(m, x, y + tr.fontHeight + 1f,
			tr.getWidth(brand), 1f,
			ColorUtil.withAlpha(HudUtil.accentA(), anim),
			ColorUtil.withAlpha(HudUtil.accentB(), anim));
	}

	// ---------- Nursultan --------------------------------------------------

	private void drawNursultan(DrawContext ctx, Matrix4f m, float anim) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String brand = QwantDLC.MOD_NAME;
		String tag   = "v1.0";
		int brandW = tr.getWidth(brand);
		int tagW   = tr.getWidth(tag);

		float padX = 10f, padY = 6f, sep = 6f;
		float stripeW = 3f;
		float w = padX + stripeW + sep + brandW + 8f + 1f + 8f + tagW + padX;
		float h = tr.fontHeight + padY * 2;
		float x = 6f + (1f - anim) * -20f;
		float y = 6f;

		int accentA = HudUtil.accentA();
		int accentB = HudUtil.accentB();

		Render2D.glow(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(accentA, 0.35f * anim), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(0xEE0E0E14, anim));

		// Accent stripe (gradient bar) on the left edge.
		Render2D.fillGradientH(m, x + padX, y + 4f, stripeW, h - 8f,
			ColorUtil.withAlpha(accentA, anim),
			ColorUtil.withAlpha(accentB, anim));

		float textY = y + (h - tr.fontHeight) / 2f + 1f;
		float textX = x + padX + stripeW + sep;
		ctx.drawText(tr, brand, (int) textX, (int) textY,
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);
		// Vertical separator bar.
		Render2D.fillRect(m, textX + brandW + 8f, y + 4f, 1f, h - 8f,
			ColorUtil.withAlpha(0xFF2A2A33, anim));
		ctx.drawText(tr, tag, (int) (textX + brandW + 8f + 1f + 8f), (int) textY,
			ColorUtil.withAlpha(0xFFB0B0B5, anim), false);
	}
}
