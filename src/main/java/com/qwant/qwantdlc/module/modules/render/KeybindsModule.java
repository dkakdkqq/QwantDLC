package com.qwant.qwantdlc.module.modules.render;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

public class KeybindsModule extends Module {
	public KeybindsModule() {
		super("Keybinds", "Виджет со списком привязанных клавиш", Category.RENDER);
		setToggled(true);
	}
}
