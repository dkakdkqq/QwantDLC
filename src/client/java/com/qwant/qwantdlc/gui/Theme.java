package com.qwant.qwantdlc.gui;

import com.qwant.qwantdlc.render.Render2D;

/**
 * Color palette for the Qwant ClickGUI (Celestial-inspired layout).
 * Constants are ARGB integers (0xAARRGGBB).
 */
public final class Theme {
	private Theme() {}

	// === Window ===
	/** Main GUI background (deep black). */
	public static final int WINDOW_BG       = 0xEE0A0A0A;
	/** Sidebar slightly darker than the body. */
	public static final int SIDEBAR_BG      = 0xFF0F0F12;
	/** Main body of the window. */
	public static final int BODY_BG         = 0xFF131316;
	/** 1px border around the window. */
	public static final int WINDOW_BORDER   = 0xFF2A2A2D;

	// === Sidebar items ===
	public static final int SIDEBAR_ITEM_BG       = 0x00000000;
	public static final int SIDEBAR_ITEM_HOVER    = 0xFF1B1B1F;
	/** Selected category, pinkish like in the reference. */
	public static final int SIDEBAR_ITEM_ACTIVE   = 0xFF8A2BE2;
	/** Active gradient endpoint. */
	public static final int SIDEBAR_ITEM_ACTIVE_2 = 0xFFB14CFF;

	// === Cards (modules) ===
	public static final int CARD_BG         = 0xFF1A1A1E;
	public static final int CARD_BG_HOVER   = 0xFF222227;
	/** Pink/violet active card background (matches reference screenshot). */
	public static final int CARD_BG_ACTIVE  = 0xFF8A2BE2;
	public static final int CARD_BORDER     = 0xFF2A2A2F;

	// === Search bar ===
	public static final int SEARCH_BG       = 0xFF1A1A1E;
	public static final int SEARCH_BORDER   = 0xFF2A2A2F;

	// === Text ===
	public static final int TEXT_PRIMARY    = 0xFFFFFFFF;
	public static final int TEXT_SECONDARY  = 0xFFB0B0B5;
	public static final int TEXT_MUTED      = 0xFF7A7A80;
	public static final int TEXT_ACCENT     = 0xFFB14CFF;

	// === Geometry ===
	public static final float WINDOW_WIDTH   = 460f;
	public static final float WINDOW_HEIGHT  = 280f;
	public static final float WINDOW_RADIUS  = 10f;

	public static final float SIDEBAR_WIDTH  = 130f;
	public static final float SIDEBAR_ITEM_HEIGHT = 24f;
	public static final float SIDEBAR_ITEM_RADIUS = 6f;

	public static final float HEADER_HEIGHT  = 36f;
	public static final float SEARCH_HEIGHT  = 22f;
	public static final float SEARCH_RADIUS  = 6f;

	public static final float CARD_RADIUS    = 6f;
	public static final float CARD_HEIGHT    = 44f;
	public static final float CARD_GAP       = 6f;

	// === Backwards-compat aliases (used by the HUD renderer). ===
	public static final int PANEL_BG        = WINDOW_BG;
	public static final int PANEL_HEADER    = SIDEBAR_BG;
	public static final int PANEL_BORDER    = WINDOW_BORDER;

	public static int argb(int a, int r, int g, int b) {
		return Render2D.argb(a, r, g, b);
	}
}
