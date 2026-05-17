package com.qwant.qwantdlc.render.hud;

import java.util.Comparator;
import java.util.List;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.KeybindsModule;
import com.qwant.qwantdlc.render.Render2D;
import com.qwant.qwantdlc.util.KeyNames;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds widget — list of every module with a keybind. Multiple style
 * modes:
 *
 *   - Modern    — full panel with title + divider + rows (default)
 *   - Nursultan — panel with accent stripe and bracket-style keys
 *   - Minced    — text-only rows: "Module [KEY]"
 *   - Nix       — outlined panel with chroma stroke
 *   - Bracket   — minimal "Module: KEY" lines, no panel
 */
public final class KeybindsWidget {
	private final Animation slide = new Animation(280f, Easing.EASE_OUT_QUART, 0f);

	public void render(DrawContext ctx, Matrix4f m) {
		KeybindsModule mod = HudUtil.findRender(KeybindsModule.class);
		if (mod == null || !mod.isToggled()) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		List<Module> bound = ModuleManager.getInstance().getModules().stream()
			.filter(mm -> mm.getKey() > 0 && mm.getKey() != GLFW.GLFW_KEY_UNKNOWN)
			.filter(mm -> !(mod.hideDisabled != null && mod.hideDisabled.get()) || mm.isToggled())
			.sorted(comparator(mod, tr))
			.toList();

		slide.setTargetBool(!bound.isEmpty());
		float t = slide.update();
		if (t <= 0.01f || bound.isEmpty()) return;

		String style = HudUtil.mode(mod.style, "Modern");
		switch (style) {
			case "Nursultan": drawNursultan(ctx, m, mod, bound, t); break;
			case "Minced":    drawMinced(ctx, m, mod, bound, t);    break;
			case "Nix":       drawNix(ctx, m, mod, bound, t);       break;
			case "Bracket":   drawBracket(ctx, m, mod, bound, t);   break;
			case "Modern":
			default:          drawModern(ctx, m, mod, bound, t);    break;
		}
	}

	private Comparator<Module> comparator(KeybindsModule mod, TextRenderer tr) {
		String s = HudUtil.mode(mod.sort, "Alphabetical");
		return switch (s) {
			case "Width"    -> Comparator.comparingInt((Module mm) -> tr.getWidth(mm.getName())).reversed();
			case "Category" -> Comparator.<Module, String>comparing(mm -> mm.getCategory().name())
				.thenComparing(Module::getName);
			default         -> Comparator.comparing(Module::getName);
		};
	}

	// ---------- Modern (default) -------------------------------------------

	private void drawModern(DrawContext ctx, Matrix4f m, KeybindsModule mod,
	                        List<Module> bound, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		boolean title = mod.showTitle == null || mod.showTitle.get();
		String titleText = "Keybinds";

		int contentW = title ? tr.getWidth(titleText) : 0;
		for (Module mm : bound) {
			String row = mm.getName() + "  " + KeyNames.of(mm.getKey());
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float padX = 8f, padY = 6f;
		float rowH = tr.fontHeight + 3f;
		float w = contentW + padX * 2;
		float h = padY * 2 + rowH * bound.size() + (title ? rowH + 4f : 0f);

		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float x = sw - w - 6f + (1f - t) * 16f;
		float y = (sh - h) / 2f + 30f;

		int accent = HudUtil.accentA();
		HudUtil.panel(m, x, y, w, h, 8f, accent, t);

		float ry = y + padY;
		if (title) {
			ctx.drawText(tr, titleText,
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			Render2D.fillRect(m, x + padX, ry + tr.fontHeight + 2f,
				w - padX * 2, 1f,
				ColorUtil.withAlpha(0xFF2A2A33, t));
			ry += rowH + 4f;
		}

		for (Module mm : bound) {
			ctx.drawText(tr, mm.getName(),
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
			String keyName = KeyNames.of(mm.getKey());
			int kw = tr.getWidth(keyName);
			ctx.drawText(tr, keyName,
				(int) (x + w - padX - kw), (int) ry,
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			ry += rowH;
		}
	}

	// ---------- Nursultan --------------------------------------------------

	private void drawNursultan(DrawContext ctx, Matrix4f m, KeybindsModule mod,
	                           List<Module> bound, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		boolean title = mod.showTitle == null || mod.showTitle.get();
		String titleText = "Keybinds";

		int contentW = title ? tr.getWidth(titleText) : 0;
		for (Module mm : bound) {
			String row = mm.getName() + "  [" + KeyNames.of(mm.getKey()) + "]";
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float padX = 10f, padY = 6f;
		float rowH = tr.fontHeight + 4f;
		float w = contentW + padX * 2 + 4f;
		float h = padY * 2 + rowH * bound.size() + (title ? rowH + 4f : 0f);

		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float x = sw - w - 6f + (1f - t) * 16f;
		float y = (sh - h) / 2f + 30f;

		int accentA = HudUtil.accentA();
		int accentB = HudUtil.accentB();
		HudUtil.panel(m, x, y, w, h, 10f, accentA, t);
		Render2D.fillGradientH(m, x + 2f, y + 4f, 3f, h - 8f,
			ColorUtil.withAlpha(accentA, t),
			ColorUtil.withAlpha(accentB, t));

		float ry = y + padY;
		if (title) {
			ctx.drawText(tr, titleText,
				(int) (x + padX + 2f), (int) ry,
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			ry += rowH + 4f;
		}
		for (Module mm : bound) {
			ctx.drawText(tr, mm.getName(),
				(int) (x + padX + 2f), (int) ry,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
			String keyName = "[" + KeyNames.of(mm.getKey()) + "]";
			int kw = tr.getWidth(keyName);
			ctx.drawText(tr, keyName,
				(int) (x + w - padX - kw), (int) ry,
				ColorUtil.withAlpha(accentB, t), false);
			ry += rowH;
		}
	}

	// ---------- Minced -----------------------------------------------------

	private void drawMinced(DrawContext ctx, Matrix4f m, KeybindsModule mod,
	                        List<Module> bound, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		int contentW = 0;
		for (Module mm : bound) {
			String row = mm.getName() + " [" + KeyNames.of(mm.getKey()) + "]";
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float rowH = tr.fontHeight + 1f;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float x = sw - contentW - 6f + (1f - t) * 16f;
		float y = (sh - rowH * bound.size()) / 2f + 30f;

		int accent = HudUtil.accentA();
		float ry = y;
		for (Module mm : bound) {
			String name = mm.getName();
			String key  = " [" + KeyNames.of(mm.getKey()) + "]";
			ctx.drawText(tr, name,
				(int) x, (int) ry,
				ColorUtil.withAlpha(0xFFFFFFFF, t), true);
			ctx.drawText(tr, key,
				(int) (x + tr.getWidth(name)), (int) ry,
				ColorUtil.withAlpha(accent, t), true);
			ry += rowH;
		}
	}

	// ---------- Nix --------------------------------------------------------

	private void drawNix(DrawContext ctx, Matrix4f m, KeybindsModule mod,
	                     List<Module> bound, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		boolean title = mod.showTitle == null || mod.showTitle.get();
		String titleText = "Keybinds";

		int contentW = title ? tr.getWidth(titleText) : 0;
		for (Module mm : bound) {
			String row = mm.getName() + "  " + KeyNames.of(mm.getKey());
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float padX = 8f, padY = 6f;
		float rowH = tr.fontHeight + 3f;
		float w = contentW + padX * 2;
		float h = padY * 2 + rowH * bound.size() + (title ? rowH + 4f : 0f);

		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float x = sw - w - 6f + (1f - t) * 16f;
		float y = (sh - h) / 2f + 30f;

		int accent = HudUtil.accentA();
		Render2D.fillRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(0xCC0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 6f,
			ColorUtil.withAlpha(accent, t));

		float ry = y + padY;
		if (title) {
			ctx.drawText(tr, titleText,
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			ry += rowH + 4f;
		}
		for (Module mm : bound) {
			ctx.drawText(tr, mm.getName(),
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
			String keyName = KeyNames.of(mm.getKey());
			int kw = tr.getWidth(keyName);
			ctx.drawText(tr, keyName,
				(int) (x + w - padX - kw), (int) ry,
				ColorUtil.withAlpha(accent, t), false);
			ry += rowH;
		}
	}

	// ---------- Bracket ----------------------------------------------------

	private void drawBracket(DrawContext ctx, Matrix4f m, KeybindsModule mod,
	                         List<Module> bound, float t) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		int contentW = 0;
		for (Module mm : bound) {
			String row = mm.getName() + ": " + KeyNames.of(mm.getKey());
			contentW = Math.max(contentW, tr.getWidth(row));
		}

		float rowH = tr.fontHeight + 1f;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		float x = sw - contentW - 6f + (1f - t) * 16f;
		float y = (sh - rowH * bound.size()) / 2f + 30f;

		int accent = HudUtil.accentA();
		float ry = y;
		for (Module mm : bound) {
			String name = mm.getName() + ": ";
			String key  = KeyNames.of(mm.getKey());
			ctx.drawText(tr, name,
				(int) x, (int) ry,
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), true);
			ctx.drawText(tr, key,
				(int) (x + tr.getWidth(name)), (int) ry,
				ColorUtil.withAlpha(accent, t), true);
			ry += rowH;
		}
	}
}
