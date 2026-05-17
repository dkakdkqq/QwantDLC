package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;
import com.qwant.qwantdlc.setting.SliderSetting;

/**
 * TargetHUD widget — shows information about the last entity the player
 * attacked. Multiple visual modes inspired by popular clients.
 *
 *   - Modern    — clean dark panel, gradient health bar, inline avatar
 *   - Nursultan — wider panel with prominent avatar and accent stripe
 *   - Minced    — compact pill, big name, thin underline bar
 *   - Nix       — outlined card with chroma stroke
 */
public class TargetHudModule extends Module {
	public final ModeSetting style;
	public final BoolSetting showAvatar;
	public final BoolSetting showDistance;
	public final BoolSetting showArmor;
	public final BoolSetting showHealthText;
	public final SliderSetting scale;

	public TargetHudModule() {
		super("TargetHud", "Информация о последней цели: имя, здоровье, броня",
			Category.RENDER);
		setToggled(true);

		this.style          = addSetting(new ModeSetting("Style",
			0, "Modern", "Nursultan", "Minced", "Nix"));
		this.showAvatar     = addSetting(new BoolSetting("Avatar", true));
		this.showDistance   = addSetting(new BoolSetting("Distance", true));
		this.showArmor      = addSetting(new BoolSetting("Armor", true));
		this.showHealthText = addSetting(new BoolSetting("Health Text", true));
		this.scale          = addSetting(new SliderSetting("Scale",
			1.0f, 0.75f, 1.5f, 2));
	}
}
