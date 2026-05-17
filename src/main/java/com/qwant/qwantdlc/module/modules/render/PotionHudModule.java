package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

public class PotionHudModule extends Module {
	public PotionHudModule() {
		super("PotionHud", "Список активных эффектов с прогрессом", Category.RENDER);
		setToggled(true);
	}
}
