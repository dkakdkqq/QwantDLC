package com.qwant.qwantdlc.gui;

import java.util.ArrayList;
import java.util.List;

import com.qwant.qwantdlc.module.Category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Strict-black, square-with-rounded-corners ClickGUI.
 * One draggable panel per Category, drawn with our own Render2D.
 */
public class ClickGuiScreen extends Screen {
	// Persisted across opens within a session.
	private static final List<CategoryPanel> PANELS = new ArrayList<>();
	private static boolean initialized = false;

	public ClickGuiScreen() {
		super(Text.literal("QwantDLC ClickGUI"));
	}

	@Override
	protected void init() {
		super.init();
		if (!initialized) {
			float startX = 8f;
			float startY = 8f;
			float gap = 6f;
			Category[] cats = Category.values();
			for (int i = 0; i < cats.length; i++) {
				PANELS.add(new CategoryPanel(
					cats[i],
					startX + i * (Theme.PANEL_WIDTH + gap),
					startY
				));
			}
			initialized = true;
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// Dim background a bit (no full vanilla overlay).
		ctx.fill(0, 0, this.width, this.height, 0x80000000);

		super.render(ctx, mouseX, mouseY, delta);

		for (CategoryPanel p : PANELS) {
			p.render(ctx, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Iterate top-down so the topmost panel handles the click and is then
		// brought to the front for subsequent renders.
		for (int i = PANELS.size() - 1; i >= 0; i--) {
			CategoryPanel p = PANELS.get(i);
			if (p.mouseClicked(mouseX, mouseY, button)) {
				// bring to front
				PANELS.remove(i);
				PANELS.add(p);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (CategoryPanel p : PANELS) {
			p.mouseReleased(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
