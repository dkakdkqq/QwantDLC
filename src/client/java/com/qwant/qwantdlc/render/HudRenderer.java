package com.qwant.qwantdlc.render;

import java.util.Locale;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.render.HudModule;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

/**
 * Desktop-style HUD inspired by the example reference:
 *
 *   Top row — three small rounded pills:
 *     [Q] · [user]                · [fps]
 *     [time]
 *
 *   Bottom row:
 *     xyz: x.x, y.y, z.z          (left)
 *     b/s: speed                  (left, line above)
 *     ping: NN                    (right)
 *
 * Numeric values are smoothed with a simple low-pass to match the
 * AnimatedValue helper from the reference. Background panels are solid
 * rounded rects at ~170/255 alpha — the reference also drew a blur, but
 * we don't have a blur shader so we keep just the rounded rect.
 */
public final class HudRenderer {
	private HudRenderer() {}

	// --- Smoothed numeric readouts (mirrors AnimatedValue from the ref).
	private static final float SMOOTH = 0.15f;
	private static float fpsAnim, xAnim, yAnim, zAnim, speedAnim, pingAnim;

	private static long lastTimeMs = 0L;
	private static String cachedTimeText = "00:00";

	// Visual constants (kept close to the reference layout).
	private static final int PANEL_BG = 0xAA000000; // (0,0,0,170)
	private static final int TEXT_PRIMARY   = 0xFFFFFFFF;
	private static final int TEXT_SECONDARY = 0xFFB5B5BE;

	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			HudModule hud = findHud();
			if (hud == null || !hud.isToggled()) return;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.options.hudHidden) return;
			if (mc.player == null) return;

			Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
			int sw = ctx.getScaledWindowWidth();
			int sh = ctx.getScaledWindowHeight();

			updateAnimations(mc.player);

			drawTopPills(ctx, m);
			drawBottomInfo(ctx, m, sw, sh);
		});
	}

	// ---------------------------------------------------------------- helpers

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

		// Refresh "HH:mm" once per second so we don't hit the clock per frame.
		long now = System.currentTimeMillis();
		if (now - lastTimeMs > 1000L) {
			java.time.LocalTime t = java.time.LocalTime.now();
			cachedTimeText = String.format(Locale.ROOT, "%02d:%02d",
				t.getHour(), t.getMinute());
			lastTimeMs = now;
		}
	}

	private static float approach(float current, float target) {
		return current + (target - current) * SMOOTH;
	}

	/** Horizontal speed in blocks per second (XZ-plane velocity * 20 ticks). */
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

	// ----------------------------------------------------------- top pills row

	private static void drawTopPills(DrawContext ctx, Matrix4f m) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		// Pill 1: brand "Q" — 22x15 starting at (5,3).
		float p1x = 5f, p1y = 3f, p1w = 22f, p1h = 15f;
		Render2D.fillRoundedRect(m, p1x, p1y, p1w, p1h, 5f, PANEL_BG);
		drawTextCentered(ctx, tr, "Q", p1x, p1y, p1w, p1h, TEXT_PRIMARY);

		// Pill 2: user name — 70x15 starting at (28,3).
		float p2x = 28f, p2y = 3f, p2w = 70f, p2h = 15f;
		Render2D.fillRoundedRect(m, p2x, p2y, p2w, p2h, 5f, PANEL_BG);
		String userText = userName();
		// Trim to fit; left-padded with 5px.
		String userFitted = trimToWidth(tr, userText, (int) (p2w - 10f));
		ctx.drawText(tr, userFitted,
			(int) (p2x + 5f),
			(int) (p2y + (p2h - tr.fontHeight) / 2f + 1f),
			TEXT_PRIMARY, false);

		// Pill 3: FPS — 50x15 starting at (99,3).
		float p3x = 99f, p3y = 3f, p3w = 50f, p3h = 15f;
		Render2D.fillRoundedRect(m, p3x, p3y, p3w, p3h, 5f, PANEL_BG);
		String fpsText = Math.round(fpsAnim) + " fps";
		drawTextCentered(ctx, tr, fpsText, p3x, p3y, p3w, p3h, TEXT_PRIMARY);

		// Pill 4: time — 60x15 starting at (5,19).
		float p4x = 5f, p4y = 19f, p4w = 60f, p4h = 15f;
		Render2D.fillRoundedRect(m, p4x, p4y, p4w, p4h, 5f, PANEL_BG);
		String timeText = cachedTimeText + " time";
		drawTextCentered(ctx, tr, timeText, p4x, p4y, p4w, p4h, TEXT_PRIMARY);
	}

	// ---------------------------------------------------------- bottom info row

	private static void drawBottomInfo(DrawContext ctx, Matrix4f m, int sw, int sh) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		// Left labels in white, values in muted gray (matches the reference's
		// ChatFormatting.GRAY treatment).
		String xyzLabel = "xyz: ";
		String xyzValue = String.format(Locale.ROOT, "%.1f, %.1f, %.1f",
			xAnim, yAnim, zAnim);
		String speedLabel = "b/s: ";
		String speedValue = String.format(Locale.ROOT, "%.1f", speedAnim);
		String pingLabel = "ping: ";
		String pingValue = Integer.toString(Math.round(pingAnim));

		// Bottom-left: b/s on top, xyz under it (closest to the bottom).
		float baseY = sh - tr.fontHeight - 2f;
		drawLabelValue(ctx, tr, 1f, baseY, xyzLabel, xyzValue);

		float speedY = baseY - tr.fontHeight - 1f;
		drawLabelValue(ctx, tr, 1f, speedY, speedLabel, speedValue);

		// Bottom-right: ping on the same line as xyz.
		int pingW = tr.getWidth(pingLabel) + tr.getWidth(pingValue);
		float pingX = sw - pingW - 3f;
		drawLabelValue(ctx, tr, pingX, baseY, pingLabel, pingValue);
	}

	private static void drawLabelValue(DrawContext ctx, TextRenderer tr,
	                                   float x, float y,
	                                   String label, String value) {
		ctx.drawText(tr, label, (int) x, (int) y, TEXT_PRIMARY, true);
		ctx.drawText(tr, value,
			(int) (x + tr.getWidth(label)),
			(int) y, TEXT_SECONDARY, true);
	}

	private static void drawTextCentered(DrawContext ctx, TextRenderer tr,
	                                     String text,
	                                     float x, float y, float w, float h,
	                                     int color) {
		int tw = tr.getWidth(text);
		ctx.drawText(tr, text,
			(int) (x + (w - tw) / 2f),
			(int) (y + (h - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(color, 1f), false);
	}

	private static String userName() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			return mc.player.getGameProfile().getName();
		}
		return QwantDLC.MOD_NAME;
	}

	private static String trimToWidth(TextRenderer tr, String text, int maxPx) {
		if (tr.getWidth(text) <= maxPx) return text;
		String ellipsis = "…";
		int ew = tr.getWidth(ellipsis);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			sb.append(text.charAt(i));
			if (tr.getWidth(sb.toString()) + ew > maxPx) {
				if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
				return sb.toString() + ellipsis;
			}
		}
		return text;
	}
}
