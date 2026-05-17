package com.qwant.qwantdlc.gui;

import java.util.List;

import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/**
 * One draggable, strict-black panel with rounded corners listing the modules
 * of a single Category. Toggling is done by clicking a module row.
 */
public class CategoryPanel {
	private final Category category;

	private float x;
	private float y;
	private final float width = Theme.PANEL_WIDTH;

	private boolean dragging;
	private double dragOffsetX;
	private double dragOffsetY;

	public CategoryPanel(Category category, float x, float y) {
		this.category = category;
		this.x = x;
		this.y = y;
	}

	public Category getCategory() {
		return category;
	}

	public float getX() { return x; }
	public float getY() { return y; }
	public float getWidth() { return width; }

	public float getHeight() {
		List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
		return Theme.HEADER_HEIGHT + modules.size() * Theme.ROW_HEIGHT + 4f;
	}

	public void render(DrawContext ctx, int mouseX, int mouseY) {
		// Drag follow
		if (dragging) {
			x = (float) (mouseX - dragOffsetX);
			y = (float) (mouseY - dragOffsetY);
		}

		float w = width;
		float h = getHeight();

		// Panel background (rounded, deep black) + thin border
		Render2D.roundedRectWithOutline(
			ctx.getMatrices().peek().getPositionMatrix(),
			x, y, w, h,
			Theme.PANEL_RADIUS,
			Theme.PANEL_BG,
			Theme.PANEL_BORDER
		);

		// Header background (slightly lighter)
		boolean headerHover = isInHeader(mouseX, mouseY);
		Render2D.fillRoundedRect(
			ctx.getMatrices().peek().getPositionMatrix(),
			x, y, w, Theme.HEADER_HEIGHT,
			Theme.PANEL_RADIUS,
			headerHover ? Theme.PANEL_HEADER_HOVER : Theme.PANEL_HEADER
		);
		// Mask the bottom of the header so corners stay rounded only at the top.
		Render2D.fillRect(
			ctx.getMatrices().peek().getPositionMatrix(),
			x, y + Theme.HEADER_HEIGHT - Theme.PANEL_RADIUS,
			w, Theme.PANEL_RADIUS,
			headerHover ? Theme.PANEL_HEADER_HOVER : Theme.PANEL_HEADER
		);
		// Re-draw border line so it stays sharp on top of the masking rect.
		Render2D.strokeRoundedRect(
			ctx.getMatrices().peek().getPositionMatrix(),
			x, y, w, h,
			Theme.PANEL_RADIUS,
			Theme.PANEL_BORDER
		);
		// Subtle 1px separator under header
		Render2D.fillRect(
			ctx.getMatrices().peek().getPositionMatrix(),
			x + 1f, y + Theme.HEADER_HEIGHT, w - 2f, 1f,
			Theme.PANEL_BORDER
		);

		// Header text
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		ctx.drawText(tr, category.getDisplay(),
			(int) (x + 6f),
			(int) (y + (Theme.HEADER_HEIGHT - tr.fontHeight) / 2f + 1f),
			Theme.TEXT_PRIMARY, false);

		// Module rows
		List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
		for (int i = 0; i < modules.size(); i++) {
			Module m = modules.get(i);
			float ry = y + Theme.HEADER_HEIGHT + 2f + i * Theme.ROW_HEIGHT;
			boolean hover = mouseX >= x + 1 && mouseX <= x + w - 1
				&& mouseY >= ry && mouseY <= ry + Theme.ROW_HEIGHT;

			if (m.isToggled()) {
				Render2D.fillRect(
					ctx.getMatrices().peek().getPositionMatrix(),
					x + 2f, ry, w - 4f, Theme.ROW_HEIGHT,
					Theme.MODULE_ACTIVE
				);
			} else if (hover) {
				Render2D.fillRect(
					ctx.getMatrices().peek().getPositionMatrix(),
					x + 2f, ry, w - 4f, Theme.ROW_HEIGHT,
					Theme.MODULE_HOVER
				);
			}

			int textColor = m.isToggled() ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
			ctx.drawText(tr, m.getName(),
				(int) (x + 6f),
				(int) (ry + (Theme.ROW_HEIGHT - tr.fontHeight) / 2f + 1f),
				textColor, false);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isInside(mouseX, mouseY)) return false;

		// Header click → start drag
		if (button == 0 && isInHeader((float) mouseX, (float) mouseY)) {
			dragging = true;
			dragOffsetX = mouseX - x;
			dragOffsetY = mouseY - y;
			return true;
		}

		// Module row click → toggle
		List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
		for (int i = 0; i < modules.size(); i++) {
			float ry = y + Theme.HEADER_HEIGHT + 2f + i * Theme.ROW_HEIGHT;
			if (mouseX >= x + 1 && mouseX <= x + width - 1
				&& mouseY >= ry && mouseY <= ry + Theme.ROW_HEIGHT) {
				if (button == 0) {
					modules.get(i).toggle();
				}
				return true;
			}
		}
		return false;
	}

	public void mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) dragging = false;
	}

	public boolean isInside(double mx, double my) {
		return mx >= x && mx <= x + width && my >= y && my <= y + getHeight();
	}

	private boolean isInHeader(float mx, float my) {
		return mx >= x && mx <= x + width && my >= y && my <= y + Theme.HEADER_HEIGHT;
	}
}
