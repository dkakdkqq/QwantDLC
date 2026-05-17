package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;

/**
 * Master HUD toggle. Hosts per-widget on/off switches that the renderer
 * reads to decide what to draw.
 *
 * The actual drawing lives in {@link com.qwant.qwantdlc.render.HudRenderer}.
 * Style is fixed (Minced-inspired): purple accent, dark rounded panels,
 * slide-in animations.
 */
public class HudModule extends Module {
	public final BoolSetting watermark;
	public final BoolSetting arrayList;
	public final BoolSetting infoPill;
	public final BoolSetting targetHud;
	public final BoolSetting potionHud;

	public HudModule() {
		super("Hud", "Watermark, ArrayList, Info, TargetHUD, PotionHUD",
			Category.RENDER);
		setToggled(true);

		this.watermark = addSetting(new BoolSetting("Watermark", true));
		this.arrayList = addSetting(new BoolSetting("ArrayList", true));
		this.infoPill  = addSetting(new BoolSetting("Info Pill", true));
		this.targetHud = addSetting(new BoolSetting("TargetHUD", true));
		this.potionHud = addSetting(new BoolSetting("PotionHUD", true));
	}
}
