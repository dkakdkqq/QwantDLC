package com.qwant.qwantdlc.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.util.TargetTracker;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

/**
 * Minced-inspired HUD: dark rounded panels with a single purple accent and
 * smooth slide-in animations. Everything is drawn in this one file so there
 * are no inter-widget dependencies and no surprising state.
 *
 * Widgets:
 *   - Watermark (top-left): "Q | Qwant" with purple dot.
 *   - ArrayList (top-right): enabled modules, sorted by width, slide-in.
 *   - Info pill (bottom-left): "user | NN fps | x, y, z | b/s | ping".
 *   - TargetHUD (centre-bottom): last attacked entity with health bar.
 *   - PotionHUD (right of centre): active effects with duration.
 */
public final class HudRenderer {
	private HudRenderer() {}

	// === Palette ===========================================================
	private static final int ACCENT       = 0xFFB14CFF; // purple primary
	private static final int ACCENT_DARK  = 0xFF8A2BE2; // purple secondary
	private static final int PANEL_BG     = 0xCC0E0E14; // dark panel
	private static final int PANEL_STROKE = 0xFF2A2A33;
	private static final int TEXT_PRIMARY   = 0xFFFFFFFF;
	private static final int TEXT_SECONDARY = 0xFFB5B5BE;
	private static final int TEXT_MUTED     = 0xFF7A7A85;

	// === Smoothed numeric readouts ========================================
	private static final float SMOOTH = 0.15f;
	private static float fpsAnim, xAnim, yAnim, zAnim, speedAnim, pingAnim;

	// === Per-widget animations ============================================
	private static final Animation watermarkAnim =
		new Animation(420f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation infoAnim =
		new Animation(420f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation targetAnim =
		new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation potionAnim =
		new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Map<Module, Animation> arrayAnim = new HashMap<>();

	private static float targetHealthSmooth = 1f;

	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			HudModule hud = findHud();
			if (hud == null || !hud.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc == null || mc.options == null) return;
			if (mc.options.hudHidden) return;

			Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
			int sw = ctx.getScaledWindowWidth();
			int sh = ctx.getScaledWindowHeight();

			ClientPlayerEntity p = mc.player;
			if (p != null) {
				updateAnimations(p);
			}

			// Always-visible (works even on title screens).
			watermarkAnim.setTargetBool(hud.watermark.get());
			if (hud.watermark.get()) {
				drawWatermark(ctx, m, watermarkAnim.update());
			} else {
				watermarkAnim.update();
			}

			if (hud.arrayList.get()) {
				drawArrayList(ctx, m, sw);
			}

			// In-game widgets.
			if (p != null && mc.world != null) {
				infoAnim.setTargetBool(hud.infoPill.get());
				if (hud.infoPill.get()) {
					drawInfoPill(ctx, m, sh, infoAnim.update());
				} else {
					infoAnim.update();
				}

				if (hud.targetHud.get()) {
					drawTargetHud(ctx, m, sw, sh);
				}
				if (hud.potionHud.get()) {
					drawPotionHud(ctx, m, sw, sh);
				}
			}
		});
	}

	// ===================================================== shared utilities

	private static HudModule findHud() {
		for (Module mod : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (mod instanceof HudModule h) return h;
		}
		return null;
	}

	private static void updateAnimations(ClientPlayerEntity p) {
		MinecraftClient mc = MinecraftClient.getInstance();
		fpsAnim   = approach(fpsAnim,   mc.getCurrentFps());
		xAnim     = approach(xAnim,     (float) p.getX());
		yAnim     = approach(yAnim,     (float) p.getY());
		zAnim     = approach(zAnim,     (float) p.getZ());
		speedAnim = approach(speedAnim, computeSpeed(p));
		pingAnim  = approach(pingAnim,  computePing(p));
	}

	private static float approach(float current, float target) {
		return current + (target - current) * SMOOTH;
	}

	private static float computeSpeed(ClientPlayerEntity p) {
		Vec3d v = p.getVelocity();
		double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
		return (float) (horiz * 20.0);
	}

	private static int computePing(ClientPlayerEntity p) {
		try {
			if (p.networkHandler == null) return 0;
			PlayerListEntry entry = p.networkHandler.getPlayerListEntry(p.getUuid());
			if (entry == null) return 0;
			return Math.max(0, entry.getLatency());
		} catch (Throwable t) {
			return 0;
		}
	}

	/**
	 * Standard Minced-style rounded panel: glow + dark fill + thin stroke.
	 */
	private static void panel(Matrix4f m, float x, float y, float w, float h,
	                          float radius, float alpha) {
		Render2D.glow(m, x, y, w, h, radius,
			ColorUtil.withAlpha(ACCENT, 0.30f * alpha), 5f);
		Render2D.fillRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(PANEL_BG, alpha));
		Render2D.strokeRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(PANEL_STROKE, alpha));
	}

	// =========================================================== Watermark

	private static void drawWatermark(DrawContext ctx, Matrix4f m, float t) {
		if (t <= 0.005f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String brand = QwantDLC.MOD_NAME;
		String letter = "Q";
		int letterW = tr.getWidth(letter);
		int brandW = tr.getWidth(brand);
		float padX = 8f, padY = 5f, gap = 8f, dot = 5f;
		float w = padX + letterW + gap + dot + gap + brandW + padX;
		float h = tr.fontHeight + padY * 2;

		float slide = (1f - t) * -16f;
		float x = 6f + slide;
		float y = 6f;

		panel(m, x, y, w, h, 6f, t);

		float textY = y + (h - tr.fontHeight) / 2f + 1f;
		ctx.drawText(tr, letter, (int) (x + padX), (int) textY,
			ColorUtil.withAlpha(ACCENT, t), false);

		float dotX = x + padX + letterW + gap;
		float dotY = y + (h - dot) / 2f;
		Render2D.fillRoundedRect(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(ACCENT, t));

		ctx.drawText(tr, brand,
			(int) (dotX + dot + gap), (int) textY,
			ColorUtil.withAlpha(TEXT_PRIMARY, t), false);
	}

	// =========================================================== ArrayList

	private static void drawArrayList(DrawContext ctx, Matrix4f m, int sw) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		List<Module> all = ModuleManager.getInstance().getModules();
		// Wire up animations once per module.
		for (Module mod : all) {
			arrayAnim.computeIfAbsent(mod,
				k -> new Animation(280f, Easing.EASE_OUT_QUART, 0f));
		}
		// Drive each animation toward its current toggled state.
		for (Module mod : all) {
			arrayAnim.get(mod).setTargetBool(mod.isToggled());
		}

		// Sort by visible width, longest at top.
		List<Module> sorted = new ArrayList<>(all);
		sorted.sort(Comparator.comparingInt((Module mod) ->
			tr.getWidth(mod.getName())).reversed());

		float padX = 6f, padY = 3f;
		float y = 4f;

		for (Module mod : sorted) {
			Animation a = arrayAnim.get(mod);
			float t = a.update();
			if (t <= 0.005f) continue;

			String text = mod.getName();
			int textW = tr.getWidth(text);
			float w = textW + padX * 2;
			float h = tr.fontHeight + padY * 2;

			float slide = (1f - t) * (w + 12f);
			float x = sw - 4f - w + slide;

			Render2D.glow(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(ACCENT, 0.30f * t), 4f);
			Render2D.fillRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(PANEL_BG, t));
			Render2D.strokeRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(ACCENT, 0.40f * t));
			// Left accent stripe.
			Render2D.fillRoundedRect(m, x, y, 2f, h, 1f,
				ColorUtil.withAlpha(ACCENT, t));

			ctx.drawText(tr, text,
				(int) (x + padX), (int) (y + padY),
				ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

			y += (h + 2f) * t;
		}
	}

	// ============================================================ Info pill

	private static void drawInfoPill(DrawContext ctx, Matrix4f m, int sh, float t) {
		if (t <= 0.005f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) return;

		String user = p.getGameProfile().getName();
		String fps = Math.round(fpsAnim) + " fps";
		String xyz = String.format(Locale.ROOT, "%.0f, %.0f, %.0f",
			xAnim, yAnim, zAnim);
		String bps = String.format(Locale.ROOT, "%.1f b/s", speedAnim);
		String ping = Math.round(pingAnim) + " ms";

		String[] segments = { user, fps, xyz, bps, ping };
		String separator = "  •  ";
		int sepW = tr.getWidth(separator);

		int contentW = 0;
		for (int i = 0; i < segments.length; i++) {
			contentW += tr.getWidth(segments[i]);
			if (i < segments.length - 1) contentW += sepW;
		}

		float padX = 8f, padY = 5f;
		float w = contentW + padX * 2;
		float h = tr.fontHeight + padY * 2;
		float slideY = (1f - t) * 16f;
		float x = 6f;
		float y = sh - 6f - h + slideY;

		panel(m, x, y, w, h, 6f, t);

		float textX = x + padX;
		float textY = y + padY;
		for (int i = 0; i < segments.length; i++) {
			boolean primary = i == 0;
			ctx.drawText(tr, segments[i],
				(int) textX, (int) textY,
				ColorUtil.withAlpha(primary ? TEXT_PRIMARY : TEXT_SECONDARY, t),
				false);
			textX += tr.getWidth(segments[i]);
			if (i < segments.length - 1) {
				ctx.drawText(tr, separator,
					(int) textX, (int) textY,
					ColorUtil.withAlpha(TEXT_MUTED, t), false);
				textX += sepW;
			}
		}
	}

	// ============================================================ TargetHUD

	private static void drawTargetHud(DrawContext ctx, Matrix4f m, int sw, int sh) {
		LivingEntity target = TargetTracker.getTarget();
		targetAnim.setTargetBool(target != null);
		float t = targetAnim.update();
		if (t <= 0.01f || target == null) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String name = safeName(target);
		float maxHp = Math.max(1f, target.getMaxHealth());
		float hp    = Math.max(0f, target.getHealth());
		float ratio = Math.max(0f, Math.min(1f, hp / maxHp));
		targetHealthSmooth += (ratio - targetHealthSmooth) * 0.15f;

		String hpText = String.format(Locale.ROOT, "%.1f / %.1f", hp, maxHp);
		String distText = String.format(Locale.ROOT, "%.1fm", distanceToLocal(target));

		int contentW = Math.max(tr.getWidth(name), tr.getWidth(hpText));
		float padX = 10f, padY = 8f;
		float w = Math.max(120f, contentW + padX * 2 + tr.getWidth(distText) + 6f);
		float h = tr.fontHeight * 2 + padY * 2 + 6f;

		float x = sw / 2f - w / 2f - 110f;
		float y = sh / 2f + 60f + (1f - t) * 12f;

		int healthColor = healthAccent(ratio);

		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(healthColor, 0.40f * t), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(PANEL_BG, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(healthColor, 0.45f * t));

		ctx.drawText(tr, name,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

		int distW = tr.getWidth(distText);
		ctx.drawText(tr, distText,
			(int) (x + w - padX - distW), (int) (y + padY),
			ColorUtil.withAlpha(TEXT_MUTED, t), false);

		float barX = x + padX;
		float barY = y + padY + tr.fontHeight + 4f;
		float barW = w - padX * 2;
		float barH = 4f;

		Render2D.fillRoundedRect(m, barX, barY, barW, barH, 2f,
			ColorUtil.withAlpha(0xFF1A1A1F, t));
		float fillW = Math.max(0f, barW * targetHealthSmooth);
		if (fillW > 0.5f) {
			Render2D.fillGradientH(m, barX, barY, fillW, barH,
				ColorUtil.withAlpha(healthColor, t),
				ColorUtil.withAlpha(brighten(healthColor, 1.3f), t));
		}

		ctx.drawText(tr, hpText,
			(int) (x + padX), (int) (barY + barH + 4f),
			ColorUtil.withAlpha(TEXT_SECONDARY, t), false);
	}

	// ============================================================ PotionHUD

	private static void drawPotionHud(DrawContext ctx, Matrix4f m, int sw, int sh) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;

		List<StatusEffectInstance> effects = collectEffects(mc.player.getStatusEffects());
		potionAnim.setTargetBool(!effects.isEmpty());
		float t = potionAnim.update();
		if (t <= 0.01f || effects.isEmpty()) return;

		TextRenderer tr = mc.textRenderer;

		// Compute width from longest "Effect II  01:23" row.
		int contentW = 0;
		for (StatusEffectInstance eff : effects) {
			String label = effectLabel(eff);
			String dur   = formatDuration(eff);
			int rowW = tr.getWidth(label) + 12 + tr.getWidth(dur);
			contentW = Math.max(contentW, rowW);
		}
		float padX = 8f, padY = 6f;
		float rowH = tr.fontHeight + 4f;
		float w = contentW + padX * 2;
		float h = rowH * effects.size() + padY * 2;

		float x = sw / 2f + 60f + (1f - t) * 16f;
		float y = sh - 60f - h;

		panel(m, x, y, w, h, 6f, t);

		float ry = y + padY;
		for (StatusEffectInstance eff : effects) {
			int color = effectColor(eff);

			// Left stripe colored by the effect.
			Render2D.fillRoundedRect(m, x + padX - 4f, ry + 1f, 2f, rowH - 2f, 1f,
				ColorUtil.withAlpha(color, t));

			String label = effectLabel(eff);
			String dur   = formatDuration(eff);
			ctx.drawText(tr, label,
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

			int durW = tr.getWidth(dur);
			ctx.drawText(tr, dur,
				(int) (x + w - padX - durW), (int) ry,
				ColorUtil.withAlpha(TEXT_SECONDARY, t), false);

			ry += rowH;
		}
	}

	// ============================================================ helpers

	private static List<StatusEffectInstance> collectEffects(
			Collection<StatusEffectInstance> source) {
		List<StatusEffectInstance> out = new ArrayList<>();
		try {
			for (StatusEffectInstance eff : source) {
				out.add(eff);
			}
		} catch (Throwable ignored) {
			// Defensive: never let HUD crash on a bad effect collection.
		}
		return out;
	}

	private static String effectLabel(StatusEffectInstance eff) {
		try {
			RegistryEntry<StatusEffect> entry = eff.getEffectType();
			MutableText name = Text.translatable(entry.value().getTranslationKey());
			int amp = eff.getAmplifier();
			if (amp > 0) return name.getString() + " " + roman(amp + 1);
			return name.getString();
		} catch (Throwable t) {
			return "?";
		}
	}

	private static String formatDuration(StatusEffectInstance eff) {
		try {
			int ticks = eff.getDuration();
			if (ticks < 0) return "**:**";
			int sec = ticks / 20;
			return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
		} catch (Throwable t) {
			return "?";
		}
	}

	private static int effectColor(StatusEffectInstance eff) {
		try {
			int rgb = eff.getEffectType().value().getColor();
			return rgb | 0xFF000000;
		} catch (Throwable t) {
			return ACCENT;
		}
	}

	private static String roman(int n) {
		switch (n) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(n);
		}
	}

	private static int healthAccent(float ratio) {
		if (ratio >= 0.66f) return 0xFF4ADE80;
		if (ratio >= 0.33f) return 0xFFFACC15;
		return 0xFFEF4444;
	}

	private static int brighten(int color, float factor) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int) ((color & 0xFF) * factor));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static String safeName(LivingEntity ent) {
		try {
			if (ent.getDisplayName() != null) return ent.getDisplayName().getString();
			return ent.getName().getString();
		} catch (Throwable t) {
			return "?";
		}
	}

	private static float distanceToLocal(LivingEntity ent) {
		try {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player == null) return 0f;
			return mc.player.distanceTo(ent);
		} catch (Throwable t) {
			return 0f;
		}
	}
}
