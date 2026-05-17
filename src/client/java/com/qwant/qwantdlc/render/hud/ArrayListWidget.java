package com.qwant.qwantdlc.render.hud;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import org.joml.Matrix4f;

/**
 * ArrayList widget — top-right list of enabled modules. Multiple style
 * modes:
 *
 *   - Modern    — pill rows with accent stripe + glow (default)
 *   - Nursultan — solid filled accent rows, white text
 *   - Minced    — text-only with gradient color, no panel
 *   - Nix       — outlined chips with chroma stroke
 *   - Outline   — text + outline, transparent background
 *   - Bracket   — "Module" with an accent dot prefix, text-only
 */
public final class ArrayListWidget {
	private final Map<Module, Animation> animations = new HashMap<>();

	public void render(DrawContext ctx, Matrix4f m) {
		HudModule hud = HudUtil.findHud();
		if (hud == null || !hud.isToggled()) return;
		if (hud.arrayList != null && !hud.arrayList.get()) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		List<Module> all = ModuleManager.getInstance().getModules();

		// Drive animations toward each module's toggle state.
		for (Module mod : all) {
			animations.computeIfAbsent(mod,
				k -> new Animation(280f, Easing.EASE_OUT_QUART, 0f));
		}
		for (Module mod : all) {
			animations.get(mod).setTargetBool(mod.isToggled());
		}

		// Sort.
		String sortMode = HudUtil.mode(hud.arrayListSort, "Width");
		Comparator<Module> cmp = switch (sortMode) {
			case "Alphabetical" -> Comparator.comparing(Module::getName);
			case "Length"       -> Comparator.comparingInt((Module mod) -> mod.getName().length()).reversed();
			case "Width"        -> Comparator.comparingInt((Module mod) -> tr.getWidth(mod.getName())).reversed();
			default             -> Comparator.comparingInt((Module mod) -> tr.getWidth(mod.getName())).reversed();
		};
		all = all.stream().sorted(cmp).toList();

		String mode = HudUtil.mode(hud.arrayListMode, "Modern");
		int screenW = ctx.getScaledWindowWidth();
		float y = 4f;

		for (Module mod : all) {
			Animation anim = animations.get(mod);
			float t = anim.update();
			if (t <= 0.005f) continue;

			float consumed = drawRow(ctx, m, mod, screenW, y, t, mode);
			y += consumed;
		}
	}

	private float drawRow(DrawContext ctx, Matrix4f m, Module mod,
	                      int screenW, float y, float t, String mode) {
		switch (mode) {
			case "Nursultan": return drawNursultan(ctx, m, mod, screenW, y, t);
			case "Minced":    return drawMinced(ctx, m, mod, screenW, y, t);
			case "Nix":       return drawNix(ctx, m, mod, screenW, y, t);
			case "Outline":   return drawOutline(ctx, m, mod, screenW, y, t);
			case "Bracket":   return drawBracket(ctx, m, mod, screenW, y, t);
			case "Modern":
			default:          return drawModern(ctx, m, mod, screenW, y, t);
		}
	}

	// ---------- Modern (default) -------------------------------------------

	private float drawModern(DrawContext ctx, Matrix4f m, Module mod,
	                         int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String text = mod.getName();
		int textW = tr.getWidth(text);
		float padX = 6f, padY = 3f;
		float w = textW + padX * 2;
		float h = tr.fontHeight + padY * 2;

		float slide = (1f - t) * (w + 12f);
		float x = screenW - 4f - w + slide;

		int accent = HudUtil.accentColorFor(mod);
		Render2D.glow(m, x, y, w, h, 4f,
			ColorUtil.withAlpha(accent, 0.45f * t), 4f);
		Render2D.fillRoundedRect(m, x, y, w, h, 4f,
			ColorUtil.withAlpha(0xEE0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 4f,
			ColorUtil.withAlpha(accent, 0.35f * t));
		Render2D.fillRoundedRect(m, x, y, 2f, h, 1f,
			ColorUtil.withAlpha(accent, t));

		ctx.drawText(tr, text,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);
		return (h + 2f) * t;
	}

	// ---------- Nursultan --------------------------------------------------

	private float drawNursultan(DrawContext ctx, Matrix4f m, Module mod,
	                            int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String text = mod.getName();
		int textW = tr.getWidth(text);
		float padX = 6f, padY = 3f;
		float w = textW + padX * 2 + 4f;
		float h = tr.fontHeight + padY * 2;

		float slide = (1f - t) * (w + 12f);
		float x = screenW - w + slide;

		int accentA = HudUtil.accentColorFor(mod);
		int accentB = HudUtil.brighten(accentA, 1.3f);

		Render2D.fillGradientH(m, x, y, w, h,
			ColorUtil.withAlpha(accentA, t),
			ColorUtil.withAlpha(accentB, t));

		ctx.drawText(tr, text,
			(int) (x + padX + 2f), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), true);
		return (h + 1f) * t;
	}

	// ---------- Minced -----------------------------------------------------

	private float drawMinced(DrawContext ctx, Matrix4f m, Module mod,
	                         int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String text = mod.getName();
		int textW = tr.getWidth(text);
		float h = tr.fontHeight;

		float slide = (1f - t) * (textW + 12f);
		float x = screenW - 3f - textW + slide;

		int accent = HudUtil.accentColorFor(mod);
		ctx.drawText(tr, text,
			(int) x, (int) y,
			ColorUtil.withAlpha(accent, t), true);
		return (h + 2f) * t;
	}

	// ---------- Nix --------------------------------------------------------

	private float drawNix(DrawContext ctx, Matrix4f m, Module mod,
	                      int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String text = mod.getName();
		int textW = tr.getWidth(text);
		float padX = 5f, padY = 2f;
		float w = textW + padX * 2;
		float h = tr.fontHeight + padY * 2;

		float slide = (1f - t) * (w + 12f);
		float x = screenW - 4f - w + slide;

		int accent = HudUtil.accentColorFor(mod);
		Render2D.strokeRoundedRect(m, x, y, w, h, 3f,
			ColorUtil.withAlpha(accent, t));
		ctx.drawText(tr, text,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);
		return (h + 2f) * t;
	}

	// ---------- Outline ----------------------------------------------------

	private float drawOutline(DrawContext ctx, Matrix4f m, Module mod,
	                          int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String text = mod.getName();
		int textW = tr.getWidth(text);
		float h = tr.fontHeight;

		float slide = (1f - t) * (textW + 12f);
		float x = screenW - 3f - textW + slide;

		int accent = HudUtil.accentColorFor(mod);
		// Render2D outline imitation: draw text with shadow only.
		ctx.drawText(tr, text,
			(int) x, (int) y,
			ColorUtil.withAlpha(0xFFFFFFFF, t), true);
		// Tiny accent dot to mark the row.
		Render2D.fillRect(m, x - 4f, y + h / 2f - 1f, 2f, 2f,
			ColorUtil.withAlpha(accent, t));
		return (h + 2f) * t;
	}

	// ---------- Bracket ----------------------------------------------------

	private float drawBracket(DrawContext ctx, Matrix4f m, Module mod,
	                          int screenW, float y, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		String body = mod.getName();
		int bodyW = tr.getWidth(body);
		int dotW = 6;
		float totalW = dotW + 2 + bodyW;
		float h = tr.fontHeight;

		float slide = (1f - t) * (totalW + 12f);
		float x = screenW - 3f - totalW + slide;

		int accent = HudUtil.accentColorFor(mod);
		// Solid accent square as a dot.
		Render2D.fillRoundedRect(m, x, y + 2f, 4f, 4f, 1.5f,
			ColorUtil.withAlpha(accent, t));
		ctx.drawText(tr, body,
			(int) (x + dotW), (int) y,
			ColorUtil.withAlpha(0xFFFFFFFF, t), true);
		return (h + 2f) * t;
	}
}
