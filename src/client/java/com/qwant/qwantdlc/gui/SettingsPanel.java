package com.qwant.qwantdlc.gui;

import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.render.Render2D;
import com.qwant.qwantdlc.setting.BoolSetting;
import com.qwant.qwantdlc.setting.ModeSetting;
import com.qwant.qwantdlc.setting.MultiSelectSetting;
import com.qwant.qwantdlc.setting.Setting;
import com.qwant.qwantdlc.setting.SliderSetting;
import com.qwant.qwantdlc.util.KeyNames;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import org.joml.Matrix4f;

/**
 * Side-panel that appears to the right of the main ClickGUI window when the
 * user right-clicks a module card. Renders all of the module's settings
 * (sliders, modes, multi-selects, booleans) and a keybind chip.
 *
 * Layout:
 *   - Header with module name + description.
 *   - Body with one block per Setting.
 */
public class SettingsPanel {
	public static final float WIDTH = 200f;

	private final Animation openAnim = new Animation(280f, Easing.EASE_OUT_QUART, 0f);
	private Module module;
	private float lastWinX, lastWinY; // anchor of the parent window
	private float currentY;

	// Drag state for the active slider, if any.
	private SliderSetting draggingSlider;
	private float sliderTrackX, sliderTrackW;

	public Module getModule() {
		return module;
	}

	public boolean isVisible() {
		return module != null && (openAnim.getRaw() > 0.01f || !openAnim.done());
	}

	/** True if the user requested the panel to be open (target == 1). */
	public boolean isOpen() {
		return module != null && openAnim.getRaw() > 0.5f;
	}

	public void open(Module mod) {
		this.module = mod;
		this.openAnim.setTargetBool(true);
	}

	public void close() {
		this.openAnim.setTargetBool(false);
	}

	public void toggle(Module mod) {
		if (this.module == mod && openAnim.getRaw() > 0.5f) {
			close();
		} else {
			open(mod);
		}
	}
	public float panelX() {
		return lastWinX + Theme.WINDOW_WIDTH + 6f;
	}

	public float panelY() {
		return lastWinY;
	}

	public float panelW() {
		return WIDTH;
	}

	public float panelH() {
		return Theme.WINDOW_HEIGHT;
	}

	public boolean inside(double mx, double my) {
		float a = openAnim.getRaw();
		if (a <= 0.05f || module == null) return false;
		return mx >= panelX() && mx <= panelX() + panelW()
			&& my >= panelY() && my <= panelY() + panelH();
	}

	public void render(DrawContext ctx, Matrix4f m,
	                   float winX, float winY,
	                   int mouseX, int mouseY, float globalAlpha) {
		this.lastWinX = winX;
		this.lastWinY = winY;

		float a = openAnim.update();
		// When fully closed AND target is 0, swallow module reference so
		// nothing keeps rendering.
		if (a <= 0.001f && openAnim.done()) {
			this.module = null;
			return;
		}
		if (a <= 0.001f) return;
		if (module == null) return;

		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float panelAlpha = a * globalAlpha;

		float px = panelX() + (1f - a) * -16f;
		float py = panelY();
		float pw = panelW();
		float ph = panelH();

		Render2D.glow(m, px, py, pw, ph, Theme.WINDOW_RADIUS,
			ColorUtil.withAlpha(0xFF8A2BE2, 0.3f * panelAlpha), 10f);
		Render2D.fillRoundedRect(m, px, py, pw, ph, Theme.WINDOW_RADIUS,
			ColorUtil.withAlpha(Theme.WINDOW_BG, panelAlpha));
		Render2D.strokeRoundedRect(m, px, py, pw, ph, Theme.WINDOW_RADIUS,
			ColorUtil.withAlpha(Theme.WINDOW_BORDER, panelAlpha));

		// Header.
		float padX = 12f, padY = 12f;
		ctx.drawText(tr, module.getName(),
			(int) (px + padX), (int) (py + padY),
			ColorUtil.withAlpha(Theme.TEXT_PRIMARY, panelAlpha), false);
		if (!module.getDescription().isEmpty()) {
			ctx.drawText(tr, trim(tr, module.getDescription(), (int) (pw - padX * 2)),
				(int) (px + padX), (int) (py + padY + tr.fontHeight + 2f),
				ColorUtil.withAlpha(Theme.TEXT_SECONDARY, panelAlpha), false);
		}
		float separatorY = py + padY + tr.fontHeight * 2 + 8f;
		Render2D.fillRect(m, px + padX, separatorY, pw - padX * 2, 1f,
			ColorUtil.withAlpha(Theme.WINDOW_BORDER, panelAlpha));

		// Keybind chip.
		String keyLabel = "Клавиша:  " + KeyNames.of(module.getKey());
		ctx.drawText(tr, keyLabel,
			(int) (px + padX), (int) (separatorY + 8f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, panelAlpha), false);

		// Body.
		this.currentY = separatorY + 8f + tr.fontHeight + 10f;
		for (Setting s : module.getSettings()) {
			renderSetting(ctx, m, s, px, pw, mouseX, mouseY, panelAlpha);
		}
	}

	private void renderSetting(DrawContext ctx, Matrix4f m, Setting s,
	                           float px, float pw,
	                           int mouseX, int mouseY, float panelAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float padX = 12f;
		float lineH = tr.fontHeight + 2f;

		// Section title.
		ctx.drawText(tr, s.getName(),
			(int) (px + padX), (int) currentY,
			ColorUtil.withAlpha(Theme.TEXT_PRIMARY, panelAlpha), false);
		currentY += lineH + 4f;

		if (s instanceof SliderSetting slider) {
			renderSlider(ctx, m, slider, px, pw, mouseX, mouseY, panelAlpha);
		} else if (s instanceof ModeSetting mode) {
			renderMode(ctx, m, mode, px, pw, mouseX, mouseY, panelAlpha);
		} else if (s instanceof MultiSelectSetting multi) {
			renderMulti(ctx, m, multi, px, pw, mouseX, mouseY, panelAlpha);
		} else if (s instanceof BoolSetting bs) {
			renderBool(ctx, m, bs, px, pw, mouseX, mouseY, panelAlpha);
		}
		currentY += 6f;
	}

	private void renderSlider(DrawContext ctx, Matrix4f m, SliderSetting s,
	                          float px, float pw,
	                          int mouseX, int mouseY, float panelAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float padX = 12f;
		float trackX = px + padX;
		float trackY = currentY + 4f;
		float trackW = pw - padX * 2;
		float trackH = 4f;

		Render2D.fillRoundedRect(m, trackX, trackY, trackW, trackH, 2f,
			ColorUtil.withAlpha(0xFF1F1F25, panelAlpha));
		float ratio = s.getRatio();
		float fillW = Math.max(0f, trackW * ratio);
		if (fillW > 0.5f) {
			Render2D.fillGradientH(m, trackX, trackY, fillW, trackH,
				ColorUtil.withAlpha(0xFF8A2BE2, panelAlpha),
				ColorUtil.withAlpha(0xFFB14CFF, panelAlpha));
		}

		float knobX = trackX + fillW - 4f;
		float knobY = trackY - 3f;
		Render2D.glow(m, knobX, knobY, 8f, 10f, 4f,
			ColorUtil.withAlpha(0xFFB14CFF, 0.5f * panelAlpha), 3f);
		Render2D.fillRoundedRect(m, knobX, knobY, 8f, 10f, 4f,
			ColorUtil.withAlpha(0xFFFFFFFF, panelAlpha));

		String value = s.formatValue();
		int vw = tr.getWidth(value);
		ctx.drawText(tr, value,
			(int) (trackX + trackW - vw), (int) (currentY - 12f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, panelAlpha), false);

		// Cache geometry for click/drag handling.
		if (s == draggingSlider) {
			sliderTrackX = trackX;
			sliderTrackW = trackW;
		}

		currentY += 12f;
	}

	private void renderMode(DrawContext ctx, Matrix4f m, ModeSetting s,
	                        float px, float pw,
	                        int mouseX, int mouseY, float panelAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float padX = 12f;
		float gap = 4f;
		float chipH = tr.fontHeight + 6f;
		float availW = pw - padX * 2;
		float chipMaxW = (availW - gap) / 2f;

		float cx = px + padX;
		float cy = currentY;
		String[] options = s.getOptions();
		for (int i = 0; i < options.length; i++) {
			String label = options[i];
			int lw = tr.getWidth(label);
			float chipW = Math.min(chipMaxW, lw + 14f);
			boolean selected = i == s.getIndex();
			boolean hover = mouseX >= cx && mouseX <= cx + chipW
				&& mouseY >= cy && mouseY <= cy + chipH;

			int bg = selected ? 0xFF8A2BE2 : (hover ? 0xFF1B1B22 : 0xFF15151A);
			int border = selected ? 0xFFB14CFF : Theme.WINDOW_BORDER;
			if (selected) {
				Render2D.glow(m, cx, cy, chipW, chipH, 4f,
					ColorUtil.withAlpha(0xFFB14CFF, 0.4f * panelAlpha), 4f);
			}
			Render2D.fillRoundedRect(m, cx, cy, chipW, chipH, 4f,
				ColorUtil.withAlpha(bg, panelAlpha));
			Render2D.strokeRoundedRect(m, cx, cy, chipW, chipH, 4f,
				ColorUtil.withAlpha(border, panelAlpha));
			ctx.drawText(tr, label,
				(int) (cx + (chipW - lw) / 2f),
				(int) (cy + (chipH - tr.fontHeight) / 2f + 1f),
				ColorUtil.withAlpha(selected ? 0xFFFFFFFF : Theme.TEXT_SECONDARY, panelAlpha),
				false);

			cx += chipW + gap;
			if (cx + chipMaxW > px + padX + availW + 1f) {
				cx = px + padX;
				cy += chipH + gap;
			}
		}
		currentY = cy + chipH + 2f;
	}

	private void renderMulti(DrawContext ctx, Matrix4f m, MultiSelectSetting s,
	                         float px, float pw,
	                         int mouseX, int mouseY, float panelAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float padX = 12f;
		float gap = 4f;
		float chipH = tr.fontHeight + 6f;
		float availW = pw - padX * 2;

		float cx = px + padX;
		float cy = currentY;
		String[] options = s.getOptions();
		for (int i = 0; i < options.length; i++) {
			String label = options[i];
			int lw = tr.getWidth(label);
			float chipW = lw + 14f;
			// Wrap to next row if it doesn't fit.
			if (cx + chipW > px + padX + availW + 0.5f) {
				cx = px + padX;
				cy += chipH + gap;
			}

			boolean selected = s.isSelected(i);
			boolean hover = mouseX >= cx && mouseX <= cx + chipW
				&& mouseY >= cy && mouseY <= cy + chipH;

			int bg = selected ? 0xFF8A2BE2 : (hover ? 0xFF1B1B22 : 0xFF15151A);
			int border = selected ? 0xFFB14CFF : Theme.WINDOW_BORDER;
			if (selected) {
				Render2D.glow(m, cx, cy, chipW, chipH, 4f,
					ColorUtil.withAlpha(0xFFB14CFF, 0.35f * panelAlpha), 4f);
			}
			Render2D.fillRoundedRect(m, cx, cy, chipW, chipH, 4f,
				ColorUtil.withAlpha(bg, panelAlpha));
			Render2D.strokeRoundedRect(m, cx, cy, chipW, chipH, 4f,
				ColorUtil.withAlpha(border, panelAlpha));
			ctx.drawText(tr, label,
				(int) (cx + (chipW - lw) / 2f),
				(int) (cy + (chipH - tr.fontHeight) / 2f + 1f),
				ColorUtil.withAlpha(selected ? 0xFFFFFFFF : Theme.TEXT_SECONDARY, panelAlpha),
				false);

			cx += chipW + gap;
		}
		currentY = cy + chipH + 2f;
	}

	private void renderBool(DrawContext ctx, Matrix4f m, BoolSetting s,
	                        float px, float pw,
	                        int mouseX, int mouseY, float panelAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float padX = 12f;
		float row = px + padX;
		float rowY = currentY;
		float trackW = 22f, trackH = 12f;
		float trackX = px + pw - padX - trackW;

		boolean v = s.get();
		int trackBg = v ? 0xFF8A2BE2 : 0xFF2A2A33;
		Render2D.fillRoundedRect(m, trackX, rowY, trackW, trackH, trackH / 2f,
			ColorUtil.withAlpha(trackBg, panelAlpha));
		float knobX = v ? trackX + trackW - 10f : trackX + 2f;
		Render2D.fillRoundedRect(m, knobX, rowY + 2f, 8f, 8f, 4f,
			ColorUtil.withAlpha(0xFFFFFFFF, panelAlpha));

		ctx.drawText(tr, s.getName(),
			(int) row, (int) (rowY + (trackH - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(Theme.TEXT_SECONDARY, panelAlpha), false);

		currentY = rowY + trackH + 4f;
	}

	// ----------------------------------------------------------- input handling

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isVisible() || openAnim.getRaw() < 0.5f) return false;
		if (!inside(mouseX, mouseY)) return false;

		if (button != 0 && button != 1) return true; // swallow

		// Re-walk the layout to find what was hit. We replay the same
		// layout loop used in render().
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float px = panelX();
		float py = panelY();
		float pw = panelW();
		float padX = 12f;
		float separatorY = py + 12f + tr.fontHeight * 2 + 8f;
		float y = separatorY + 8f + tr.fontHeight + 10f;

		for (Setting s : module.getSettings()) {
			y += tr.fontHeight + 2f + 4f; // section title

			if (s instanceof SliderSetting slider) {
				float trackX = px + padX;
				float trackY = y + 4f;
				float trackW = pw - padX * 2;
				float trackH = 14f; // hit zone is a bit taller
				if (mouseX >= trackX && mouseX <= trackX + trackW
					&& mouseY >= trackY - 4f && mouseY <= trackY + trackH) {
					draggingSlider = slider;
					sliderTrackX = trackX;
					sliderTrackW = trackW;
					updateSlider(slider, (float) mouseX);
					return true;
				}
				y += 12f + 6f;
				continue;
			}
			if (s instanceof ModeSetting mode) {
				float gap = 4f;
				float chipH = tr.fontHeight + 6f;
				float availW = pw - padX * 2;
				float chipMaxW = (availW - gap) / 2f;
				float cx = px + padX;
				float cy = y;
				String[] options = mode.getOptions();
				for (int i = 0; i < options.length; i++) {
					int lw = tr.getWidth(options[i]);
					float chipW = Math.min(chipMaxW, lw + 14f);
					if (mouseX >= cx && mouseX <= cx + chipW
						&& mouseY >= cy && mouseY <= cy + chipH) {
						mode.setIndex(i);
						return true;
					}
					cx += chipW + gap;
					if (cx + chipMaxW > px + padX + availW + 1f) {
						cx = px + padX;
						cy += chipH + gap;
					}
				}
				y = cy + chipH + 2f + 6f;
				continue;
			}
			if (s instanceof MultiSelectSetting multi) {
				float gap = 4f;
				float chipH = tr.fontHeight + 6f;
				float availW = pw - padX * 2;
				float cx = px + padX;
				float cy = y;
				String[] options = multi.getOptions();
				for (int i = 0; i < options.length; i++) {
					int lw = tr.getWidth(options[i]);
					float chipW = lw + 14f;
					if (cx + chipW > px + padX + availW + 0.5f) {
						cx = px + padX;
						cy += chipH + gap;
					}
					if (mouseX >= cx && mouseX <= cx + chipW
						&& mouseY >= cy && mouseY <= cy + chipH) {
						multi.toggle(i);
						return true;
					}
					cx += chipW + gap;
				}
				y = cy + chipH + 2f + 6f;
				continue;
			}
			if (s instanceof BoolSetting bs) {
				float trackW = 22f, trackH = 12f;
				float trackX = px + pw - padX - trackW;
				if (mouseX >= trackX - 60f && mouseX <= trackX + trackW
					&& mouseY >= y && mouseY <= y + trackH) {
					bs.toggle();
					return true;
				}
				y += trackH + 4f + 6f;
				continue;
			}
			y += 6f;
		}
		// Click inside the panel but didn't hit anything actionable —
		// still swallow it so the underlying GUI doesn't react.
		return true;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingSlider != null && button == 0) {
			draggingSlider = null;
			return true;
		}
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		if (draggingSlider != null && button == 0) {
			updateSlider(draggingSlider, (float) mouseX);
			return true;
		}
		return false;
	}

	private void updateSlider(SliderSetting s, float mouseX) {
		if (sliderTrackW <= 0f) return;
		float ratio = (mouseX - sliderTrackX) / sliderTrackW;
		s.setRatio(ratio);
	}

	private static String trim(TextRenderer tr, String text, int maxPx) {
		if (tr.getWidth(text) <= maxPx) return text;
		String e = "…";
		int ew = tr.getWidth(e);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			sb.append(text.charAt(i));
			if (tr.getWidth(sb.toString()) + ew > maxPx) {
				sb.deleteCharAt(sb.length() - 1);
				return sb.toString() + e;
			}
		}
		return text;
	}
}
