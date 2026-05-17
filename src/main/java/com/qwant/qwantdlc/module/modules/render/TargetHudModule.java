package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

public class TargetHudModule extends Module {
	public TargetHudModule() {
		super("TargetHud", "Информация о последней цели атаки", Category.RENDER);
		setToggled(true);
	}
}
