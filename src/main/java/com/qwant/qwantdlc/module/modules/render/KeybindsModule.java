package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;

/**
 * Keybinds widget — lists every module with a keybind set.
 *
 *   - Modern    — panel with title + divider + rows
 *   - Nursultan — panel with accent stripe per row
 *   - Minced    — compact "Module [KEY]" rows, no panel
 *   - Nix       — outlined card with bracket-style key labels
 *   - Bracket   — minimal "Module: KEY" lines
 */
public class KeybindsModule extends Module {
	public final ModeSetting style;
	public final ModeSetting sort;
	public final BoolSetting showTitle;
	public final BoolSetting hideDisabled;

	public KeybindsModule() {
		super("Keybinds", "Список модулей с привязанными клавишами",
			Category.RENDER);
		setToggled(true);

		this.style        = addSetting(new ModeSetting("Style",
			0, "Modern", "Nursultan", "Minced", "Nix", "Bracket"));
		this.sort         = addSetting(new ModeSetting("Sort",
			0, "Alphabetical", "Width", "Category"));
		this.showTitle    = addSetting(new BoolSetting("Show Title", true));
		this.hideDisabled = addSetting(new BoolSetting("Hide Disabled", false));
	}
}
