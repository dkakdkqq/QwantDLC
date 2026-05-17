package com.qwant.qwantdlc;

import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.module.modules.combat.AttackAuraModule;
import com.qwant.qwantdlc.module.modules.combat.AutoAttackModule;
import com.qwant.qwantdlc.module.modules.misc.AutoRespawnModule;
import com.qwant.qwantdlc.module.modules.movement.FlyModule;
import com.qwant.qwantdlc.module.modules.movement.SprintModule;
import com.qwant.qwantdlc.module.modules.player.NoFallModule;
import com.qwant.qwantdlc.module.modules.render.EspModule;
import com.qwant.qwantdlc.module.modules.render.HudModule;
import com.qwant.qwantdlc.module.modules.render.KeybindsModule;
import com.qwant.qwantdlc.module.modules.render.PotionHudModule;
import com.qwant.qwantdlc.module.modules.render.TargetHudModule;
import com.qwant.qwantdlc.module.modules.themes.DarkThemeModule;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QwantDLC implements ModInitializer {
	public static final String MOD_ID = "qwantdlc";
	public static final String MOD_NAME = "QwantDLC";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] Initializing common entrypoint...", MOD_NAME);

		ModuleManager mm = ModuleManager.getInstance();
		// Combat
		mm.register(new AttackAuraModule());
		mm.register(new AutoAttackModule());
		// Movement
		mm.register(new FlyModule());
		mm.register(new SprintModule());
		// Render
		mm.register(new HudModule());
		mm.register(new EspModule());
		mm.register(new TargetHudModule());
		mm.register(new PotionHudModule());
		mm.register(new KeybindsModule());
		// Player
		mm.register(new NoFallModule());
		// Misc
		mm.register(new AutoRespawnModule());
		// Themes
		mm.register(new DarkThemeModule());

		LOGGER.info("[{}] Registered {} modules.", MOD_NAME, mm.getModules().size());
	}
}
