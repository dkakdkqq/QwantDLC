package com.qwant.qwantdlc.render;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.gui.Theme;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import org.joml.Matrix4f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Animated HUD inspired by Minced / Nursultan / Expensive / Celestial:
 *   - Glowing rounded "Qwant" watermark in the top-left.
 *   - ArrayList of active modules in the top-right with slide-in/out and a
 *     coloured accent strip per row.
 *   - User info pill in the bottom-left (name, FPS, coords).
 *   - One-shot welcome popup that fades in then out a few seconds after
 *     joining the world.
 *
 * All draw calls go through our own Render2D.
 */
public final class HudRenderer {
	private HudRenderer() {}

	// === Watermark / welcome state ===
	private static final Animation watermarkAnim =
		new Animation(700f, Easing.EASE_OUT_BACK, 0f);
	private static final Animation welcomeAnim =
		new Animation(450f, Easing.EASE_OUT_QUART, 0f);
	private static long welcomeStartMs = -1L;
	private static boolean welcomeShown = false;

	// === ArrayList per-module slide animations ===
	private static final Map<Module, Animation> arrayAnimations = new HashMap<>();

	private static long lastFpsSampleMs = 0L;
	private static int lastFps = 0;

	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			HudModule hud = findHudModule();
			if (hud == null || !hud.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.options.hudHidden) return;
			// We still render on top of our ClickGUI screen — looks good there.
			boolean inGame = mc.player != null && mc.world != null;

			// Drive watermark as soon as HUD is on.
			watermarkAnim.setTargetBool(true);
			watermarkAnim.update();

			// Welcome popup once per world join.
			if (inGame && !welcomeShown && mc.currentScreen == null) {
				welcomeStartMs = System.currentTimeMillis();
				welcomeShown = true;
			}
			if (!inGame) {
				welcomeShown = false;
				welcomeStartMs = -1L;
				welcomeAnim.setTargetBool(false);
			}
			updateWelcomeAnim();

			Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
			renderWatermark(ctx, m);
			renderArrayList(ctx, m);
			if (inGame) {
				renderInfoPill(ctx, m);
				renderWelcome(ctx, m);
			}
		});
	}

	// ----------------------------------------------------------------- helpers

	private static HudModule findHudModule() {
		for (Module mod : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (mod instanceof HudModule h) return h;
		}
		return null;
	}

	private static int sampleFps() {
		long now = System.currentTimeMillis();
		if (now - lastFpsSampleMs > 250L) {
			lastFps = MinecraftClient.getInstance().getCurrentFps();
			lastFpsSampleMs = now;
		}
		return lastFps;
	}

	private static int accentColorFor(Module mod) {
		// Stable per-module hue based on its name.
		float hue = (Math.abs(mod.getName().hashCode()) % 360) / 360f;
		float t = (System.currentTimeMillis() % 6000L) / 6000f;
		float h = (hue + t * 0.15f) % 1f;
		int rgb = java.awt.Color.HSBtoRGB(h, 0.75f, 1f);
		return rgb | 0xFF000000;
	}

	// ---------------------------------------------------------------- widgets

	private static void renderWatermark(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float anim = watermarkAnim.update();
		if (anim <= 0.001f) return;

		// Slide in from the top-left a bit + fade in.
		float slideX = (1f - anim) * -16f;
		float alphaScale = anim;

		String brand = "Qwant";
		String tag   = "v1.0";
		int brandW = tr.getWidth(brand);
		int tagW   = tr.getWidth(tag);

		float padX = 8f, padY = 5f, gap = 6f, dot = 6f;
		float w = padX + dot + gap + brandW + 6f + tagW + padX;
		float h = tr.fontHeight + padY * 2;
		float x = 6f + slideX;
		float y = 6f;

		int chroma = ColorUtil.chroma(6f, 0.85f, 1f, 0f);
		int glowColor = ColorUtil.withAlpha(chroma, 0.7f * alphaScale);

		// Soft glow behind the pill.
		Render2D.glow(m, x, y, w, h, 8f, glowColor, 6f);

		// Pill background.
		int bg     = ColorUtil.withAlpha(0xEE0E0E14, alphaScale);
		int border = ColorUtil.withAlpha(0xFF2A2A33, alphaScale);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f, bg);
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f, border);

		// Chroma dot.
		float dotX = x + padX;
		float dotY = y + (h - dot) / 2f;
		Render2D.fillRoundedRect(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(chroma, alphaScale));
		Render2D.glow(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(chroma, 0.6f * alphaScale), 3f);

		// Text.
		int textColor = ColorUtil.withAlpha(0xFFFFFFFF, alphaScale);
		int dimColor  = ColorUtil.withAlpha(0xFFB0B0B5, alphaScale);
		float textY = y + (h - tr.fontHeight) / 2f + 1f;
		float textX = dotX + dot + gap;
		ctx.drawText(tr, brand, (int) textX, (int) textY, textColor, false);
		ctx.drawText(tr, tag,   (int) (textX + brandW + 6f), (int) textY, dimColor, false);
	}

	private static void renderArrayList(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		List<Module> all = ModuleManager.getInstance().getModules();

		// Make sure animations exist for every module.
		for (Module mod : all) {
			arrayAnimations.computeIfAbsent(mod,
				k -> new Animation(280f, Easing.EASE_OUT_QUART, 0f));
		}
		// Drive targets.
		for (Module mod : all) {
			arrayAnimations.get(mod).setTargetBool(mod.isToggled());
		}

		// Sort: longest module name first for the classic stair effect.
		all = all.stream()
			.sorted(Comparator.comparingInt((Module mod) -> tr.getWidth(mod.getName())).reversed())
			.toList();

		int screenW = ctx.getScaledWindowWidth();
		float padX = 6f, padY = 3f;
		float y = 4f;

		for (Module mod : all) {
			Animation anim = arrayAnimations.get(mod);
			float t = anim.update();
			if (t <= 0.005f) continue;

			String text = mod.getName();
			int textW = tr.getWidth(text);
			float w = textW + padX * 2;
			float h = tr.fontHeight + padY * 2;

			// Slide in from the right.
			float slide = (1f - t) * (w + 12f);
			float x = screenW - 4f - w + slide;

			int accent = accentColorFor(mod);
			int bg     = ColorUtil.withAlpha(0xEE0E0E14, t);
			int border = ColorUtil.withAlpha(accent, 0.35f * t);

			Render2D.glow(m, x, y, w, h, 4f, ColorUtil.withAlpha(accent, 0.45f * t), 4f);
			Render2D.fillRoundedRect(m, x, y, w, h, 4f, bg);
			Render2D.strokeRoundedRect(m, x, y, w, h, 4f, border);

			// Left coloured accent strip.
			Render2D.fillRoundedRect(m, x, y, 2f, h, 1f, ColorUtil.withAlpha(accent, t));

			ctx.drawText(tr, text,
				(int) (x + padX),
				(int) (y + padY),
				ColorUtil.withAlpha(0xFFFFFFFF, t),
				false);

			y += (h + 2f) * t;
		}
	}

	private static void renderInfoPill(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) return;

		String user = p.getGameProfile().getName();
		int fps = sampleFps();
		String coords = String.format(Locale.ROOT, "%.0f, %.0f, %.0f", p.getX(), p.getY(), p.getZ());

		float anim = watermarkAnim.getValue();
		if (anim <= 0.01f) return;

		String line1 = user + "  •  " + fps + " FPS";
		String line2 = "XYZ: " + coords;
		int w1 = tr.getWidth(line1), w2 = tr.getWidth(line2);
		int contentW = Math.max(w1, w2);
		float padX = 8f, padY = 5f;
		float w = contentW + padX * 2;
		float h = tr.fontHeight * 2 + padY * 2 + 2f;
		int screenH = ctx.getScaledWindowHeight();
		float slideY = (1f - anim) * 16f;
		float x = 6f;
		float y = screenH - 6f - h + slideY;

		int accent = ColorUtil.chroma(6f, 0.85f, 1f, 0.3f);
		int bg = ColorUtil.withAlpha(0xEE0E0E14, anim);
		int border = ColorUtil.withAlpha(accent, 0.3f * anim);

		Render2D.glow(m, x, y, w, h, 8f, ColorUtil.withAlpha(accent, 0.35f * anim), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f, bg);
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f, border);

		ctx.drawText(tr, line1,
			(int) (x + padX),
			(int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);
		ctx.drawText(tr, line2,
			(int) (x + padX),
			(int) (y + padY + tr.fontHeight + 2f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, anim), false);
	}

	private static void updateWelcomeAnim() {
		if (welcomeStartMs < 0) return;
		long elapsed = System.currentTimeMillis() - welcomeStartMs;
		if (elapsed < 3500L) {
			welcomeAnim.setTargetBool(true);
		} else {
			welcomeAnim.setTargetBool(false);
		}
		welcomeAnim.update();
	}

	private static void renderWelcome(DrawContext ctx, Matrix4f m) {
		float anim = welcomeAnim.getValue();
		if (anim <= 0.01f) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		MinecraftClient mc = MinecraftClient.getInstance();
		String name = mc.player != null ? mc.player.getGameProfile().getName() : "";
		String text = "Добро пожаловать, " + name;

		int textW = tr.getWidth(text);
		float padX = 14f, padY = 8f;
		float w = textW + padX * 2;
		float h = tr.fontHeight + padY * 2;

		int screenW = ctx.getScaledWindowWidth();
		int screenH = ctx.getScaledWindowHeight();

		// Slide down from the top-center.
		float startY = -h - 4f;
		float endY   = screenH * 0.18f;
		float y = startY + (endY - startY) * anim;
		float x = (screenW - w) / 2f;

		int accentA = 0xFF8A2BE2;
		int accentB = 0xFFB14CFF;
		int bg = ColorUtil.withAlpha(0xEE111118, anim);
		int border = ColorUtil.withAlpha(accentB, 0.5f * anim);

		Render2D.glow(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB, 0.45f * anim), 10f);
		Render2D.fillRoundedRect(m, x, y, w, h, 10f, bg);
		// Gradient accent line under the text.
		Render2D.fillGradientH(m, x + padX / 2f, y + h - 3f,
			w - padX, 1.5f,
			ColorUtil.withAlpha(accentA, anim),
			ColorUtil.withAlpha(accentB, anim));
		Render2D.strokeRoundedRect(m, x, y, w, h, 10f, border);

		ctx.drawText(tr, text,
			(int) (x + padX),
			(int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, anim),
			false);

		// Subtitle line.
		String subtitle = QwantDLC.MOD_NAME + " готов к работе";
		int sw = tr.getWidth(subtitle);
		ctx.drawText(tr, subtitle,
			(int) (x + (w - sw) / 2f),
			(int) (y + h + 4f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, anim),
			false);
	}
}
