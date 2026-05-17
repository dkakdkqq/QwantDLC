package com.qwant.qwantdlc.module.modules.themes;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;

public class DarkThemeModule extends Module {
	public DarkThemeModule() {
		super("Dark", "Тёмная тема интерфейса", Category.THEMES);
		setToggled(true);
	}
}
