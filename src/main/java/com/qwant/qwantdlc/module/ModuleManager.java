package com.qwant.qwantdlc.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
	private static final ModuleManager INSTANCE = new ModuleManager();

	public static ModuleManager getInstance() {
		return INSTANCE;
	}

	private final List<Module> modules = new ArrayList<>();
	private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);

	private ModuleManager() {
		for (Category c : Category.values()) {
			byCategory.put(c, new ArrayList<>());
		}
	}

	public void register(Module module) {
		modules.add(module);
		byCategory.get(module.getCategory()).add(module);
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getModulesByCategory(Category category) {
		return Collections.unmodifiableList(byCategory.get(category));
	}

	public void onTick() {
		for (Module m : modules) {
			if (m.isToggled()) {
				m.onTick();
			}
		}
	}

	public void onKey(int key) {
		if (key == 0) return;
		for (Module m : modules) {
			if (m.getKey() == key) {
				m.toggle();
			}
		}
	}
}
