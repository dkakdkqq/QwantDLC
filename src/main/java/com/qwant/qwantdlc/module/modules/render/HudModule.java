package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;

/**
 * Master HUD toggle. Hosts per-widget on/off switches and a small accent
 * picker that the renderer reads at draw time.
 *
 * The actual drawing lives in {@link com.qwant.qwantdlc.render.HudRenderer}.
 */
public class HudModule extends Module {
	public final BoolSetting watermark;
	public final BoolSetting arrayList;
	public final BoolSetting infoPill;
	public final BoolSetting coordsHud;
	public final BoolSetting keybinds;
	public final BoolSetting targetHud;
	public final BoolSetting potionHud;
	public final BoolSetting welcomePopup;
	public final BoolSetting notifications;
	public final ModeSetting accent;

	public HudModule() {
		super("Hud", "Watermark, ArrayList, Info, Keybinds, TargetHUD, PotionHUD, etc.",
			Category.RENDER);
		setToggled(true);

		this.watermark     = addSetting(new BoolSetting("Watermark",     true));
		this.arrayList     = addSetting(new BoolSetting("ArrayList",     true));
		this.infoPill      = addSetting(new BoolSetting("Info Pill",     true));
		this.coordsHud     = addSetting(new BoolSetting("Coords",        true));
		this.keybinds      = addSetting(new BoolSetting("Keybinds",      true));
		this.targetHud     = addSetting(new BoolSetting("TargetHUD",     true));
		this.potionHud     = addSetting(new BoolSetting("PotionHUD",     true));
		this.welcomePopup  = addSetting(new BoolSetting("Welcome Popup", true));
		this.notifications = addSetting(new BoolSetting("Notifications", true));

		this.accent = addSetting(new ModeSetting("Accent", 0,
			"Purple", "Pink", "Cyan", "Green", "Amber", "Red", "Chroma"));
	}
}
