package com.qwant.qwantdlc.gui;

import com.qwant.qwantdlc.render.Render2D;

/**
 * Color palette for the strict-black themed ClickGUI.
 * Constants are ARGB integers (0xAARRGGBB).
 */
public final class Theme {
	private Theme() {}

	/** Deep black panel background. */
	public static final int PANEL_BG       = 0xEE0A0A0A;
	/** Slightly lighter header. */
	public static final int PANEL_HEADER   = 0xFF1A1A1A;
	/** Thin border. */
	public static final int PANEL_BORDER   = 0xFF2A2A2A;
	/** Lifted background of header on hover. */
	public static final int PANEL_HEADER_HOVER = 0xFF222222;

	/** Module row background. */
	public static final int MODULE_BG      = 0x00000000;
	public static final int MODULE_HOVER   = 0x33FFFFFF;
	public static final int MODULE_ACTIVE  = 0xFF1F6FEB;

	/** Text. */
	public static final int TEXT_PRIMARY   = 0xFFFFFFFF;
	public static final int TEXT_SECONDARY = 0xFFB0B0B0;
	public static final int TEXT_ACCENT    = 0xFF1F6FEB;

	/** Common geometry. */
	public static final float PANEL_RADIUS = 6f;
	public static final float PANEL_WIDTH  = 110f;
	public static final float HEADER_HEIGHT = 18f;
	public static final float ROW_HEIGHT   = 14f;

	/** Helper to wrap Render2D.argb. */
	public static int argb(int a, int r, int g, int b) {
		return Render2D.argb(a, r, g, b);
	}
}
