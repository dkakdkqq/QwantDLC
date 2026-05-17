package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

public class HudModule extends Module {
	public HudModule() {
		super("Hud", "Отображает водяной знак и список включённых модулей", Category.RENDER);
		// Enabled by default so the watermark is visible at first launch.
		setToggled(true);
	}
}
