package com.qwant.qwantdlc.render.hud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.modules.render.PotionHudModule;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import org.joml.Matrix4f;

/**
 * PotionHUD widget — list of active status effects with name + duration.
 * Sits above the hotbar, right of centre. Multiple style modes:
 *
 *   - Modern    — full panel with tile + name + duration column
 *   - Nursultan — slim panel with accent stripe per row
 *   - Minced    — compact rows with bracket-style duration
 *   - Nix       — outlined panel with chroma stroke
 */
public final class PotionHudWidget {
	private final Animation slide = new Animation(280f, Easing.EASE_OUT_QUART, 0f);

	public void render(DrawContext ctx, Matrix4f m) {
		PotionHudModule mod = HudUtil.findRender(PotionHudModule.class);
		if (mod == null || !mod.isToggled()) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;

		Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
		List<StatusEffectInstance> visible = filter(effects, mod);

		slide.setTargetBool(!visible.isEmpty());
		float t = slide.update();
		if (t <= 0.01f || visible.isEmpty()) return;

		String style = HudUtil.mode(mod.style, "Modern");
		switch (style) {
			case "Nursultan": drawNursultan(ctx, m, mod, visible, t); break;
			case "Minced":    drawMinced(ctx, m, mod, visible, t);    break;
			case "Nix":       drawNix(ctx, m, mod, visible, t);       break;
			case "Modern":
			default:          drawModern(ctx, m, mod, visible, t);    break;
		}
	}

	private List<StatusEffectInstance> filter(Collection<StatusEffectInstance> effects,
	                                          PotionHudModule mod) {
		List<StatusEffectInstance> out = new ArrayList<>();
		for (StatusEffectInstance eff : effects) {
			if (mod.hideAmbient != null && mod.hideAmbient.get() && eff.isAmbient()) {
				continue;
			}
			out.add(eff);
		}
		return out;
	}

	// ----- Common helpers --------------------------------------------------

	private String label(StatusEffectInstance eff, PotionHudModule mod) {
		try {
			RegistryEntry<StatusEffect> entry = eff.getEffectType();
			MutableText name = Text.translatable(entry.value().getTranslationKey());
			if (mod.showAmplifier != null && mod.showAmplifier.get()) {
				return name.getString() + " " + HudUtil.roman(eff.getAmplifier() + 1);
			}
			return name.getString();
		} catch (Throwable t) {
			return "?";
		}
	}

	private String duration(StatusEffectInstance eff) {
		try {
			Text d = StatusEffectUtil.getDurationText(eff, 1f, 20f);
			return d.getString();
		} catch (Throwable t) {
			int sec = eff.getDuration() / 20;
			return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
		}
	}

	// ----- Modern (default) ------------------------------------------------

	private void drawModern(DrawContext ctx, Matrix4f m, PotionHudModule mod,
	                        List<StatusEffectInstance> effects, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		boolean tile = mod.showTile != null && mod.showTile.get();
		boolean dur  = mod.showDuration != null && mod.showDuration.get();
		float tileSize = 12f;

		int contentW = 0;
		for (StatusEffectInstance eff : effects) {
			contentW = Math.max(contentW, tr.getWidth(label(eff, mod)));
		}
		float padX = 8f, padY = 6f, durCol = 30f;
		float rowH = tr.fontHeight + 5f;
		float w = contentW + padX * 2 + (tile ? tileSize + 6f : 0f) + (dur ? durCol : 0f);
		float h = rowH * effects.size() + padY * 2;

		float x = (ctx.getScaledWindowWidth() + 100f) / 2f + (1f - t) * 20f;
		float y = ctx.getScaledWindowHeight() - 60f - h;

		int accent = HudUtil.accentA();
		HudUtil.panel(m, x, y, w, h, 8f, accent, t);

		float ry = y + padY;
		for (StatusEffectInstance eff : effects) {
			float rx = x + padX;
			if (tile) {
				HudUtil.drawEffectTile(m, eff, rx, ry, tileSize, t);
				rx += tileSize + 6f;
			} else {
				Render2D.fillRoundedRect(m, x + padX - 4f, ry + 1f, 2f, rowH - 2f, 1f,
					ColorUtil.withAlpha(HudUtil.effectColor(eff), t));
			}

			ctx.drawText(tr, label(eff, mod),
				(int) rx, (int) (ry + 1f),
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);

			if (dur) {
				String d = duration(eff);
				int dw = tr.getWidth(d);
				ctx.drawText(tr, d,
					(int) (x + w - padX - dw),
					(int) (ry + 1f),
					ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
			}
			ry += rowH;
		}
	}

	// ----- Nursultan -------------------------------------------------------

	private void drawNursultan(DrawContext ctx, Matrix4f m, PotionHudModule mod,
	                           List<StatusEffectInstance> effects, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		boolean dur = mod.showDuration != null && mod.showDuration.get();
		int contentW = 0;
		for (StatusEffectInstance eff : effects) {
			contentW = Math.max(contentW, tr.getWidth(label(eff, mod)));
		}

		float padX = 10f, padY = 6f, durCol = 32f;
		float rowH = tr.fontHeight + 6f;
		float w = contentW + padX * 2 + 6f + (dur ? durCol : 0f);
		float h = rowH * effects.size() + padY * 2;

		float x = (ctx.getScaledWindowWidth() + 100f) / 2f + (1f - t) * 20f;
		float y = ctx.getScaledWindowHeight() - 60f - h;

		int accentA = HudUtil.accentA();
		int accentB = HudUtil.accentB();
		HudUtil.panel(m, x, y, w, h, 10f, accentA, t);
		Render2D.fillGradientH(m, x + 2f, y + 4f, 3f, h - 8f,
			ColorUtil.withAlpha(accentA, t),
			ColorUtil.withAlpha(accentB, t));

		float ry = y + padY;
		for (StatusEffectInstance eff : effects) {
			int color = HudUtil.effectColor(eff);
			Render2D.fillRect(m, x + padX, ry + rowH - 2f,
				w - padX * 2, 1f,
				ColorUtil.withAlpha(color, 0.35f * t));

			ctx.drawText(tr, label(eff, mod),
				(int) (x + padX + 4f), (int) (ry + 2f),
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);

			if (dur) {
				String d = duration(eff);
				int dw = tr.getWidth(d);
				ctx.drawText(tr, d,
					(int) (x + w - padX - dw),
					(int) (ry + 2f),
					ColorUtil.withAlpha(color, t), false);
			}
			ry += rowH;
		}
	}

	// ----- Minced ----------------------------------------------------------

	private void drawMinced(DrawContext ctx, Matrix4f m, PotionHudModule mod,
	                        List<StatusEffectInstance> effects, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		// Plain text rows: "Speed II [01:23]" with effect color.
		int contentW = 0;
		boolean dur = mod.showDuration != null && mod.showDuration.get();
		for (StatusEffectInstance eff : effects) {
			String row = label(eff, mod) + (dur ? "  [" + duration(eff) + "]" : "");
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float rowH = tr.fontHeight + 1f;
		float w = contentW + 8f;
		float h = rowH * effects.size();

		float x = (ctx.getScaledWindowWidth() + 100f) / 2f + (1f - t) * 20f;
		float y = ctx.getScaledWindowHeight() - 60f - h;

		float ry = y;
		for (StatusEffectInstance eff : effects) {
			int color = HudUtil.effectColor(eff);
			String main = label(eff, mod);
			ctx.drawText(tr, main,
				(int) x, (int) ry,
				ColorUtil.withAlpha(color, t), true);
			if (dur) {
				String d = "[" + duration(eff) + "]";
				ctx.drawText(tr, d,
					(int) (x + tr.getWidth(main) + 4),
					(int) ry,
					ColorUtil.withAlpha(0xFFB0B0B5, t), true);
			}
			ry += rowH;
		}
	}

	// ----- Nix -------------------------------------------------------------

	private void drawNix(DrawContext ctx, Matrix4f m, PotionHudModule mod,
	                     List<StatusEffectInstance> effects, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		boolean dur = mod.showDuration != null && mod.showDuration.get();
		int contentW = 0;
		for (StatusEffectInstance eff : effects) {
			contentW = Math.max(contentW, tr.getWidth(label(eff, mod)));
		}

		float padX = 8f, padY = 6f, durCol = 30f;
		float rowH = tr.fontHeight + 4f;
		float w = contentW + padX * 2 + (dur ? durCol : 0f);
		float h = rowH * effects.size() + padY * 2;

		float x = (ctx.getScaledWindowWidth() + 100f) / 2f + (1f - t) * 20f;
		float y = ctx.getScaledWindowHeight() - 60f - h;

		int accent = HudUtil.accentA();
		Render2D.fillRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(0xCC0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(accent, t));

		float ry = y + padY;
		for (StatusEffectInstance eff : effects) {
			ctx.drawText(tr, label(eff, mod),
				(int) (x + padX), (int) (ry + 1f),
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			if (dur) {
				String d = duration(eff);
				int dw = tr.getWidth(d);
				ctx.drawText(tr, d,
					(int) (x + w - padX - dw),
					(int) (ry + 1f),
					ColorUtil.withAlpha(HudUtil.effectColor(eff), t), false);
			}
			ry += rowH;
		}
	}
}
