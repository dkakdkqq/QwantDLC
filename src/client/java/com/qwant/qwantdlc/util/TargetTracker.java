package com.qwant.qwantdlc.util;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

/**
 * Tracks the last entity the local player attacked. Used by TargetHUD.
 *
 * The event is non-cancelling — we always return ActionResult.PASS so the
 * vanilla attack still happens; we just record who was hit and when.
 */
public final class TargetTracker {
	private static LivingEntity target;
	private static long lastAttackMs = 0L;

	private TargetTracker() {}

	public static void register() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof LivingEntity living) {
				target = living;
				lastAttackMs = System.currentTimeMillis();
			}
			return ActionResult.PASS;
		});
	}

	public static LivingEntity getTarget() {
		// Forget the target if it was hit too long ago, was removed, or died.
		if (target == null) return null;
		if (target.isRemoved() || target.getHealth() <= 0f) return null;
		if (System.currentTimeMillis() - lastAttackMs > 4000L) return null;
		return target;
	}

	public static long getLastAttackMs() {
		return lastAttackMs;
	}
}
