package com.qwant.qwantdlc.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

/**
 * Custom immediate-mode 3D renderer working in world space.
 *
 * Use these methods inside a WorldRenderEvents.LAST handler. The
 * {@link MatrixStack} provided by the event must be passed in; its top is
 * the camera-space matrix (camera is at origin).
 */
public final class Render3D {
	private Render3D() {}

	private static float a(int c) { return ((c >> 24) & 0xFF) / 255f; }
	private static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
	private static float g(int c) { return ((c >> 8) & 0xFF) / 255f; }
	private static float b(int c) { return (c & 0xFF) / 255f; }

	private static Camera camera() {
		return MinecraftClient.getInstance().gameRenderer.getCamera();
	}

	/**
	 * Renders an axis-aligned filled box at world coordinates.
	 * Translucent fill, no depth test (so it draws through walls).
	 */
	public static void fillBox(MatrixStack stack, Box worldBox, int color) {
		Vec3d cam = camera().getPos();

		stack.push();
		stack.translate(worldBox.minX - cam.x, worldBox.minY - cam.y, worldBox.minZ - cam.z);

		float w = (float) (worldBox.maxX - worldBox.minX);
		float h = (float) (worldBox.maxY - worldBox.minY);
		float d = (float) (worldBox.maxZ - worldBox.minZ);

		Matrix4f m = stack.peek().getPositionMatrix();

		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

		Tessellator t = Tessellator.getInstance();
		BufferBuilder buf = t.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		// -Y (bottom)
		buf.vertex(m, 0, 0, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, 0, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, 0, d).color(fr, fg, fb, fa);
		buf.vertex(m, 0, 0, d).color(fr, fg, fb, fa);
		// +Y (top)
		buf.vertex(m, 0, h, 0).color(fr, fg, fb, fa);
		buf.vertex(m, 0, h, d).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, d).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, 0).color(fr, fg, fb, fa);
		// -Z
		buf.vertex(m, 0, 0, 0).color(fr, fg, fb, fa);
		buf.vertex(m, 0, h, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, 0, 0).color(fr, fg, fb, fa);
		// +Z
		buf.vertex(m, 0, 0, d).color(fr, fg, fb, fa);
		buf.vertex(m, w, 0, d).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, d).color(fr, fg, fb, fa);
		buf.vertex(m, 0, h, d).color(fr, fg, fb, fa);
		// -X
		buf.vertex(m, 0, 0, 0).color(fr, fg, fb, fa);
		buf.vertex(m, 0, 0, d).color(fr, fg, fb, fa);
		buf.vertex(m, 0, h, d).color(fr, fg, fb, fa);
		buf.vertex(m, 0, h, 0).color(fr, fg, fb, fa);
		// +X
		buf.vertex(m, w, 0, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, 0).color(fr, fg, fb, fa);
		buf.vertex(m, w, h, d).color(fr, fg, fb, fa);
		buf.vertex(m, w, 0, d).color(fr, fg, fb, fa);

		BufferRenderer.drawWithGlobalProgram(buf.end());

		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();

		stack.pop();
	}

	/**
	 * Renders the wireframe of an axis-aligned box (12 edges as line segments).
	 * Drawn through walls (no depth test) for ESP-style outlines.
	 */
	public static void outlineBox(MatrixStack stack, Box worldBox, int color, float lineWidth) {
		Vec3d cam = camera().getPos();

		stack.push();
		stack.translate(worldBox.minX - cam.x, worldBox.minY - cam.y, worldBox.minZ - cam.z);

		float w = (float) (worldBox.maxX - worldBox.minX);
		float h = (float) (worldBox.maxY - worldBox.minY);
		float d = (float) (worldBox.maxZ - worldBox.minZ);

		Matrix4f m = stack.peek().getPositionMatrix();
		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.lineWidth(lineWidth);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

		Tessellator t = Tessellator.getInstance();
		BufferBuilder buf = t.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

		// bottom rectangle
		edge(buf, m, 0, 0, 0,  w, 0, 0,  fr, fg, fb, fa);
		edge(buf, m, w, 0, 0,  w, 0, d,  fr, fg, fb, fa);
		edge(buf, m, w, 0, d,  0, 0, d,  fr, fg, fb, fa);
		edge(buf, m, 0, 0, d,  0, 0, 0,  fr, fg, fb, fa);
		// top rectangle
		edge(buf, m, 0, h, 0,  w, h, 0,  fr, fg, fb, fa);
		edge(buf, m, w, h, 0,  w, h, d,  fr, fg, fb, fa);
		edge(buf, m, w, h, d,  0, h, d,  fr, fg, fb, fa);
		edge(buf, m, 0, h, d,  0, h, 0,  fr, fg, fb, fa);
		// vertical edges
		edge(buf, m, 0, 0, 0,  0, h, 0,  fr, fg, fb, fa);
		edge(buf, m, w, 0, 0,  w, h, 0,  fr, fg, fb, fa);
		edge(buf, m, w, 0, d,  w, h, d,  fr, fg, fb, fa);
		edge(buf, m, 0, 0, d,  0, h, d,  fr, fg, fb, fa);

		BufferRenderer.drawWithGlobalProgram(buf.end());

		RenderSystem.lineWidth(1f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();

		stack.pop();
	}

	/**
	 * Tracer line from the camera's near plane (player POV center) to a world position.
	 */
	public static void tracer(MatrixStack stack, Vec3d worldTo, int color, float lineWidth) {
		Vec3d cam = camera().getPos();

		Matrix4f m = stack.peek().getPositionMatrix();
		float fa = a(color), fr = r(color), fg = g(color), fb = b(color);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.lineWidth(lineWidth);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

		Tessellator t = Tessellator.getInstance();
		BufferBuilder buf = t.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

		// "from" is the camera origin in camera space (0,0,0).
		// We start a tiny bit forward so it doesn't get clipped by the near plane.
		float fromX = 0f, fromY = 0f, fromZ = 0f;
		float toX = (float) (worldTo.x - cam.x);
		float toY = (float) (worldTo.y - cam.y);
		float toZ = (float) (worldTo.z - cam.z);

		buf.vertex(m, fromX, fromY, fromZ).color(fr, fg, fb, fa);
		buf.vertex(m, toX,   toY,   toZ).color(fr, fg, fb, fa);

		BufferRenderer.drawWithGlobalProgram(buf.end());

		RenderSystem.lineWidth(1f);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	private static void edge(BufferBuilder buf, Matrix4f m,
	                         float x1, float y1, float z1,
	                         float x2, float y2, float z2,
	                         float fr, float fg, float fb, float fa) {
		buf.vertex(m, x1, y1, z1).color(fr, fg, fb, fa);
		buf.vertex(m, x2, y2, z2).color(fr, fg, fb, fa);
	}
}
