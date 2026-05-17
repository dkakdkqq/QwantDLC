package com.qwant.qwantdlc.render.hud;

import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.render.Render2D;
import com.qwant.qwantdlc.setting.ModeSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;

import org.joml.Matrix4f;

/**
 * Shared helpers for HUD widgets — accent color resolution, panel/gradient
 * helpers, and small utilities for entity / status-effect rendering.
 *
 * Style-related code that every widget needs (accent palette, brand colors)
 * lives here so widgets stay focused on their layout.
 */
public final class HudUtil {
	private HudUtil() {}

	// === Accent palette =====================================================

	public static final int ACCENT_PURPLE_A = 0xFF8A2BE2;
	public static final int ACCENT_PURPLE_B = 0xFFB14CFF;
	public static final int ACCENT_CYAN_A   = 0xFF22D3EE;
	public static final int ACCENT_CYAN_B   = 0xFF67E8F9;
	public static final int ACCENT_RED_A    = 0xFFEF4444;
	public static final int ACCENT_RED_B    = 0xFFF87171;
	public static final int ACCENT_GREEN_A  = 0xFF22C55E;
	public static final int ACCENT_GREEN_B  = 0xFF86EFAC;
	public static final int ACCENT_AMBER_A  = 0xFFF59E0B;
	public static final int ACCENT_AMBER_B  = 0xFFFCD34D;
	public static final int ACCENT_PINK_A   = 0xFFEC4899;
	public static final int ACCENT_PINK_B   = 0xFFF9A8D4;

	public static int accentA() {
		return accentPair()[0];
	}

	public static int accentB() {
		return accentPair()[1];
	}

	/**
	 * Returns the current [primary, secondary] accent pair selected by the
	 * HUD module. Falls back to the purple Qwant accent.
	 */
	public static int[] accentPair() {
		HudModule hud = findHud();
		if (hud == null) return new int[] { ACCENT_PURPLE_A, ACCENT_PURPLE_B };

		String mode = hud.accentMode != null ? hud.accentMode.getCurrent() : "Purple";
		switch (mode) {
			case "Cyan":   return new int[] { ACCENT_CYAN_A,  ACCENT_CYAN_B };
			case "Red":    return new int[] { ACCENT_RED_A,   ACCENT_RED_B };
			case "Green":  return new int[] { ACCENT_GREEN_A, ACCENT_GREEN_B };
			case "Amber":  return new int[] { ACCENT_AMBER_A, ACCENT_AMBER_B };
			case "Pink":   return new int[] { ACCENT_PINK_A,  ACCENT_PINK_B };
			case "Chroma": {
				int a = ColorUtil.chroma(6f, 0.85f, 1f, 0f);
				int b = ColorUtil.chroma(6f, 0.85f, 1f, 0.15f);
				return new int[] { a, b };
			}
			case "Purple":
			default:       return new int[] { ACCENT_PURPLE_A, ACCENT_PURPLE_B };
		}
	}

	/** Per-module deterministic accent color (used by ArrayList stripes). */
	public static int accentColorFor(Module mod) {
		HudModule hud = findHud();
		if (hud != null && hud.accentMode != null
			&& "Chroma".equals(hud.accentMode.getCurrent())) {
			float hue = (Math.abs(mod.getName().hashCode()) % 360) / 360f;
			float t = (System.currentTimeMillis() % 6000L) / 6000f;
			float h = (hue + t * 0.15f) % 1f;
			return java.awt.Color.HSBtoRGB(h, 0.75f, 1f) | 0xFF000000;
		}
		return accentA();
	}

	public static HudModule findHud() {
		for (Module mod : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (mod instanceof HudModule h) return h;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T findRender(Class<T> cls) {
		for (Module mod : ModuleManager.getInstance().getModulesByCategory(Category.RENDER)) {
			if (cls.isInstance(mod)) return (T) mod;
		}
		return null;
	}

	// === Health =============================================================

	public static int healthAccent(float ratio) {
		if (ratio >= 0.66f) return 0xFF4ADE80; // green
		if (ratio >= 0.33f) return 0xFFFACC15; // yellow
		return 0xFFEF4444;                     // red
	}

	public static int brighten(int color, float factor) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int) ((color & 0xFF) * factor));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// === Status effect helpers =============================================

	/** Hex color from the effect type's particle/registry color. */
	public static int effectColor(StatusEffectInstance eff) {
		try {
			RegistryEntry<StatusEffect> entry = eff.getEffectType();
			int rgb = entry.value().getColor();
			return (rgb & 0x00FFFFFF) | 0xFF000000;
		} catch (Throwable t) {
			return 0xFFB14CFF;
		}
	}

	/**
	 * Draws an "icon tile" for an effect: a rounded square in the effect's
	 * own color with a darker stroke. Used as a visual proxy for the
	 * vanilla effect sprite without depending on the
	 * StatusEffectSpriteManager API (which has shifted between versions).
	 */
	public static void drawEffectTile(Matrix4f m, StatusEffectInstance eff,
	                                  float x, float y, float size, float alpha) {
		int color = effectColor(eff);
		Render2D.fillRoundedRect(m, x, y, size, size, 3f,
			ColorUtil.withAlpha(color, alpha));
		Render2D.fillRoundedRect(m, x + 1f, y + 1f, size - 2f, size - 2f, 2f,
			ColorUtil.withAlpha(brighten(color, 1.25f), alpha * 0.85f));
		Render2D.strokeRoundedRect(m, x, y, size, size, 3f,
			ColorUtil.withAlpha(0xFF1A1A1F, alpha));
	}

	/**
	 * Draws an "avatar tile" for an entity: a rounded square in the accent
	 * color with the first letter of the entity's name. Used as a stand-in
	 * for player heads without depending on the SkinTextures API.
	 */
	public static void drawEntityAvatar(net.minecraft.client.gui.DrawContext ctx,
	                                    Matrix4f m, Entity ent,
	                                    float x, float y, float size, float alpha) {
		int color = isPlayer(ent) ? accentA() : 0xFF6B7280;
		Render2D.fillRoundedRect(m, x, y, size, size, 3f,
			ColorUtil.withAlpha(color, alpha));
		Render2D.strokeRoundedRect(m, x, y, size, size, 3f,
			ColorUtil.withAlpha(brighten(color, 1.4f), alpha));

		String name = ent.getName().getString();
		String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
		var tr = MinecraftClient.getInstance().textRenderer;
		int lw = tr.getWidth(letter);
		ctx.drawText(tr, letter,
			(int) (x + (size - lw) / 2f),
			(int) (y + (size - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(0xFFFFFFFF, alpha), false);
	}

	// === Pretty rendering ==================================================

	/**
	 * Convenience: panel background with stroke and soft glow. Used by
	 * every widget so they share the same look.
	 */
	public static void panel(Matrix4f m, float x, float y, float w, float h,
	                         float radius, int accent, float alpha) {
		Render2D.glow(m, x, y, w, h, radius,
			ColorUtil.withAlpha(accent, 0.30f * alpha), 6f);
		Render2D.fillRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(0xEE0E0E14, alpha));
		Render2D.strokeRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(accent, 0.40f * alpha));
	}

	/**
	 * Filled progress bar with a 1px dark background. The bar itself is
	 * a horizontal gradient between {@code colorA} and {@code colorB}.
	 */
	public static void gradientBar(Matrix4f m, float x, float y, float w, float h,
	                               float radius, int colorA, int colorB, float alpha) {
		Render2D.fillRoundedRect(m, x, y, w, h, radius,
			ColorUtil.withAlpha(0xFF1A1A1F, alpha));
		Render2D.fillGradientH(m, x + 1f, y + 1f,
			Math.max(0f, w - 2f), Math.max(0f, h - 2f),
			ColorUtil.withAlpha(colorA, alpha),
			ColorUtil.withAlpha(colorB, alpha));
	}

	// === Style modes (string ids reused everywhere) ========================

	public static String mode(ModeSetting setting, String fallback) {
		return setting != null ? setting.getCurrent() : fallback;
	}

	// === Misc utilities ====================================================

	/** Best-effort distance from the local player to another entity. */
	public static float distanceTo(LivingEntity other) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || other == null) return 0f;
		return mc.player.distanceTo(other);
	}

	/** True if the entity is the local player. */
	public static boolean isLocalPlayer(Entity ent) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return ent != null && ent == mc.player;
	}

	/** Best-effort armor value for a living entity (0..20). */
	public static int armorOf(LivingEntity ent) {
		try {
			return ent.getArmor();
		} catch (Throwable t) {
			return 0;
		}
	}

	public static String safeName(LivingEntity ent) {
		try {
			if (ent.getDisplayName() != null) return ent.getDisplayName().getString();
			return ent.getName().getString();
		} catch (Throwable t) {
			return "?";
		}
	}

	/** True if the entity is a real player rather than a mob. */
	public static boolean isPlayer(Entity ent) {
		return ent instanceof PlayerEntity;
	}

	/** Roman numeral 1..5 (anything higher is rendered as a number). */
	public static String roman(int n) {
		switch (n) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(n);
		}
	}
}
