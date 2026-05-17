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
import com.qwant.qwantdlc.module.modules.render.KeybindsModule;
import com.qwant.qwantdlc.module.modules.render.PotionHudModule;
import com.qwant.qwantdlc.module.modules.render.TargetHudModule;
import com.qwant.qwantdlc.util.KeyNames;
import com.qwant.qwantdlc.util.TargetTracker;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Animated HUD inspired by Minced / Nursultan / Nix / Expensive / Celestial.
 *
 * Widgets:
 *   - Watermark pill (top-left) with chroma dot, slide-in.
 *   - ArrayList (top-right) with per-module slide+glow.
 *   - Info pill (bottom-left): user, FPS, XYZ.
 *   - TargetHUD (centre-left): last attacked living entity with health bar.
 *   - PotionHUD (above hotbar): active effects with progress.
 *   - Keybinds widget (centre-right): all bound modules.
 *   - Welcome popup on first world join.
 */
public final class HudRenderer {
	private HudRenderer() {}

	private static final Animation watermarkAnim =
		new Animation(700f, Easing.EASE_OUT_BACK, 0f);
	private static final Animation welcomeAnim =
		new Animation(450f, Easing.EASE_OUT_QUART, 0f);
	private static long welcomeStartMs = -1L;
	private static boolean welcomeShown = false;

	private static final Map<Module, Animation> arrayAnimations = new HashMap<>();
	private static final Animation targetAnim =
		new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation potionAnim =
		new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation keybindsAnim =
		new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static float targetHealthSmooth = 1f;

	private static long lastFpsSampleMs = 0L;
	private static int lastFps = 0;

	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			HudModule hud = findRenderModule(HudModule.class);
			if (hud == null || !hud.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.options.hudHidden) return;

			boolean inGame = mc.player != null && mc.world != null;

			watermarkAnim.setTargetBool(true);
			watermarkAnim.update();

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
				renderTargetHud(ctx, m);
				renderPotionHud(ctx, m);
				renderKeybinds(ctx, m);
				renderWelcome(ctx, m);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static <T extends Module> T findRenderModule(Class<T> cls) {
		for (Module mod : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (cls.isInstance(mod)) return (T) mod;
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
		float hue = (Math.abs(mod.getName().hashCode()) % 360) / 360f;
		float t = (System.currentTimeMillis() % 6000L) / 6000f;
		float h = (hue + t * 0.15f) % 1f;
		int rgb = java.awt.Color.HSBtoRGB(h, 0.75f, 1f);
		return rgb | 0xFF000000;
	}

	// ============================================================ Watermark

	private static void renderWatermark(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float anim = watermarkAnim.update();
		if (anim <= 0.001f) return;

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
		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(chroma, 0.7f * alphaScale), 6f);

		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xEE0E0E14, alphaScale));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xFF2A2A33, alphaScale));

		float dotX = x + padX;
		float dotY = y + (h - dot) / 2f;
		Render2D.fillRoundedRect(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(chroma, alphaScale));
		Render2D.glow(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(chroma, 0.6f * alphaScale), 3f);

		float textY = y + (h - tr.fontHeight) / 2f + 1f;
		float textX = dotX + dot + gap;
		ctx.drawText(tr, brand, (int) textX, (int) textY,
			ColorUtil.withAlpha(0xFFFFFFFF, alphaScale), false);
		ctx.drawText(tr, tag, (int) (textX + brandW + 6f), (int) textY,
			ColorUtil.withAlpha(0xFFB0B0B5, alphaScale), false);
	}

	// ============================================================ ArrayList

	private static void renderArrayList(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		List<Module> all = ModuleManager.getInstance().getModules();

		for (Module mod : all) {
			arrayAnimations.computeIfAbsent(mod,
				k -> new Animation(280f, Easing.EASE_OUT_QUART, 0f));
		}
		for (Module mod : all) {
			arrayAnimations.get(mod).setTargetBool(mod.isToggled());
		}

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

			float slide = (1f - t) * (w + 12f);
			float x = screenW - 4f - w + slide;

			int accent = accentColorFor(mod);
			Render2D.glow(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(accent, 0.45f * t), 4f);
			Render2D.fillRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(0xEE0E0E14, t));
			Render2D.strokeRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(accent, 0.35f * t));
			Render2D.fillRoundedRect(m, x, y, 2f, h, 1f,
				ColorUtil.withAlpha(accent, t));

			ctx.drawText(tr, text,
				(int) (x + padX),
				(int) (y + padY),
				ColorUtil.withAlpha(0xFFFFFFFF, t),
				false);

			y += (h + 2f) * t;
		}
	}

	// ============================================================ Info pill

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
		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.35f * anim), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xEE0E0E14, anim));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.3f * anim));

		ctx.drawText(tr, line1,
			(int) (x + padX),
			(int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);
		ctx.drawText(tr, line2,
			(int) (x + padX),
			(int) (y + padY + tr.fontHeight + 2f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, anim), false);
	}

	// ============================================================ TargetHUD

	private static void renderTargetHud(DrawContext ctx, Matrix4f m) {
		TargetHudModule mod = findRenderModule(TargetHudModule.class);
		if (mod == null || !mod.isToggled()) return;

		LivingEntity target = TargetTracker.getTarget();
		targetAnim.setTargetBool(target != null);
		float t = targetAnim.update();
		if (t <= 0.01f) return;
		if (target == null) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String name = target.getDisplayName() != null
			? target.getDisplayName().getString()
			: target.getName().getString();
		float maxHp = Math.max(1f, target.getMaxHealth());
		float hp = Math.max(0f, target.getHealth());
		float ratio = Math.max(0f, Math.min(1f, hp / maxHp));

		// Smooth health bar with simple low-pass.
		targetHealthSmooth += (ratio - targetHealthSmooth) * 0.12f;

		String hpStr = String.format(Locale.ROOT, "%.1f / %.1f", hp, maxHp);
		int contentW = Math.max(tr.getWidth(name), tr.getWidth(hpStr));
		float padX = 10f, padY = 8f;
		float w = Math.max(120f, contentW + padX * 2);
		float h = tr.fontHeight * 2 + padY * 2 + 6f;

		int screenW = ctx.getScaledWindowWidth();
		int screenH = ctx.getScaledWindowHeight();
		float x = (screenW - w) / 2f - 110f;
		float y = (screenH - h) / 2f + 60f;
		float slideY = (1f - t) * 12f;
		y += slideY;

		int accent = healthAccent(ratio);
		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.4f * t), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xEE0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.45f * t));

		ctx.drawText(tr, name,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);

		// Health bar.
		float barX = x + padX;
		float barY = y + padY + tr.fontHeight + 4f;
		float barW = w - padX * 2;
		float barH = 4f;
		Render2D.fillRoundedRect(m, barX, barY, barW, barH, 2f,
			ColorUtil.withAlpha(0xFF1A1A1F, t));
		float fillW = Math.max(0f, barW * targetHealthSmooth);
		if (fillW > 0.5f) {
			Render2D.fillGradientH(m, barX, barY, fillW, barH,
				ColorUtil.withAlpha(accent, t),
				ColorUtil.withAlpha(brighten(accent, 1.3f), t));
		}

		ctx.drawText(tr, hpStr,
			(int) (x + padX),
			(int) (barY + barH + 4f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
	}

	private static int healthAccent(float ratio) {
		if (ratio >= 0.66f) return 0xFF4ADE80; // green
		if (ratio >= 0.33f) return 0xFFFACC15; // yellow
		return 0xFFEF4444;                     // red
	}

	private static int brighten(int color, float factor) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int) ((color & 0xFF) * factor));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ============================================================ PotionHUD

	private static void renderPotionHud(DrawContext ctx, Matrix4f m) {
		PotionHudModule mod = findRenderModule(PotionHudModule.class);
		if (mod == null || !mod.isToggled()) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;

		var effects = mc.player.getStatusEffects();
		potionAnim.setTargetBool(!effects.isEmpty());
		float t = potionAnim.update();
		if (t <= 0.01f || effects.isEmpty()) return;

		TextRenderer tr = mc.textRenderer;

		// Compute width from longest entry.
		int contentW = 0;
		for (StatusEffectInstance eff : effects) {
			String label = effectLabel(eff);
			contentW = Math.max(contentW, tr.getWidth(label));
		}
		float padX = 8f, padY = 6f;
		float rowH = tr.fontHeight + 5f;
		float w = contentW + padX * 2 + 30f; // +30 for the duration column
		float h = rowH * effects.size() + padY * 2;

		int screenW = ctx.getScaledWindowWidth();
		int screenH = ctx.getScaledWindowHeight();
		// Above the hotbar, right of centre.
		float x = (screenW + 100f) / 2f;
		float y = screenH - 60f - h;
		float slideX = (1f - t) * 20f;
		x += slideX;

		int accent = 0xFFB14CFF;
		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.3f * t), 5f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xEE0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.35f * t));

		float ry = y + padY;
		for (StatusEffectInstance eff : effects) {
			String label = effectLabel(eff);
			String dur   = formatDuration(eff);
			int color = effectColor(eff);

			Render2D.fillRoundedRect(m, x + padX - 4f, ry + 1f, 2f, rowH - 2f, 1f,
				ColorUtil.withAlpha(color, t));

			ctx.drawText(tr, label,
				(int) (x + padX + 2f),
				(int) (ry + 1f),
				ColorUtil.withAlpha(0xFFFFFFFF, t), false);
			int durW = tr.getWidth(dur);
			ctx.drawText(tr, dur,
				(int) (x + w - padX - durW),
				(int) (ry + 1f),
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, t), false);
			ry += rowH;
		}
	}

	private static String effectLabel(StatusEffectInstance eff) {
		RegistryEntry<StatusEffect> entry = eff.getEffectType();
		MutableText name = Text.translatable(entry.value().getTranslationKey());
		int amp = eff.getAmplifier();
		String roman = romanNumeral(amp + 1);
		return name.getString() + " " + roman;
	}

	private static String formatDuration(StatusEffectInstance eff) {
		try {
			Text d = StatusEffectUtil.getDurationText(eff, 1f, 20f);
			return d.getString();
		} catch (Throwable t) {
			int sec = eff.getDuration() / 20;
			return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
		}
	}

	private static int effectColor(StatusEffectInstance eff) {
		try {
			int rgb = eff.getEffectType().value().getColor();
			return rgb | 0xFF000000;
		} catch (Throwable t) {
			return 0xFFB14CFF;
		}
	}

	private static String romanNumeral(int n) {
		switch (n) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(n);
		}
	}

	// ============================================================ Keybinds widget

	private static void renderKeybinds(DrawContext ctx, Matrix4f m) {
		KeybindsModule mod = findRenderModule(KeybindsModule.class);
		if (mod == null || !mod.isToggled()) return;

		List<Module> bound = ModuleManager.getInstance().getModules().stream()
			.filter(mm -> mm.getKey() > 0 && mm.getKey() != GLFW.GLFW_KEY_UNKNOWN)
			.toList();

		keybindsAnim.setTargetBool(!bound.isEmpty());
		float t = keybindsAnim.update();
		if (t <= 0.01f || bound.isEmpty()) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String title = "Keybinds";
		int contentW = tr.getWidth(title);
		for (Module mm : bound) {
			String row = mm.getName() + "  " + KeyNames.of(mm.getKey());
			contentW = Math.max(contentW, tr.getWidth(row));
		}
		float padX = 8f, padY = 6f;
		float rowH = tr.fontHeight + 3f;
		float w = contentW + padX * 2;
		float h = padY * 2 + rowH * (bound.size() + 1) + 4f;

		int screenW = ctx.getScaledWindowWidth();
		int screenH = ctx.getScaledWindowHeight();
		float x = screenW - w - 6f;
		float y = (screenH - h) / 2f + 30f;
		float slideX = (1f - t) * 16f;
		x += slideX;

		int accent = 0xFF8A2BE2;
		Render2D.glow(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.3f * t), 5f);
		Render2D.fillRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(0xEE0E0E14, t));
		Render2D.strokeRoundedRect(m, x, y, w, h, 8f,
			ColorUtil.withAlpha(accent, 0.4f * t));

		ctx.drawText(tr, title,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, t), false);
		Render2D.fillRect(m, x + padX, y + padY + tr.fontHeight + 2f,
			w - padX * 2, 1f,
			ColorUtil.withAlpha(0xFF2A2A33, t));

		float ry = y + padY + rowH + 4f;
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

	// ============================================================ Welcome

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

		float startY = -h - 4f;
		float endY   = screenH * 0.18f;
		float y = startY + (endY - startY) * anim;
		float x = (screenW - w) / 2f;

		int accentA = 0xFF8A2BE2;
		int accentB = 0xFFB14CFF;
		Render2D.glow(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB, 0.45f * anim), 10f);
		Render2D.fillRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(0xEE111118, anim));
		Render2D.fillGradientH(m, x + padX / 2f, y + h - 3f,
			w - padX, 1.5f,
			ColorUtil.withAlpha(accentA, anim),
			ColorUtil.withAlpha(accentB, anim));
		Render2D.strokeRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB, 0.5f * anim));

		ctx.drawText(tr, text,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(0xFFFFFFFF, anim), false);

		String subtitle = QwantDLC.MOD_NAME + " готов к работе";
		int sw = tr.getWidth(subtitle);
		ctx.drawText(tr, subtitle,
			(int) (x + (w - sw) / 2f),
			(int) (y + h + 4f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, anim), false);
	}
}
