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
import com.qwant.qwantdlc.util.KeyNames;
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
import org.lwjgl.glfw.GLFW;

/**
 * Full-recode HUD inspired by Minced / Nursultan / Nix:
 *
 *   Top-left:        Watermark (Q + brand + accent dot)
 *   Top-right:       ArrayList of enabled modules (slide-in stripes)
 *   Bottom-left:     Info pill   (user / fps / xyz / b/s / ping)
 *   Bottom-right:    Coords readout in muted text (X/Y/Z + facing)
 *   Centre-right:    Keybinds widget
 *   Centre-bottom:   TargetHUD with health bar
 *   Right-of-centre: PotionHUD with effect rows + duration
 *   Top-centre:      Welcome popup (3.5s on world join)
 *   Top-right:       Notification stack (toast on module toggle)
 *
 * All widgets share the accent palette resolved from {@code HudModule.accent}.
 * Defensive null-checks and try/catch around every API call to make sure
 * the HUD can't crash the game.
 */
public final class HudRenderer {
	private HudRenderer() {}

	// =========================================================== palette

	private static final int PANEL_BG       = 0xCC0E0E14;
	private static final int PANEL_BG_DEEP  = 0xEE0E0E14;
	private static final int PANEL_STROKE   = 0xFF2A2A33;
	private static final int TEXT_PRIMARY   = 0xFFFFFFFF;
	private static final int TEXT_SECONDARY = 0xFFB5B5BE;
	private static final int TEXT_MUTED     = 0xFF7A7A85;

	private static int accentA() {
		HudModule h = findHud();
		String mode = (h != null && h.accent != null) ? h.accent.getCurrent() : "Purple";
		switch (mode) {
			case "Pink":   return 0xFFEC4899;
			case "Cyan":   return 0xFF22D3EE;
			case "Green":  return 0xFF22C55E;
			case "Amber":  return 0xFFF59E0B;
			case "Red":    return 0xFFEF4444;
			case "Chroma": return ColorUtil.chroma(6f, 0.85f, 1f, 0f);
			case "Purple":
			default:       return 0xFFB14CFF;
		}
	}

	private static int accentB() {
		HudModule h = findHud();
		String mode = (h != null && h.accent != null) ? h.accent.getCurrent() : "Purple";
		switch (mode) {
			case "Pink":   return 0xFFF9A8D4;
			case "Cyan":   return 0xFF67E8F9;
			case "Green":  return 0xFF86EFAC;
			case "Amber":  return 0xFFFCD34D;
			case "Red":    return 0xFFF87171;
			case "Chroma": return ColorUtil.chroma(6f, 0.85f, 1f, 0.18f);
			case "Purple":
			default:       return 0xFF8A2BE2;
		}
	}

	// =========================================================== state

	private static final float SMOOTH = 0.15f;
	private static float fpsAnim, xAnim, yAnim, zAnim, speedAnim, pingAnim;

	private static final Animation watermarkAnim = new Animation(420f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation infoAnim      = new Animation(420f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation coordsAnim    = new Animation(420f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation keybindsAnim  = new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation targetAnim    = new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation potionAnim    = new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private static final Animation welcomeAnim   = new Animation(450f, Easing.EASE_OUT_QUART, 0f);
	private static final Map<Module, Animation> arrayAnim = new HashMap<>();

	private static float targetHealthSmooth = 1f;

	// Welcome popup state
	private static long welcomeStartMs = -1L;
	private static boolean welcomeShown = false;

	// Notifications: track last known toggle state so we can fire a toast
	// whenever a module flips.
	private static final Map<Module, Boolean> lastToggleState = new HashMap<>();
	private static final List<Toast> toasts = new ArrayList<>();
	private static final int MAX_TOASTS = 5;

	private static final class Toast {
		final String text;
		final int accent;
		final long startMs;
		final Animation anim;
		Toast(String text, int accent) {
			this.text = text;
			this.accent = accent;
			this.startMs = System.currentTimeMillis();
			this.anim = new Animation(300f, Easing.EASE_OUT_QUART, 0f);
			this.anim.setTargetBool(true);
		}
	}

	// =========================================================== entry point

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
			boolean inGame = p != null && mc.world != null;

			if (p != null) {
				updateAnimations(p);
			}

			// Welcome popup state machine.
			if (inGame && !welcomeShown && mc.currentScreen == null) {
				welcomeStartMs = System.currentTimeMillis();
				welcomeShown = true;
			}
			if (!inGame) {
				welcomeShown = false;
				welcomeStartMs = -1L;
			}

			detectToggleEvents();

			// Always-visible:
			if (hud.watermark.get()) {
				watermarkAnim.setTargetBool(true);
				drawWatermark(ctx, m, watermarkAnim.update());
			} else {
				watermarkAnim.setTargetBool(false);
				watermarkAnim.update();
			}

			if (hud.arrayList.get()) {
				drawArrayList(ctx, m, sw);
			}

			if (hud.notifications.get()) {
				drawNotifications(ctx, m, sw);
			}

			// In-game widgets:
			if (inGame) {
				if (hud.infoPill.get()) {
					infoAnim.setTargetBool(true);
					drawInfoPill(ctx, m, sh, infoAnim.update());
				} else {
					infoAnim.setTargetBool(false);
					infoAnim.update();
				}

				if (hud.coordsHud.get()) {
					coordsAnim.setTargetBool(true);
					drawCoordsHud(ctx, m, sw, sh, coordsAnim.update(), p);
				} else {
					coordsAnim.setTargetBool(false);
					coordsAnim.update();
				}

				if (hud.keybinds.get()) {
					drawKeybinds(ctx, m, sw, sh);
				}

				if (hud.targetHud.get()) {
					drawTargetHud(ctx, m, sw, sh);
				}

				if (hud.potionHud.get()) {
					drawPotionHud(ctx, m, sw, sh);
				}

				if (hud.welcomePopup.get()) {
					updateWelcomeTarget();
					drawWelcomePopup(ctx, m, sw, sh, welcomeAnim.update(), p);
				} else {
					welcomeAnim.setTargetBool(false);
					welcomeAnim.update();
				}
			}
		});
	}

	// =========================================================== shared utils

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

	/** Standard rounded panel: glow + dark fill + thin stroke. */
	private static void panel(Matrix4f m, float x, float y, float w, float h,
	                          float radius, float alpha) {
		Render2D.glow(m, x, y, w, h, radius,
			ColorUtil.withAlpha(accentA(), 0.30f * alpha), 5f);
		Render2D.fillRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(PANEL_BG_DEEP, alpha));
		Render2D.strokeRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(PANEL_STROKE, alpha));
	}

	// =========================================================== watermark

	private static void drawWatermark(DrawContext ctx, Matrix4f m, float t) {
		if (t <= 0.005f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String letter = "Q";
		String brand = QwantDLC.MOD_NAME;
		String tag = "v1.0";
		int letterW = tr.getWidth(letter);
		int brandW = tr.getWidth(brand);
		int tagW = tr.getWidth(tag);

		float padX = 8f, padY = 5f, gap = 6f, dot = 5f, sep = 6f;
		float w = padX + letterW + gap + dot + gap + brandW + sep + 1f + sep + tagW + padX;
		float h = tr.fontHeight + padY * 2;

		float slide = (1f - t) * -16f;
		float x = 6f + slide;
		float y = 6f;

		panel(m, x, y, w, h, 6f, t);

		float textY = y + (h - tr.fontHeight) / 2f + 1f;

		// Letter (accent).
		ctx.drawText(tr, letter, (int) (x + padX), (int) textY,
			ColorUtil.withAlpha(accentA(), t), false);

		// Accent dot.
		float dotX = x + padX + letterW + gap;
		float dotY = y + (h - dot) / 2f;
		Render2D.fillRoundedRect(m, dotX, dotY, dot, dot, dot / 2f,
			ColorUtil.withAlpha(accentA(), t));

		// Brand.
		float brandX = dotX + dot + gap;
		ctx.drawText(tr, brand, (int) brandX, (int) textY,
			ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

		// Vertical separator.
		float sepX = brandX + brandW + sep;
		Render2D.fillRect(m, sepX, y + 4f, 1f, h - 8f,
			ColorUtil.withAlpha(PANEL_STROKE, t));

		// Version tag.
		ctx.drawText(tr, tag, (int) (sepX + sep), (int) textY,
			ColorUtil.withAlpha(TEXT_SECONDARY, t), false);
	}

	// =========================================================== arraylist

	private static void drawArrayList(DrawContext ctx, Matrix4f m, int sw) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		List<Module> all = ModuleManager.getInstance().getModules();
		for (Module mod : all) {
			arrayAnim.computeIfAbsent(mod,
				k -> new Animation(280f, Easing.EASE_OUT_QUART, 0f));
		}
		for (Module mod : all) {
			arrayAnim.get(mod).setTargetBool(mod.isToggled());
		}

		List<Module> sorted = new ArrayList<>(all);
		sorted.sort(Comparator.comparingInt((Module mod) ->
			tr.getWidth(mod.getName())).reversed());

		float padX = 6f, padY = 3f;
		float y = 4f;
		// If notifications stack starts at the top-right, push the
		// arraylist down to make room.
		HudModule hud = findHud();
		if (hud != null && hud.notifications != null && hud.notifications.get()
			&& !toasts.isEmpty()) {
			y += toasts.size() * 18f + 4f;
		}

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
				ColorUtil.withAlpha(accentA(), 0.30f * t), 4f);
			Render2D.fillRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(PANEL_BG, t));
			Render2D.strokeRoundedRect(m, x, y, w, h, 4f,
				ColorUtil.withAlpha(accentA(), 0.40f * t));
			Render2D.fillRoundedRect(m, x, y, 2f, h, 1f,
				ColorUtil.withAlpha(accentA(), t));

			ctx.drawText(tr, text,
				(int) (x + padX), (int) (y + padY),
				ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

			y += (h + 2f) * t;
		}
	}

	// =========================================================== info pill

	private static void drawInfoPill(DrawContext ctx, Matrix4f m, int sh, float t) {
		if (t <= 0.005f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) return;

		String user = p.getGameProfile().getName();
		String fps  = Math.round(fpsAnim) + " fps";
		String bps  = String.format(Locale.ROOT, "%.1f b/s", speedAnim);
		String ping = Math.round(pingAnim) + " ms";

		String[] segments = { user, fps, bps, ping };
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

	// =========================================================== coords hud

	private static void drawCoordsHud(DrawContext ctx, Matrix4f m, int sw, int sh,
	                                  float t, ClientPlayerEntity p) {
		if (t <= 0.005f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String facing = facingFor(p.getYaw());
		String[] lines = {
			String.format(Locale.ROOT, "X  %.1f", xAnim),
			String.format(Locale.ROOT, "Y  %.1f", yAnim),
			String.format(Locale.ROOT, "Z  %.1f", zAnim),
			"FACE  " + facing,
		};

		int contentW = 0;
		for (String line : lines) contentW = Math.max(contentW, tr.getWidth(line));

		float padX = 8f, padY = 6f;
		float lineH = tr.fontHeight + 2f;
		float w = contentW + padX * 2;
		float h = lineH * lines.length + padY * 2 - 2f;

		float slideY = (1f - t) * 16f;
		float x = sw - w - 6f;
		float y = sh - 6f - h + slideY;

		panel(m, x, y, w, h, 6f, t);
		Render2D.fillRoundedRect(m, x, y, 2f, h, 1f,
			ColorUtil.withAlpha(accentA(), t));

		float ly = y + padY;
		for (String line : lines) {
			int idx = line.indexOf("  ");
			if (idx > 0) {
				String label = line.substring(0, idx);
				String value = line.substring(idx);
				ctx.drawText(tr, label,
					(int) (x + padX), (int) ly,
					ColorUtil.withAlpha(accentA(), t), false);
				int lw = tr.getWidth(label);
				ctx.drawText(tr, value,
					(int) (x + padX + lw), (int) ly,
					ColorUtil.withAlpha(TEXT_PRIMARY, t), false);
			} else {
				ctx.drawText(tr, line,
					(int) (x + padX), (int) ly,
					ColorUtil.withAlpha(TEXT_PRIMARY, t), false);
			}
			ly += lineH;
		}
	}

	private static String facingFor(float yaw) {
		float n = ((yaw % 360f) + 360f) % 360f;
		if (n >= 315f || n < 45f)  return "S";
		if (n < 135f)              return "W";
		if (n < 225f)              return "N";
		return "E";
	}

	// =========================================================== keybinds

	private static void drawKeybinds(DrawContext ctx, Matrix4f m, int sw, int sh) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		List<Module> bound = ModuleManager.getInstance().getModules().stream()
			.filter(mm -> mm.getKey() > 0 && mm.getKey() != GLFW.GLFW_KEY_UNKNOWN)
			.sorted(Comparator.comparing(Module::getName))
			.toList();

		keybindsAnim.setTargetBool(!bound.isEmpty());
		float t = keybindsAnim.update();
		if (t <= 0.01f || bound.isEmpty()) return;

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

		float slideX = (1f - t) * 16f;
		float x = sw - w - 6f + slideX;
		float y = (sh - h) / 2f + 30f;

		panel(m, x, y, w, h, 6f, t);

		ctx.drawText(tr, title,
			(int) (x + padX), (int) (y + padY),
			ColorUtil.withAlpha(accentA(), t), false);
		Render2D.fillRect(m, x + padX, y + padY + tr.fontHeight + 2f,
			w - padX * 2, 1f,
			ColorUtil.withAlpha(PANEL_STROKE, t));

		float ry = y + padY + rowH + 4f;
		for (Module mm : bound) {
			ctx.drawText(tr, mm.getName(),
				(int) (x + padX), (int) ry,
				ColorUtil.withAlpha(TEXT_SECONDARY, t), false);
			String keyName = "[" + KeyNames.of(mm.getKey()) + "]";
			int kw = tr.getWidth(keyName);
			ctx.drawText(tr, keyName,
				(int) (x + w - padX - kw), (int) ry,
				ColorUtil.withAlpha(TEXT_PRIMARY, t), false);
			ry += rowH;
		}
	}

	// =========================================================== targethud

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
			ColorUtil.withAlpha(PANEL_BG_DEEP, t));
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

	// =========================================================== potionhud

	private static void drawPotionHud(DrawContext ctx, Matrix4f m, int sw, int sh) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;

		List<StatusEffectInstance> effects = collectEffects(mc.player.getStatusEffects());
		potionAnim.setTargetBool(!effects.isEmpty());
		float t = potionAnim.update();
		if (t <= 0.01f || effects.isEmpty()) return;

		TextRenderer tr = mc.textRenderer;

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

	// =========================================================== welcome popup

	private static void updateWelcomeTarget() {
		if (welcomeStartMs < 0) {
			welcomeAnim.setTargetBool(false);
			return;
		}
		long elapsed = System.currentTimeMillis() - welcomeStartMs;
		welcomeAnim.setTargetBool(elapsed < 3500L);
	}

	private static void drawWelcomePopup(DrawContext ctx, Matrix4f m,
	                                     int sw, int sh, float a,
	                                     ClientPlayerEntity p) {
		if (a <= 0.01f) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		String name = p.getGameProfile().getName();
		String title = "Welcome, " + name;
		String subtitle = QwantDLC.MOD_NAME + " ready";

		int titleW = tr.getWidth(title);
		int subW   = tr.getWidth(subtitle);
		int contentW = Math.max(titleW, subW);
		float padX = 14f, padY = 10f;
		float w = contentW + padX * 2;
		float h = tr.fontHeight * 2 + padY * 2 + 4f;

		float startY = -h - 4f;
		float endY   = sh * 0.18f;
		float y = startY + (endY - startY) * a;
		float x = (sw - w) / 2f;

		Render2D.glow(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB(), 0.45f * a), 10f);
		Render2D.fillRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(0xEE111118, a));
		Render2D.fillGradientH(m, x + padX / 2f, y + h - 3f,
			w - padX, 1.5f,
			ColorUtil.withAlpha(accentA(), a),
			ColorUtil.withAlpha(accentB(), a));
		Render2D.strokeRoundedRect(m, x, y, w, h, 10f,
			ColorUtil.withAlpha(accentB(), 0.5f * a));

		ctx.drawText(tr, title,
			(int) (x + (w - titleW) / 2f), (int) (y + padY),
			ColorUtil.withAlpha(TEXT_PRIMARY, a), false);
		ctx.drawText(tr, subtitle,
			(int) (x + (w - subW) / 2f), (int) (y + padY + tr.fontHeight + 4f),
			ColorUtil.withAlpha(TEXT_MUTED, a), false);
	}

	// =========================================================== notifications

	/** Compares each module's toggled state against the previously-recorded
	 *  state. When it flips we push a toast (only if notifications are on,
	 *  to avoid an unbounded queue when the widget is disabled). */
	private static void detectToggleEvents() {
		HudModule hud = findHud();
		boolean wantToasts = hud != null && hud.notifications != null
			&& hud.notifications.get();
		for (Module mod : ModuleManager.getInstance().getModules()) {
			Boolean was = lastToggleState.get(mod);
			boolean now = mod.isToggled();
			if (was == null) {
				lastToggleState.put(mod, now);
				continue;
			}
			if (was != now) {
				lastToggleState.put(mod, now);
				if (!wantToasts) continue;
				int color = now ? 0xFF22C55E : 0xFFEF4444;
				String label = mod.getName() + (now ? "  ENABLED" : "  DISABLED");
				toasts.add(0, new Toast(label, color));
				if (toasts.size() > MAX_TOASTS) {
					toasts.remove(toasts.size() - 1);
				}
			}
		}
	}

	private static void drawNotifications(DrawContext ctx, Matrix4f m, int sw) {
		if (toasts.isEmpty()) return;
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		long now = System.currentTimeMillis();

		float padX = 8f, padY = 4f;
		float spacing = 2f;
		float toastH = tr.fontHeight + padY * 2;
		float y = 4f;

		List<Toast> kept = new ArrayList<>();
		for (Toast toast : toasts) {
			long elapsed = now - toast.startMs;
			if (elapsed > 3000L) {
				toast.anim.setTargetBool(false);
			}
			float t = toast.anim.update();
			if (elapsed > 3000L && t <= 0.005f) continue; // expired
			kept.add(toast);

			int textW = tr.getWidth(toast.text);
			float w = textW + padX * 2 + 6f;

			float slide = (1f - t) * (w + 12f);
			float x = sw - 4f - w + slide;

			Render2D.glow(m, x, y, w, toastH, 4f,
				ColorUtil.withAlpha(toast.accent, 0.30f * t), 4f);
			Render2D.fillRoundedRect(m, x, y, w, toastH, 4f,
				ColorUtil.withAlpha(PANEL_BG_DEEP, t));
			Render2D.strokeRoundedRect(m, x, y, w, toastH, 4f,
				ColorUtil.withAlpha(toast.accent, 0.5f * t));
			Render2D.fillRoundedRect(m, x, y, 3f, toastH, 1.5f,
				ColorUtil.withAlpha(toast.accent, t));

			ctx.drawText(tr, toast.text,
				(int) (x + padX + 4f), (int) (y + padY),
				ColorUtil.withAlpha(TEXT_PRIMARY, t), false);

			y += (toastH + spacing) * t;
		}
		toasts.clear();
		toasts.addAll(kept);
	}

	// =========================================================== effect helpers

	private static List<StatusEffectInstance> collectEffects(
			Collection<StatusEffectInstance> source) {
		List<StatusEffectInstance> out = new ArrayList<>();
		try {
			for (StatusEffectInstance eff : source) {
				out.add(eff);
			}
		} catch (Throwable ignored) {}
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
			return accentA();
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
