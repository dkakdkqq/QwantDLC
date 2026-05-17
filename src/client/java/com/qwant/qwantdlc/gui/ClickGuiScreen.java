package com.qwant.qwantdlc.gui;

import java.util.ArrayList;
import java.util.List;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Single-window ClickGUI inspired by Celestial:
 *   - Sidebar (Qwant logo + categories + "Выйти")
 *   - Body  (search bar + 2-column grid of module cards)
 *
 * Drawn entirely with our own {@link Render2D}.
 */
public class ClickGuiScreen extends Screen {
	private static Category selectedCategory = Category.COMBAT;
	private static String searchQuery = "";

	// Computed each frame in render(); cached for click handling.
	private float winX, winY;

	// Scrolling for the cards grid.
	private float scroll = 0f;
	private float maxScroll = 0f;

	public ClickGuiScreen() {
		super(Text.literal("Qwant"));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// Dimmed backdrop so the world behind is muted.
		ctx.fill(0, 0, this.width, this.height, 0x99000000);

		this.winX = (this.width  - Theme.WINDOW_WIDTH)  / 2f;
		this.winY = (this.height - Theme.WINDOW_HEIGHT) / 2f;

		Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();

		// Window background + border (rounded).
		Render2D.roundedRectWithOutline(m,
			winX, winY, Theme.WINDOW_WIDTH, Theme.WINDOW_HEIGHT,
			Theme.WINDOW_RADIUS,
			Theme.WINDOW_BG, Theme.WINDOW_BORDER);

		// Sidebar fill (a touch darker than the body) — masked rectangle inside
		// the rounded window.
		Render2D.fillRect(m,
			winX + 1f, winY + 1f, Theme.SIDEBAR_WIDTH, Theme.WINDOW_HEIGHT - 2f,
			Theme.SIDEBAR_BG);

		// Vertical divider between sidebar and body.
		Render2D.fillRect(m,
			winX + Theme.SIDEBAR_WIDTH, winY + 1f, 1f, Theme.WINDOW_HEIGHT - 2f,
			Theme.WINDOW_BORDER);

		drawSidebar(ctx, m, mouseX, mouseY);
		drawBody(ctx, m, mouseX, mouseY);

		super.render(ctx, mouseX, mouseY, delta);
	}

	// ------------------------------------------------------------------ sidebar

	private void drawSidebar(DrawContext ctx, Matrix4f m, int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		// Logo header "Qwant"
		float lx = winX + 14f;
		float ly = winY + 14f;
		// Small rounded square as a logo bullet.
		Render2D.fillRoundedRect(m, lx, ly, 18f, 18f, 5f, Theme.SIDEBAR_ITEM_ACTIVE);
		Render2D.strokeRoundedRect(m, lx, ly, 18f, 18f, 5f, Theme.SIDEBAR_ITEM_ACTIVE_2);
		ctx.drawText(tr, "Q",
			(int) (lx + 6f), (int) (ly + 5f),
			Theme.TEXT_PRIMARY, false);

		ctx.drawText(tr, QwantDLC.MOD_NAME,
			(int) (lx + 26f), (int) (ly + 5f),
			Theme.TEXT_PRIMARY, false);

		// "Основные" label
		ctx.drawText(tr, "Основные",
			(int) (winX + 14f), (int) (winY + 44f),
			Theme.TEXT_MUTED, false);

		// Category buttons.
		Category[] mainCats = {
			Category.COMBAT, Category.MOVEMENT, Category.RENDER,
			Category.PLAYER, Category.MISC,
		};

		float itemY = winY + 58f;
		for (Category c : mainCats) {
			drawSidebarItem(ctx, m, c, itemY, mouseX, mouseY);
			itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;
		}

		// "Другое" section.
		ctx.drawText(tr, "Другое",
			(int) (winX + 14f), (int) (itemY + 4f),
			Theme.TEXT_MUTED, false);
		itemY += 16f;

		drawSidebarItem(ctx, m, Category.THEMES, itemY, mouseX, mouseY);
		itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;

		// "Выйти" button (special — doesn't represent a category).
		drawExitItem(ctx, m, itemY, mouseX, mouseY);
	}

	private void drawSidebarItem(DrawContext ctx, Matrix4f m, Category cat,
	                             float y, int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float x = winX + 8f;
		float w = Theme.SIDEBAR_WIDTH - 16f;
		float h = Theme.SIDEBAR_ITEM_HEIGHT;

		boolean active = cat == selectedCategory;
		boolean hover  = isInside(mouseX, mouseY, x, y, w, h);

		int bg;
		if (active) {
			Render2D.fillGradient(m, x, y, w, h,
				Theme.SIDEBAR_ITEM_ACTIVE, Theme.SIDEBAR_ITEM_ACTIVE_2);
			Render2D.strokeRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				Theme.SIDEBAR_ITEM_ACTIVE_2);
			bg = -1;
		} else if (hover) {
			bg = Theme.SIDEBAR_ITEM_HOVER;
		} else {
			bg = Theme.SIDEBAR_ITEM_BG;
		}
		if (bg != -1 && (bg & 0xFF000000) != 0) {
			Render2D.fillRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS, bg);
		}

		ctx.drawText(tr, cat.getDisplay(),
			(int) (x + 12f),
			(int) (y + (h - tr.fontHeight) / 2f + 1f),
			active ? Theme.TEXT_PRIMARY
			       : (hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY),
			false);
	}

	private void drawExitItem(DrawContext ctx, Matrix4f m,
	                          float y, int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float x = winX + 8f;
		float w = Theme.SIDEBAR_WIDTH - 16f;
		float h = Theme.SIDEBAR_ITEM_HEIGHT;

		boolean hover = isInside(mouseX, mouseY, x, y, w, h);
		if (hover) {
			Render2D.fillRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				Theme.SIDEBAR_ITEM_HOVER);
		}
		ctx.drawText(tr, "Выйти",
			(int) (x + 12f),
			(int) (y + (h - tr.fontHeight) / 2f + 1f),
			hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
	}

	// ------------------------------------------------------------------ body

	private void drawBody(DrawContext ctx, Matrix4f m, int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		float bodyX = winX + Theme.SIDEBAR_WIDTH + 1f;
		float bodyY = winY + 1f;
		float bodyW = Theme.WINDOW_WIDTH - Theme.SIDEBAR_WIDTH - 2f;
		float bodyH = Theme.WINDOW_HEIGHT - 2f;

		// Search bar.
		float searchX = bodyX + 12f;
		float searchY = bodyY + 12f;
		float searchW = bodyW - 24f;
		Render2D.fillRoundedRect(m, searchX, searchY, searchW, Theme.SEARCH_HEIGHT,
			Theme.SEARCH_RADIUS, Theme.SEARCH_BG);
		Render2D.strokeRoundedRect(m, searchX, searchY, searchW, Theme.SEARCH_HEIGHT,
			Theme.SEARCH_RADIUS, Theme.SEARCH_BORDER);

		String displayed = searchQuery.isEmpty() ? "Поиск" : searchQuery;
		int textColor = searchQuery.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT_PRIMARY;
		ctx.drawText(tr, displayed,
			(int) (searchX + 8f),
			(int) (searchY + (Theme.SEARCH_HEIGHT - tr.fontHeight) / 2f + 1f),
			textColor, false);

		// Cards grid.
		float gridTop    = searchY + Theme.SEARCH_HEIGHT + 12f;
		float gridLeft   = bodyX + 12f;
		float gridRight  = bodyX + bodyW - 12f;
		float gridBottom = bodyY + bodyH - 12f;

		List<Module> modules = filteredModules();
		int cols = 2;
		float gap = Theme.CARD_GAP;
		float cardW = (gridRight - gridLeft - gap * (cols - 1)) / cols;
		float cardH = Theme.CARD_HEIGHT;

		// Compute total content height for scroll bounds.
		int rows = (modules.size() + cols - 1) / cols;
		float contentH = rows * cardH + Math.max(0, rows - 1) * gap;
		float viewportH = gridBottom - gridTop;
		this.maxScroll = Math.max(0f, contentH - viewportH);
		if (scroll > maxScroll) scroll = maxScroll;

		// Scissor: cards are drawn through a software clip (we just draw and rely
		// on the window border being above + content fitting in most cases).
		// For simplicity we don't enable GL scissor here.
		for (int i = 0; i < modules.size(); i++) {
			int row = i / cols;
			int col = i % cols;
			float cx = gridLeft + col * (cardW + gap);
			float cy = gridTop + row * (cardH + gap) - scroll;

			// Skip cards entirely outside the viewport.
			if (cy + cardH < gridTop - 4f || cy > gridBottom + 4f) continue;

			drawCard(ctx, m, modules.get(i), cx, cy, cardW, cardH, mouseX, mouseY);
		}
	}

	private void drawCard(DrawContext ctx, Matrix4f m, Module module,
	                      float x, float y, float w, float h,
	                      int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		boolean active = module.isToggled();
		boolean hover  = isInside(mouseX, mouseY, x, y, w, h);

		int bg;
		if (active)      bg = Theme.CARD_BG_ACTIVE;
		else if (hover)  bg = Theme.CARD_BG_HOVER;
		else             bg = Theme.CARD_BG;

		Render2D.fillRoundedRect(m, x, y, w, h, Theme.CARD_RADIUS, bg);
		Render2D.strokeRoundedRect(m, x, y, w, h, Theme.CARD_RADIUS, Theme.CARD_BORDER);

		// Title
		ctx.drawText(tr, module.getName(),
			(int) (x + 8f), (int) (y + 6f),
			Theme.TEXT_PRIMARY, false);

		// Description (truncated to fit).
		String desc = module.getDescription();
		if (!desc.isEmpty()) {
			String trimmed = trimToWidth(tr, desc, (int) (w - 16f));
			ctx.drawText(tr, trimmed,
				(int) (x + 8f),
				(int) (y + 18f),
				active ? 0xFFEAD8FF : Theme.TEXT_SECONDARY,
				false);
		}

		// Tiny "settings" dot in the top-right corner just for visual parity.
		Render2D.fillRoundedRect(m, x + w - 11f, y + 6f, 5f, 5f, 2f,
			active ? 0xFFFFFFFF : Theme.TEXT_MUTED);
	}

	private static String trimToWidth(TextRenderer tr, String text, int maxPx) {
		if (tr.getWidth(text) <= maxPx) return text;
		String ellipsis = "…";
		int eW = tr.getWidth(ellipsis);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			sb.append(text.charAt(i));
			if (tr.getWidth(sb.toString()) + eW > maxPx) {
				if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
				return sb.toString() + ellipsis;
			}
		}
		return text;
	}

	private List<Module> filteredModules() {
		List<Module> source = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
		if (searchQuery.isEmpty()) return new ArrayList<>(source);
		String q = searchQuery.toLowerCase();
		List<Module> out = new ArrayList<>();
		for (Module m : source) {
			if (m.getName().toLowerCase().contains(q)
				|| m.getDescription().toLowerCase().contains(q)) {
				out.add(m);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

		// Sidebar category buttons.
		Category[] mainCats = {
			Category.COMBAT, Category.MOVEMENT, Category.RENDER,
			Category.PLAYER, Category.MISC,
		};
		float itemY = winY + 58f;
		for (Category c : mainCats) {
			float x = winX + 8f;
			float w = Theme.SIDEBAR_WIDTH - 16f;
			float h = Theme.SIDEBAR_ITEM_HEIGHT;
			if (isInside(mouseX, mouseY, x, itemY, w, h)) {
				selectedCategory = c;
				scroll = 0f;
				return true;
			}
			itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;
		}
		// "Другое" gap label
		itemY += 16f;
		// Themes
		float x = winX + 8f, w = Theme.SIDEBAR_WIDTH - 16f, h = Theme.SIDEBAR_ITEM_HEIGHT;
		if (isInside(mouseX, mouseY, x, itemY, w, h)) {
			selectedCategory = Category.THEMES;
			scroll = 0f;
			return true;
		}
		itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;
		// Exit
		if (isInside(mouseX, mouseY, x, itemY, w, h)) {
			this.close();
			return true;
		}

		// Cards grid clicks → toggle.
		float bodyX = winX + Theme.SIDEBAR_WIDTH + 1f;
		float bodyY = winY + 1f;
		float bodyW = Theme.WINDOW_WIDTH - Theme.SIDEBAR_WIDTH - 2f;
		float bodyH = Theme.WINDOW_HEIGHT - 2f;
		float searchY = bodyY + 12f;
		float gridTop    = searchY + Theme.SEARCH_HEIGHT + 12f;
		float gridLeft   = bodyX + 12f;
		float gridRight  = bodyX + bodyW - 12f;
		float gridBottom = bodyY + bodyH - 12f;

		List<Module> modules = filteredModules();
		int cols = 2;
		float gap = Theme.CARD_GAP;
		float cardW = (gridRight - gridLeft - gap * (cols - 1)) / cols;
		float cardH = Theme.CARD_HEIGHT;

		for (int i = 0; i < modules.size(); i++) {
			int row = i / cols;
			int col = i % cols;
			float cx = gridLeft + col * (cardW + gap);
			float cy = gridTop + row * (cardH + gap) - scroll;
			if (cy + cardH < gridTop || cy > gridBottom) continue;
			if (isInside(mouseX, mouseY, cx, cy, cardW, cardH)) {
				modules.get(i).toggle();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
	                             double horizontalAmount, double verticalAmount) {
		float bodyX = winX + Theme.SIDEBAR_WIDTH + 1f;
		float bodyY = winY + 1f;
		float bodyW = Theme.WINDOW_WIDTH - Theme.SIDEBAR_WIDTH - 2f;
		float bodyH = Theme.WINDOW_HEIGHT - 2f;
		if (mouseX >= bodyX && mouseX <= bodyX + bodyW
			&& mouseY >= bodyY && mouseY <= bodyY + bodyH) {
			scroll = Math.max(0f, Math.min(maxScroll,
				scroll - (float) verticalAmount * 16f));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (chr >= 32 && chr != 127) {
			searchQuery += chr;
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
			searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static boolean isInside(double mx, double my,
	                                float x, float y, float w, float h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}
}
