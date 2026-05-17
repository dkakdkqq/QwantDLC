package com.qwant.qwantdlc.module.modules.combat;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;
import com.qwant.qwantdlc.setting.MultiSelectSetting;
import com.qwant.qwantdlc.setting.SliderSetting;

public class AttackAuraModule extends Module {
	public final SliderSetting range;
	public final SliderSetting vision;
	public final ModeSetting rotations;
	public final MultiSelectSetting target;
	public final MultiSelectSetting movementCorrection;
	public final MultiSelectSetting extras;

	public AttackAuraModule() {
		super("AttackAura", "Автоматически бьёт ближайших противников", Category.COMBAT);

		this.range = addSetting(new SliderSetting("Range",  4.5f, 3.0f, 6.0f, 1));
		this.vision = addSetting(new SliderSetting("Vision", 4.0f, 2.5f, 6.0f, 1));

		this.rotations = addSetting(new ModeSetting(
			"Rotations", 0,
			"SpookyTime", "HolyWorld", "ReallyWorld", "FunTime"
		));

		this.target = addSetting(new MultiSelectSetting(
			"Target",
			new String[] {"Игроков", "Животных", "Мобов", "Друзей"},
			new boolean[] { true,      false,      false,    false }
		));

		this.movementCorrection = addSetting(new MultiSelectSetting(
			"Коррекция движения",
			new String[] {"Свободная", "Таргетированная"},
			new boolean[] { true,        false }
		));

		this.extras = addSetting(new MultiSelectSetting(
			"Доп. настройки",
			new String[] {
				"Не бить если ешь",
				"Бить только с оружием",
				"Не бить в инвентаре",
				"Бить только критами"
			},
			new boolean[] { true, false, true, false }
		));
	}
}
