package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

/**
 * Master HUD toggle. The HUD itself is drawn by {@link
 * com.qwant.qwantdlc.render.HudRenderer} in the Desktop style: rounded pill
 * tiles in the top-left for brand / user / FPS / time, and bottom info
 * rows for XYZ / b/s / ping.
 */
public class HudModule extends Module {
	public HudModule() {
		super("Hud", "Отображает плашки HUD: брэнд, пользователь, FPS, время, XYZ, скорость, пинг",
			Category.RENDER);
		setToggled(true);
	}
}
