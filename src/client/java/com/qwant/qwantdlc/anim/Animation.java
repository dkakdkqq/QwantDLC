package com.qwant.qwantdlc.anim;

/**
 * Time-based 0..1 animation driver. Call {@link #setTarget(float)} to choose
 * the destination (0 or 1) and {@link #update()} every frame to advance the
 * eased value. Wall-clock based, so it's frame-rate independent.
 */
public class Animation {
	private final float durationMs;
	private final Easing.Function ease;

	private float current;
	private float target;
	private long lastUpdateMs = -1L;

	public Animation(float durationMs, Easing.Function ease, float initial) {
		this.durationMs = Math.max(1f, durationMs);
		this.ease = ease == null ? Easing.LINEAR : ease;
		this.current = clamp(initial);
		this.target = this.current;
	}

	public Animation(float durationMs, Easing.Function ease) {
		this(durationMs, ease, 0f);
	}

	public Animation(float durationMs) {
		this(durationMs, Easing.EASE_OUT_QUART, 0f);
	}

	public void setTarget(float t) {
		this.target = clamp(t);
	}

	public void setTargetBool(boolean active) {
		setTarget(active ? 1f : 0f);
	}

	public float getRaw() {
		return current;
	}

	public boolean done() {
		return Math.abs(current - target) < 1e-4f;
	}

	/** Advance animation; returns the eased value. */
	public float update() {
		long now = System.currentTimeMillis();
		if (lastUpdateMs < 0) lastUpdateMs = now;
		float dt = (now - lastUpdateMs) / durationMs;
		lastUpdateMs = now;

		if (current < target) {
			current = Math.min(target, current + dt);
		} else if (current > target) {
			current = Math.max(target, current - dt);
		}
		return ease.apply(clamp(current));
	}

	/** Eased value without advancing. */
	public float getValue() {
		return ease.apply(clamp(current));
	}

	private static float clamp(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}
