package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;

/**
 * Master HUD toggle. Hosts the global accent color and per-widget on/off
 * switches that aren't worth a full module of their own (watermark style,
 * arraylist style, info pill, welcome popup).
 *
 * Per-widget detail settings (target hud style, potion hud style, keybinds
 * style, etc.) live in their own modules so each widget is configurable
 * from the ClickGUI in isolation.
 */
public class HudModule extends Module {
	/** Watermark visual style. */
	public final ModeSetting watermarkMode;
	/** ArrayList visual style. */
	public final ModeSetting arrayListMode;
	/** Sort order for ArrayList entries. */
	public final ModeSetting arrayListSort;
	/** Global accent color used by every widget. */
	public final ModeSetting accentMode;

	/** Show the bottom-left info pill (user/FPS/XYZ). */
	public final BoolSetting infoPill;
	/** Show the watermark in the top-left. */
	public final BoolSetting watermark;
	/** Show the welcome popup on world join. */
	public final BoolSetting welcomePopup;
	/** Show the ArrayList of enabled modules. */
	public final BoolSetting arrayList;

	public HudModule() {
		super("Hud", "Главный модуль HUD: водяной знак, ArrayList, плашка с информацией",
			Category.RENDER);
		setToggled(true);

		this.watermarkMode = addSetting(new ModeSetting("Watermark Style",
			0, "Pill", "Bracket", "Minimal", "Nursultan"));
		this.arrayListMode = addSetting(new ModeSetting("ArrayList Style",
			0, "Modern", "Nursultan", "Minced", "Nix", "Outline", "Bracket"));
		this.arrayListSort = addSetting(new ModeSetting("ArrayList Sort",
			0, "Width", "Alphabetical", "Length"));
		this.accentMode = addSetting(new ModeSetting("Accent",
			0, "Purple", "Pink", "Cyan", "Red", "Green", "Amber", "Chroma"));

		this.infoPill = addSetting(new BoolSetting("Info Pill", true));
		this.watermark = addSetting(new BoolSetting("Watermark", true));
		this.welcomePopup = addSetting(new BoolSetting("Welcome Popup", true));
		this.arrayList = addSetting(new BoolSetting("ArrayList", true));
	}
}
