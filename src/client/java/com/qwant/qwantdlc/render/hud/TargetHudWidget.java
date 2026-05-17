package com.qwant.qwantdlc.render.hud;

import java.util.Locale;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.modules.render.TargetHudModule;
import com.qwant.qwantdlc.render.Render2D;
import com.qwant.qwantdlc.util.TargetTracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import org.joml.Matrix4f;

/**
 * TargetHUD widget — displays the last attacked living entity. Switches
 * between four visual styles inspired by Minced, Nursultan, Nix and a
 * generic modern look.
 */
public final class TargetHudWidget {
	private final Animation slide  = new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private float healthSmooth = 1f;

	public void render(DrawContext ctx, Matrix4f rootMatrix) {
		TargetHudModule mod = HudUtil.findRender(TargetHudModule.class);
		if (mod == null || !mod.isToggled()) return;

		LivingEntity target = TargetTracker.getTarget();
		slide.setTargetBool(target != null);
		float t = slide.update();
		if (t <= 0.01f || target == null) return;

		// Smooth health transitions.
		float maxHp = Math.max(1f, target.getMaxHealth());
		float hp    = Math.max(0f, target.getHealth());
		float ratio = Math.max(0f, Math.min(1f, hp / maxHp));
		healthSmooth += (ratio - healthSmooth) * 0.12f;

		// Apply user scale via the matrix stack so all 4 styles inherit it.
		float scale = mod.scale != null ? mod.scale.get() : 1f;
		MatrixStack ms = ctx.getMatrices();
		ms.push();
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float cx = sw / 2f - 110f;
		float cy = sh / 2f + 60f;
		ms.translate(cx, cy, 0f);
		ms.scale(scale, scale, 1f);
		ms.translate(-cx, -cy, 0f);

		Matrix4f m = ms.peek().getPositionMatrix();

		String style = HudUtil.mode(mod.style, "Modern");
		switch (style) {
			case "Nursultan": drawNursultan(ctx, m, mod, target, hp, maxHp, ratio, t); break;
			case "Minced":    drawMinced(ctx, m, mod, target, hp, maxHp, ratio, t);    break;
			case "Nix":       drawNix(ctx, m, mod, target, hp, maxHp, ratio, t);       break;
			case "Modern":
			default:          drawModern(ctx, m, mod, target, hp, maxHp, ratio, t);    break;
		}

		ms.pop();
	}

	private String hpStr(float hp, float maxHp) {
		return String.format(Locale.ROOT, "%.1f / %.1f", hp, maxHp);
	}

	private String distStr(LivingEntity target) {
		float d = HudUtil.distanceTo(target);
		return String.format(Locale.ROOT, "%.1fm", d);
	}

	private float anchorX(DrawContext ctx, float w) {
		return (ctx.getScaledWindowWidth() - w) / 2f - 110f;
	}

	private float anchorY(DrawContext ctx, float h) {
		return (ctx.getScaledWindowHeight() - h) / 2f + 60f;
	}

	// ---------- Modern (default) -------------------------------------------

	private void drawModern(DrawContext ctx, Matrix4f m, TargetHudModule mod,
	                        LivingEntity target, float hp, float maxHp,
	                        float ratio, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String name = HudUtil.safeName(target);
		String hpText = hpStr(hp, maxHp);

		boolean avatar = mod.showAvatar != null && mod.showAvatar.get();
		float avatarSize = 22f;
		float padX = 10f, padY = 8f;
		int contentW = Math.max(tr.getWidth(name), tr.getWidth(hpText));
		float w = Math.max(120f, contentW + padX * 2 + (avatar ? avatarSize + 8f : 0f));
		float h = tr.fontHeight * 2 + padY * 2 + 6f;

		float x = anchorX(ctx, w);
		float y = anchorY(ctx, h) + (1f - t) * 12f;

		int accent = HudUtil.healthAccent(ratio);
		HudUtil.panel(m, x, y, w, h, 8f, accent, t);

		float textX = x + padX;
		if (avatar) {
			HudUtil.drawEntityAvatar(ctx, m, target,
				textX, y + (h - avatarSize) / 2f, avatarSize, t);
			textX += avatarSize + 8f;
		}

		ctx.drawText(tr, name, (int) textX, (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);

		if (mod.showDistance != null && mod.showDistance.get()) {
			String d = distStr(target);
			int dw = tr.getWidth(d);
			ctx.drawText(tr, d,
				(int) (x + w - padX - dw),
				(int) (y + padY),
				ColorUtil.withAlpha(Theme.TEXT_MUTED, t), false);
		}

		float barX = textX;
		float barY = y + padY + tr.fontHeight + 4f;
		float barW = x + w - padX - barX;
		float barH = 4f;

		Render2D.fillRoundedRect(m, barX, barY, barW, barH, 2f,
			ColorUtil.withAlpha(0xFF1A1A1F, t));
		float fillW = Math.max(0f, barW * healthSmooth);
		if (fillW > 0.5f) {
			Render2D.fillGradientH(m, barX, barY, fillW, barH,
				ColorUtil.withAlpha(accent, t),
				ColorUtil.withAlpha(HudUtil.brighten(accent, 1.3f), t));
		}

		float ly = barY + barH + 4f;
		if (mod.showHealthText != null && mod.showHealthText.get()) {
			ctx.drawText(tr, hpText,
				(int) textX, (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
		}
		if (mod.showArmor != null && mod.showArmor.get()) {
			String armor = "Armor: " + HudUtil.armorOf(target);
			int aw = tr.getWidth(armor);
			ctx.drawText(tr, armor,
				(int) (x + w - padX - aw), (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_MUTED, t), false);
		}
	}

	// ---------- Nursultan --------------------------------------------------

	private void drawNursultan(DrawContext ctx, Matrix4f m, TargetHudModule mod,
	                           LivingEntity target, float hp, float maxHp,
	                           float ratio, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String name = HudUtil.safeName(target);
		String hpText = hpStr(hp, maxHp);

		float avatarSize = 28f;
		float padX = 10f, padY = 8f;
		int contentW = Math.max(tr.getWidth(name), tr.getWidth(hpText));
		float w = Math.max(150f, contentW + padX * 2 + avatarSize + 14f);
		float h = avatarSize + padY * 2;

		float x = anchorX(ctx, w);
		float y = anchorY(ctx, h) + (1f - t) * 12f;

		int accentA = HudUtil.accentA();
		int accentB = HudUtil.accentB();
		HudUtil.panel(m, x, y, w, h, 10f, accentA, t);

		// Accent stripe down the left edge.
		Render2D.fillGradientH(m, x + 2f, y + 4f, 3f, h - 8f,
			ColorUtil.withAlpha(accentA, t),
			ColorUtil.withAlpha(accentB, t));

		float ax = x + padX;
		float ay = y + (h - avatarSize) / 2f;
		HudUtil.drawEntityAvatar(ctx, m, target, ax, ay, avatarSize, t);

		float textX = ax + avatarSize + 8f;
		ctx.drawText(tr, name, (int) textX, (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);

		// Health bar.
		float barX = textX;
		float barY = y + padY + tr.fontHeight + 4f;
		float barW = x + w - padX - barX;
		float barH = 5f;
		HudUtil.gradientBar(m, barX, barY, barW, barH, 2f,
			HudUtil.healthAccent(ratio),
			HudUtil.brighten(HudUtil.healthAccent(ratio), 1.3f),
			t);

		// Mask remaining (overlay) — clip via dark fill on the right.
		float used = Math.max(0f, Math.min(1f, healthSmooth));
		float maskW = Math.max(0f, (barW - 2f) * (1f - used));
		if (maskW > 0.5f) {
			Render2D.fillRoundedRect(m,
				barX + 1f + (barW - 2f - maskW), barY + 1f,
				maskW, barH - 2f, 1f,
				ColorUtil.withAlpha(0xFF1A1A1F, t));
		}

		// Bottom strip: HP / Distance / Armor.
		String bottomL = "";
		if (mod.showHealthText != null && mod.showHealthText.get()) bottomL = hpText;
		String bottomR = "";
		if (mod.showDistance != null && mod.showDistance.get()) bottomR = distStr(target);
		if (mod.showArmor != null && mod.showArmor.get()) {
			String a = "AP " + HudUtil.armorOf(target);
			bottomR = bottomR.isEmpty() ? a : (bottomR + "  " + a);
		}
		float ly = barY + barH + 3f;
		if (!bottomL.isEmpty()) {
			ctx.drawText(tr, bottomL,
				(int) textX, (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
		}
		if (!bottomR.isEmpty()) {
			int rw = tr.getWidth(bottomR);
			ctx.drawText(tr, bottomR,
				(int) (x + w - padX - rw), (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_MUTED, t), false);
		}
	}

	// ---------- Minced -----------------------------------------------------

	private void drawMinced(DrawContext ctx, Matrix4f m, TargetHudModule mod,
	                        LivingEntity target, float hp, float maxHp,
	                        float ratio, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String name = HudUtil.safeName(target);
		String hpText = String.format(Locale.ROOT, "%.0f", hp);

		float padX = 10f, padY = 6f;
		int nameW = tr.getWidth(name);
		int hpW   = tr.getWidth(hpText);
		float gap = 8f;
		float w = padX + nameW + gap + hpW + padX;
		float h = tr.fontHeight + padY * 2;

		float x = anchorX(ctx, w);
		float y = anchorY(ctx, h) + (1f - t) * 12f;

		int accent = HudUtil.healthAccent(ratio);
		Render2D.glow(m, x, y, w, h, h / 2f,
			ColorUtil.withAlpha(accent, 0.35f * t), 4f);
		Render2D.fillRoundedRect(m, x, y, w, h, h / 2f,
			ColorUtil.withAlpha(0xEE111118, t));

		ctx.drawText(tr, name,
			(int) (x + padX),
			(int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);
		ctx.drawText(tr, hpText,
			(int) (x + padX + nameW + gap),
			(int) (y + padY),
			ColorUtil.withAlpha(accent, t), false);

		// Underline progress bar.
		float bx = x + padX;
		float bw = w - padX * 2;
		Render2D.fillRect(m, bx, y + h - 2f, bw, 1f,
			ColorUtil.withAlpha(0xFF1A1A1F, t));
		Render2D.fillRect(m, bx, y + h - 2f, bw * healthSmooth, 1f,
			ColorUtil.withAlpha(accent, t));
	}

	// ---------- Nix --------------------------------------------------------

	private void drawNix(DrawContext ctx, Matrix4f m, TargetHudModule mod,
	                     LivingEntity target, float hp, float maxHp,
	                     float ratio, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String name = HudUtil.safeName(target);
		String hpText = hpStr(hp, maxHp);

		float padX = 10f, padY = 8f;
		int contentW = Math.max(tr.getWidth(name), tr.getWidth(hpText));
		float w = Math.max(120f, contentW + padX * 2);
		float h = tr.fontHeight * 2 + padY * 2 + 6f;

		float x = anchorX(ctx, w);
		float y = anchorY(ctx, h) + (1f - t) * 12f;

		int accent = HudUtil.accentA();
		// Outlined card, no glow.
		Render2D.fillRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(0xCC0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(accent, t));

		ctx.drawText(tr, name,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);

		// Simple horizontal bar at the bottom.
		float bx = x + padX;
		float by = y + padY + tr.fontHeight + 4f;
		float bw = w - padX * 2;
		float bh = 3f;
		Render2D.fillRect(m, bx, by, bw, bh,
			ColorUtil.withAlpha(0xFF1A1A1F, t));
		Render2D.fillRect(m, bx, by, bw * healthSmooth, bh,
			ColorUtil.withAlpha(HudUtil.healthAccent(ratio), t));

		float ly = by + bh + 4f;
		if (mod.showHealthText != null && mod.showHealthText.get()) {
			ctx.drawText(tr, hpText,
				(int) bx, (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
		}
		if (mod.showDistance != null && mod.showDistance.get()) {
			String d = distStr(target);
			int dw = tr.getWidth(d);
			ctx.drawText(tr, d,
				(int) (x + w - padX - dw), (int) ly,
				ColorUtil.withAlpha(Theme.TEXT_MUTED, t), false);
		}
	}
}
