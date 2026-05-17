package com.qwant.qwantdlc.anim;

/**
 * Easing curves for animations.
 * Inputs and outputs are in [0, 1].
 */
public final class Easing {
	private Easing() {}

	@FunctionalInterface
	public interface Function {
		float apply(float t);
	}

	public static final Function LINEAR = t -> t;

	public static final Function EASE_OUT_QUAD = t -> 1f - (1f - t) * (1f - t);

	public static final Function EASE_OUT_CUBIC = t -> {
		float p = 1f - t;
		return 1f - p * p * p;
	};

	public static final Function EASE_OUT_QUART = t -> {
		float p = 1f - t;
		return 1f - p * p * p * p;
	};

	public static final Function EASE_OUT_EXPO = t ->
		t == 1f ? 1f : 1f - (float) Math.pow(2, -10f * t);

	public static final Function EASE_IN_OUT_QUAD = t ->
		t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;

	public static final Function EASE_IN_OUT_CUBIC = t ->
		t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;

	public static final Function EASE_IN_OUT_QUART = t -> {
		if (t < 0.5f) return 8f * t * t * t * t;
		float p = -2f * t + 2f;
		return 1f - (p * p * p * p) / 2f;
	};

	/** Springy overshoot — nice for popping cards. */
	public static final Function EASE_OUT_BACK = t -> {
		final float c1 = 1.70158f;
		final float c3 = c1 + 1f;
		float p = t - 1f;
		return 1f + c3 * p * p * p + c1 * p * p;
	};
}
