package com.qwant.qwantdlc.anim;

import java.awt.Color;

public final class ColorUtil {
	private ColorUtil() {}

	/** ARGB linear interpolation. */
	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		int oa = (int) (aa + (ba - aa) * t);
		int or = (int) (ar + (br - ar) * t);
		int og = (int) (ag + (bg - ag) * t);
		int ob = (int) (ab + (bb - ab) * t);
		return (oa << 24) | (or << 16) | (og << 8) | ob;
	}

	/** Multiply alpha channel by a scalar. */
	public static int withAlpha(int color, float alphaScale) {
		int a = (color >> 24) & 0xFF;
		int na = Math.max(0, Math.min(255, (int) (a * Math.max(0f, Math.min(1f, alphaScale)))));
		return (na << 24) | (color & 0x00FFFFFF);
	}

	public static int rgba(int r, int g, int b, int a) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	/** Returns an HSB color rotating with time, useful for chroma accents. */
	public static int chroma(float speedSec, float saturation, float brightness, float offset) {
		float t = (System.currentTimeMillis() % (long) (speedSec * 1000f)) / (speedSec * 1000f);
		t = (t + offset) % 1f;
		return Color.HSBtoRGB(t, saturation, brightness) | 0xFF000000;
	}

	/** Smooth two-color sweep over time (sin-based). */
	public static int sweep(int colorA, int colorB, float speedSec) {
		float t = (System.currentTimeMillis() % (long) (speedSec * 1000f)) / (speedSec * 1000f);
		float k = (float) (0.5 - 0.5 * Math.cos(t * Math.PI * 2));
		return lerp(colorA, colorB, k);
	}
}
