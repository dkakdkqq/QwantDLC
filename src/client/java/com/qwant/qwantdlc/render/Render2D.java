package com.qwant.qwantdlc.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import org.joml.Matrix4f;

/**
 * Custom immediate-mode 2D renderer.
 * Wraps Tessellator/BufferBuilder + RenderSystem to draw filled rectangles,
 * outlines, rounded rectangles, and lines using the position-color shader.
 *
 * All public methods accept ARGB colors (0xAARRGGBB).
 */
public final class Render2D {
	private Render2D() {}

	public static int argb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static float a(int c) { return ((c >> 24) & 0xFF) / 255f; }
	private static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
	private static float g(int c) { return ((c >> 8) & 0xFF) / 255f; }
	private static float b(int c) { return (c & 0xFF) / 255f; }

	private static void enableBlend() {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}

	private static void disableBlend() {
		RenderSystem.disableBlend();
	}

	private static void setShader() {
		// 1.21.4: ShaderProgramKeys are how vanilla refers to position+color program.
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
	}

	/**
	 * Filled rectangle [x..x+w, y..y+h].
	 */
	public static void fillRect(Matrix4f matrix, float x, float y, float w, float h, int color) {
		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		enableBlend();
		setShader();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		buf.vertex(matrix, x, y, 0f).color(fr, fg, fb, fa);
		buf.vertex(matrix, x, y + h, 0f).color(fr, fg, fb, fa);
		buf.vertex(matrix, x + w, y + h, 0f).color(fr, fg, fb, fa);
		buf.vertex(matrix, x + w, y, 0f).color(fr, fg, fb, fa);

		BufferRenderer.drawWithGlobalProgram(buf.end());
		disableBlend();
	}

	/**
	 * Vertical gradient fill (color1 on top, color2 on bottom).
	 */
	public static void fillGradient(Matrix4f matrix, float x, float y, float w, float h, int color1, int color2) {
		enableBlend();
		setShader();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		buf.vertex(matrix, x, y, 0f).color(r(color1), g(color1), b(color1), a(color1));
		buf.vertex(matrix, x, y + h, 0f).color(r(color2), g(color2), b(color2), a(color2));
		buf.vertex(matrix, x + w, y + h, 0f).color(r(color2), g(color2), b(color2), a(color2));
		buf.vertex(matrix, x + w, y, 0f).color(r(color1), g(color1), b(color1), a(color1));

		BufferRenderer.drawWithGlobalProgram(buf.end());
		disableBlend();
	}

	/**
	 * 1px rectangle outline (4 thin filled rectangles).
	 */
	public static void outline(Matrix4f matrix, float x, float y, float w, float h, float thickness, int color) {
		fillRect(matrix, x, y, w, thickness, color);                          // top
		fillRect(matrix, x, y + h - thickness, w, thickness, color);          // bottom
		fillRect(matrix, x, y + thickness, thickness, h - 2 * thickness, color); // left
		fillRect(matrix, x + w - thickness, y + thickness, thickness, h - 2 * thickness, color); // right
	}

	/**
	 * Filled rectangle with rounded corners.
	 * Uses a triangle fan per corner; the rest is filled by 3 inner rectangles.
	 *
	 * Layout:
	 *   - center band:  full width, height = h - 2*radius
	 *   - top band:     x+r..x+w-r, height = radius
	 *   - bottom band:  x+r..x+w-r, height = radius
	 *   - 4 corner fans
	 */
	public static void fillRoundedRect(Matrix4f matrix, float x, float y, float w, float h, float radius, int color) {
		float maxR = Math.min(w, h) / 2f;
		float r = Math.max(0f, Math.min(radius, maxR));

		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		// Inner bands
		fillRect(matrix, x + r, y, w - 2 * r, r, color);                 // top band
		fillRect(matrix, x, y + r, w, h - 2 * r, color);                 // center band (full width)
		fillRect(matrix, x + r, y + h - r, w - 2 * r, r, color);         // bottom band

		// 4 rounded corners as triangle fans
		fillCornerArc(matrix, x + r,         y + r,         r, 180f, 270f, fr, fg, fb, fa); // top-left
		fillCornerArc(matrix, x + w - r,     y + r,         r, 270f, 360f, fr, fg, fb, fa); // top-right
		fillCornerArc(matrix, x + w - r,     y + h - r,     r, 0f,   90f,  fr, fg, fb, fa); // bottom-right
		fillCornerArc(matrix, x + r,         y + h - r,     r, 90f,  180f, fr, fg, fb, fa); // bottom-left
	}

	/**
	 * Rounded rectangle with a 1px outline (drawn after the fill via stroke segments).
	 */
	public static void roundedRectWithOutline(Matrix4f matrix, float x, float y, float w, float h,
	                                          float radius, int fillColor, int outlineColor) {
		fillRoundedRect(matrix, x, y, w, h, radius, fillColor);
		strokeRoundedRect(matrix, x, y, w, h, radius, outlineColor);
	}

	/**
	 * 1px outline of a rounded rectangle. Implemented as 4 straight 1px rects
	 * and 4 quarter-arc strips (filled annular sectors of width 1px).
	 */
	public static void strokeRoundedRect(Matrix4f matrix, float x, float y, float w, float h,
	                                     float radius, int color) {
		float maxR = Math.min(w, h) / 2f;
		float r = Math.max(0f, Math.min(radius, maxR));

		// straight edges
		fillRect(matrix, x + r, y,             w - 2 * r, 1f, color); // top
		fillRect(matrix, x + r, y + h - 1f,    w - 2 * r, 1f, color); // bottom
		fillRect(matrix, x,         y + r,     1f, h - 2 * r, color); // left
		fillRect(matrix, x + w - 1f,y + r,     1f, h - 2 * r, color); // right

		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		strokeCornerArc(matrix, x + r,         y + r,         r, 180f, 270f, fr, fg, fb, fa);
		strokeCornerArc(matrix, x + w - r,     y + r,         r, 270f, 360f, fr, fg, fb, fa);
		strokeCornerArc(matrix, x + w - r,     y + h - r,     r, 0f,   90f,  fr, fg, fb, fa);
		strokeCornerArc(matrix, x + r,         y + h - r,     r, 90f,  180f, fr, fg, fb, fa);
	}

	/**
	 * Filled 90-degree arc (triangle fan) used for rounded corners.
	 */
	private static void fillCornerArc(Matrix4f matrix, float cx, float cy, float radius,
	                                  float fromDeg, float toDeg,
	                                  float fr, float fg, float fb, float fa) {
		final int segments = 16;

		enableBlend();
		setShader();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

		// center
		buf.vertex(matrix, cx, cy, 0f).color(fr, fg, fb, fa);
		for (int i = 0; i <= segments; i++) {
			float t = (float) i / (float) segments;
			float angle = (float) Math.toRadians(fromDeg + (toDeg - fromDeg) * t);
			float vx = cx + (float) Math.cos(angle) * radius;
			float vy = cy + (float) Math.sin(angle) * radius;
			buf.vertex(matrix, vx, vy, 0f).color(fr, fg, fb, fa);
		}

		BufferRenderer.drawWithGlobalProgram(buf.end());
		disableBlend();
	}

	/**
	 * 1px-wide arc stroke (annular sector) for rounded corner outlines.
	 */
	private static void strokeCornerArc(Matrix4f matrix, float cx, float cy, float radius,
	                                    float fromDeg, float toDeg,
	                                    float fr, float fg, float fb, float fa) {
		final int segments = 16;
		final float inner = Math.max(0f, radius - 1f);

		enableBlend();
		setShader();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

		for (int i = 0; i <= segments; i++) {
			float t = (float) i / (float) segments;
			float angle = (float) Math.toRadians(fromDeg + (toDeg - fromDeg) * t);
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);

			float ox = cx + cos * radius;
			float oy = cy + sin * radius;
			float ix = cx + cos * inner;
			float iy = cy + sin * inner;

			buf.vertex(matrix, ox, oy, 0f).color(fr, fg, fb, fa);
			buf.vertex(matrix, ix, iy, 0f).color(fr, fg, fb, fa);
		}

		BufferRenderer.drawWithGlobalProgram(buf.end());
		disableBlend();
	}

	/**
	 * Single-pixel line from (x1,y1) to (x2,y2).
	 * Uses POSITION_COLOR + DEBUG_LINES so we don't need the line vertex
	 * format with normals.
	 */
	public static void line(Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		enableBlend();
		RenderSystem.lineWidth(1f);
		setShader();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

		buf.vertex(matrix, x1, y1, 0f).color(fr, fg, fb, fa);
		buf.vertex(matrix, x2, y2, 0f).color(fr, fg, fb, fa);

		BufferRenderer.drawWithGlobalProgram(buf.end());
		disableBlend();
	}
}
