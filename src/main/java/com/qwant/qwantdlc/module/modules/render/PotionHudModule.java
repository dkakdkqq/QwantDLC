package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;

/**
 * PotionHUD widget — list of active status effects with colored tile and
 * remaining duration. Multiple visual modes:
 *
 *   - Modern    — full panel, rows with tile + name + duration
 *   - Nursultan — slim panel, accent stripe per row
 *   - Minced    — compact rows with bracket-style duration
 *   - Nix       — text-only, no panel, drops shadow
 */
public class PotionHudModule extends Module {
	public final ModeSetting style;
	public final BoolSetting showTile;
	public final BoolSetting showAmplifier;
	public final BoolSetting showDuration;
	public final BoolSetting hideAmbient;

	public PotionHudModule() {
		super("PotionHud", "Активные эффекты с прогрессом и временем",
			Category.RENDER);
		setToggled(true);

		this.style          = addSetting(new ModeSetting("Style",
			0, "Modern", "Nursultan", "Minced", "Nix"));
		this.showTile       = addSetting(new BoolSetting("Tile", true));
		this.showAmplifier  = addSetting(new BoolSetting("Amplifier", true));
		this.showDuration   = addSetting(new BoolSetting("Duration", true));
		this.hideAmbient    = addSetting(new BoolSetting("Hide Ambient", false));
	}
}
