package com.qwant.qwantdlc.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qwant.qwantdlc.QwantDLC;
import com.qwant.qwantdlc.anim.Animation;
import com.qwant.qwantdlc.anim.ColorUtil;
import com.qwant.qwantdlc.anim.Easing;
import com.qwant.qwantdlc.module.Category;
import com.qwant.qwantdlc.module.Module;
import com.qwant.qwantdlc.module.ModuleManager;
import com.qwant.qwantdlc.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Animated single-window ClickGUI with sidebar + body grid.
 *
 * Animations:
 *   - Window: scale-in + fade-in on open (back-out easing).
 *   - Sidebar items: per-item hover and active fade.
 *   - Module cards: per-card hover lift + active color blend.
 *   - Search bar: focus glow.
 */
public class ClickGuiScreen extends Screen {
	private static Category selectedCategory = Category.COMBAT;
	private static String searchQuery = "";

	// Layout cache for the current frame.
	private float winX, winY;

	// Scrolling.
	private float scroll = 0f;
	private float maxScroll = 0f;

	// === Animations ===
	private final Animation openAnim = new Animation(420f, Easing.EASE_OUT_BACK, 0f);
	private final Map<Category, Animation> sidebarHover = new HashMap<>();
	private final Map<Category, Animation> sidebarActive = new HashMap<>();
	private final Animation exitHover = new Animation(160f, Easing.EASE_OUT_QUART, 0f);
	private final Map<Module, Animation> cardHover = new HashMap<>();
	private final Map<Module, Animation> cardActive = new HashMap<>();
	private final Animation searchFocus = new Animation(220f, Easing.EASE_OUT_QUART, 0f);
	private boolean searchFocused = false;

	public ClickGuiScreen() {
		super(Text.literal("Qwant"));
	}

	@Override
	protected void init() {
		super.init();
		openAnim.setTargetBool(true);
		for (Category c : Category.values()) {
			sidebarHover.computeIfAbsent(c, k -> new Animation(150f, Easing.EASE_OUT_QUART, 0f));
			sidebarActive.computeIfAbsent(c, k -> new Animation(220f, Easing.EASE_OUT_QUART, 0f));
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		float openK = openAnim.update();

		// Backdrop with eased alpha.
		int backdrop = (int) (0x99 * openK) << 24;
		ctx.fill(0, 0, this.width, this.height, backdrop);

		// Scale-in around the centre.
		float scale = 0.92f + 0.08f * openK;
		float cx = this.width / 2f;
		float cy = this.height / 2f;

		MatrixStack ms = ctx.getMatrices();
		ms.push();
		ms.translate(cx, cy, 0f);
		ms.scale(scale, scale, 1f);
		ms.translate(-cx, -cy, 0f);

		this.winX = (this.width  - Theme.WINDOW_WIDTH)  / 2f;
		this.winY = (this.height - Theme.WINDOW_HEIGHT) / 2f;

		Matrix4f m = ms.peek().getPositionMatrix();
		float globalAlpha = openK;

		// Subtle outer glow that pulses along with the open animation.
		int glowAccent = ColorUtil.withAlpha(0xFF8A2BE2, 0.35f * globalAlpha);
		Render2D.glow(m, winX, winY, Theme.WINDOW_WIDTH, Theme.WINDOW_HEIGHT,
			Theme.WINDOW_RADIUS, glowAccent, 12f);

		// Window background + border.
		Render2D.roundedRectWithOutline(m,
			winX, winY, Theme.WINDOW_WIDTH, Theme.WINDOW_HEIGHT,
			Theme.WINDOW_RADIUS,
			ColorUtil.withAlpha(Theme.WINDOW_BG, globalAlpha),
			ColorUtil.withAlpha(Theme.WINDOW_BORDER, globalAlpha));

		// Sidebar fill.
		Render2D.fillRect(m,
			winX + 1f, winY + 1f, Theme.SIDEBAR_WIDTH, Theme.WINDOW_HEIGHT - 2f,
			ColorUtil.withAlpha(Theme.SIDEBAR_BG, globalAlpha));

		Render2D.fillRect(m,
			winX + Theme.SIDEBAR_WIDTH, winY + 1f, 1f, Theme.WINDOW_HEIGHT - 2f,
			ColorUtil.withAlpha(Theme.WINDOW_BORDER, globalAlpha));

		drawSidebar(ctx, m, mouseX, mouseY, globalAlpha);
		drawBody(ctx, m, mouseX, mouseY, globalAlpha);

		ms.pop();
		super.render(ctx, mouseX, mouseY, delta);
	}

	// ---------------------------------------------------------------- sidebar

	private void drawSidebar(DrawContext ctx, Matrix4f m,
	                         int mouseX, int mouseY, float globalAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		// Logo.
		float lx = winX + 14f;
		float ly = winY + 14f;
		int chroma = ColorUtil.chroma(6f, 0.8f, 1f, 0f);
		Render2D.glow(m, lx, ly, 18f, 18f, 5f,
			ColorUtil.withAlpha(chroma, 0.6f * globalAlpha), 5f);
		Render2D.fillRoundedRect(m, lx, ly, 18f, 18f, 5f,
			ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE, globalAlpha));
		Render2D.strokeRoundedRect(m, lx, ly, 18f, 18f, 5f,
			ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE_2, globalAlpha));
		ctx.drawText(tr, "Q",
			(int) (lx + 6f), (int) (ly + 5f),
			ColorUtil.withAlpha(Theme.TEXT_PRIMARY, globalAlpha), false);

		ctx.drawText(tr, QwantDLC.MOD_NAME,
			(int) (lx + 26f), (int) (ly + 5f),
			ColorUtil.withAlpha(Theme.TEXT_PRIMARY, globalAlpha), false);

		// "Основные"
		ctx.drawText(tr, "Основные",
			(int) (winX + 14f), (int) (winY + 44f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, globalAlpha), false);

		Category[] mainCats = {
			Category.COMBAT, Category.MOVEMENT, Category.RENDER,
			Category.PLAYER, Category.MISC,
		};

		float itemY = winY + 58f;
		for (Category c : mainCats) {
			drawSidebarItem(ctx, m, c, itemY, mouseX, mouseY, globalAlpha);
			itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;
		}

		ctx.drawText(tr, "Другое",
			(int) (winX + 14f), (int) (itemY + 4f),
			ColorUtil.withAlpha(Theme.TEXT_MUTED, globalAlpha), false);
		itemY += 16f;

		drawSidebarItem(ctx, m, Category.THEMES, itemY, mouseX, mouseY, globalAlpha);
		itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;

		drawExitItem(ctx, m, itemY, mouseX, mouseY, globalAlpha);
	}

	private void drawSidebarItem(DrawContext ctx, Matrix4f m, Category cat,
	                             float y, int mouseX, int mouseY,
	                             float globalAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float x = winX + 8f;
		float w = Theme.SIDEBAR_WIDTH - 16f;
		float h = Theme.SIDEBAR_ITEM_HEIGHT;

		boolean active = cat == selectedCategory;
		boolean hover  = isInside(mouseX, mouseY, x, y, w, h);

		Animation hoverAnim = sidebarHover.get(cat);
		Animation activeAnim = sidebarActive.get(cat);
		hoverAnim.setTargetBool(hover && !active);
		activeAnim.setTargetBool(active);
		float hk = hoverAnim.update();
		float ak = activeAnim.update();

		// Hover background.
		if (hk > 0.01f) {
			Render2D.fillRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_HOVER, hk * globalAlpha));
		}

		// Active gradient + glow with eased alpha.
		if (ak > 0.005f) {
			Render2D.glow(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE_2, 0.45f * ak * globalAlpha), 5f);
			Render2D.fillRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE, ak * globalAlpha));
			Render2D.fillGradientH(m, x, y, w, h,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE,    ak * globalAlpha),
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE_2, ak * globalAlpha));
			Render2D.strokeRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE_2, ak * globalAlpha));
		}

		// Text colour blends hover/active.
		int color = ColorUtil.lerp(Theme.TEXT_SECONDARY, Theme.TEXT_PRIMARY,
			Math.max(hk, ak));
		ctx.drawText(tr, cat.getDisplay(),
			(int) (x + 12f),
			(int) (y + (h - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(color, globalAlpha), false);
	}

	private void drawExitItem(DrawContext ctx, Matrix4f m,
	                          float y, int mouseX, int mouseY,
	                          float globalAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float x = winX + 8f;
		float w = Theme.SIDEBAR_WIDTH - 16f;
		float h = Theme.SIDEBAR_ITEM_HEIGHT;
		boolean hover = isInside(mouseX, mouseY, x, y, w, h);
		exitHover.setTargetBool(hover);
		float hk = exitHover.update();

		if (hk > 0.01f) {
			Render2D.fillRoundedRect(m, x, y, w, h, Theme.SIDEBAR_ITEM_RADIUS,
				ColorUtil.withAlpha(0xFFB22A2A, hk * globalAlpha));
		}

		int color = ColorUtil.lerp(Theme.TEXT_SECONDARY, 0xFFFFFFFF, hk);
		ctx.drawText(tr, "Выйти",
			(int) (x + 12f),
			(int) (y + (h - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(color, globalAlpha), false);
	}

	// ------------------------------------------------------------------- body

	private void drawBody(DrawContext ctx, Matrix4f m,
	                      int mouseX, int mouseY, float globalAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		float bodyX = winX + Theme.SIDEBAR_WIDTH + 1f;
		float bodyY = winY + 1f;
		float bodyW = Theme.WINDOW_WIDTH - Theme.SIDEBAR_WIDTH - 2f;
		float bodyH = Theme.WINDOW_HEIGHT - 2f;

		// Search bar.
		float searchX = bodyX + 12f;
		float searchY = bodyY + 12f;
		float searchW = bodyW - 24f;
		boolean searchHover = isInside(mouseX, mouseY,
			searchX, searchY, searchW, Theme.SEARCH_HEIGHT);
		searchFocus.setTargetBool(searchHover || searchFocused);
		float fk = searchFocus.update();

		if (fk > 0.01f) {
			Render2D.glow(m, searchX, searchY, searchW, Theme.SEARCH_HEIGHT,
				Theme.SEARCH_RADIUS,
				ColorUtil.withAlpha(Theme.TEXT_ACCENT, 0.35f * fk * globalAlpha),
				4f);
		}

		Render2D.fillRoundedRect(m, searchX, searchY, searchW, Theme.SEARCH_HEIGHT,
			Theme.SEARCH_RADIUS,
			ColorUtil.withAlpha(Theme.SEARCH_BG, globalAlpha));
		int border = ColorUtil.lerp(Theme.SEARCH_BORDER, Theme.TEXT_ACCENT, fk);
		Render2D.strokeRoundedRect(m, searchX, searchY, searchW, Theme.SEARCH_HEIGHT,
			Theme.SEARCH_RADIUS, ColorUtil.withAlpha(border, globalAlpha));

		String displayed;
		int textColor;
		if (searchQuery.isEmpty()) {
			displayed = "Поиск";
			textColor = Theme.TEXT_MUTED;
		} else {
			displayed = searchQuery;
			textColor = Theme.TEXT_PRIMARY;
		}
		ctx.drawText(tr, displayed,
			(int) (searchX + 8f),
			(int) (searchY + (Theme.SEARCH_HEIGHT - tr.fontHeight) / 2f + 1f),
			ColorUtil.withAlpha(textColor, globalAlpha), false);
		// Blinking caret when focused.
		if (searchFocused
			&& (System.currentTimeMillis() / 500L) % 2L == 0L) {
			float cx = searchX + 8f + tr.getWidth(searchQuery) + 1f;
			float cy = searchY + (Theme.SEARCH_HEIGHT - tr.fontHeight) / 2f + 1f;
			Render2D.fillRect(m, cx, cy, 1f, tr.fontHeight,
				ColorUtil.withAlpha(Theme.TEXT_PRIMARY, globalAlpha));
		}

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

		int rows = (modules.size() + cols - 1) / cols;
		float contentH = rows * cardH + Math.max(0, rows - 1) * gap;
		float viewportH = gridBottom - gridTop;
		this.maxScroll = Math.max(0f, contentH - viewportH);
		if (scroll > maxScroll) scroll = maxScroll;

		for (int i = 0; i < modules.size(); i++) {
			int row = i / cols;
			int col = i % cols;
			float cx = gridLeft + col * (cardW + gap);
			float cy = gridTop + row * (cardH + gap) - scroll;

			if (cy + cardH < gridTop - 4f || cy > gridBottom + 4f) continue;
			drawCard(ctx, m, modules.get(i),
				cx, cy, cardW, cardH, mouseX, mouseY, globalAlpha);
		}
	}

	private void drawCard(DrawContext ctx, Matrix4f m, Module module,
	                      float x, float y, float w, float h,
	                      int mouseX, int mouseY, float globalAlpha) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		boolean active = module.isToggled();
		boolean hover  = isInside(mouseX, mouseY, x, y, w, h);

		Animation hoverAnim = cardHover.computeIfAbsent(module,
			k -> new Animation(160f, Easing.EASE_OUT_QUART, 0f));
		Animation activeAnim = cardActive.computeIfAbsent(module,
			k -> new Animation(260f, Easing.EASE_OUT_QUART, 0f));
		hoverAnim.setTargetBool(hover);
		activeAnim.setTargetBool(active);
		float hk = hoverAnim.update();
		float ak = activeAnim.update();

		// Active glow.
		if (ak > 0.01f) {
			Render2D.glow(m, x, y, w, h, Theme.CARD_RADIUS,
				ColorUtil.withAlpha(Theme.SIDEBAR_ITEM_ACTIVE_2, 0.45f * ak * globalAlpha),
				6f);
		}

		// Background blends Card -> Hover -> Active (in that priority).
		int bg = ColorUtil.lerp(Theme.CARD_BG, Theme.CARD_BG_HOVER, hk);
		bg = ColorUtil.lerp(bg, Theme.CARD_BG_ACTIVE, ak);
		Render2D.fillRoundedRect(m, x, y, w, h, Theme.CARD_RADIUS,
			ColorUtil.withAlpha(bg, globalAlpha));

		int borderC = ColorUtil.lerp(Theme.CARD_BORDER, Theme.SIDEBAR_ITEM_ACTIVE_2, ak);
		Render2D.strokeRoundedRect(m, x, y, w, h, Theme.CARD_RADIUS,
			ColorUtil.withAlpha(borderC, globalAlpha));

		// Title.
		ctx.drawText(tr, module.getName(),
			(int) (x + 8f), (int) (y + 6f),
			ColorUtil.withAlpha(Theme.TEXT_PRIMARY, globalAlpha), false);

		// Description.
		String desc = module.getDescription();
		if (!desc.isEmpty()) {
			String trimmed = trimToWidth(tr, desc, (int) (w - 16f));
			int descColor = ColorUtil.lerp(Theme.TEXT_SECONDARY, 0xFFEAD8FF, ak);
			ctx.drawText(tr, trimmed,
				(int) (x + 8f),
				(int) (y + 18f),
				ColorUtil.withAlpha(descColor, globalAlpha),
				false);
		}

		// Status dot.
		int dotColor = ColorUtil.lerp(Theme.TEXT_MUTED, 0xFFFFFFFF, ak);
		Render2D.fillRoundedRect(m, x + w - 11f, y + 6f, 5f, 5f, 2f,
			ColorUtil.withAlpha(dotColor, globalAlpha));
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
		for (Module mod : source) {
			if (mod.getName().toLowerCase().contains(q)
				|| mod.getDescription().toLowerCase().contains(q)) {
				out.add(mod);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

		// Sidebar.
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
		itemY += 16f;
		float x = winX + 8f, w = Theme.SIDEBAR_WIDTH - 16f, h = Theme.SIDEBAR_ITEM_HEIGHT;
		if (isInside(mouseX, mouseY, x, itemY, w, h)) {
			selectedCategory = Category.THEMES;
			scroll = 0f;
			return true;
		}
		itemY += Theme.SIDEBAR_ITEM_HEIGHT + 4f;
		if (isInside(mouseX, mouseY, x, itemY, w, h)) {
			this.close();
			return true;
		}

		// Search bar focus.
		float bodyX = winX + Theme.SIDEBAR_WIDTH + 1f;
		float bodyY = winY + 1f;
		float bodyW = Theme.WINDOW_WIDTH - Theme.SIDEBAR_WIDTH - 2f;
		float bodyH = Theme.WINDOW_HEIGHT - 2f;
		float searchX = bodyX + 12f;
		float searchY = bodyY + 12f;
		float searchW = bodyW - 24f;
		if (isInside(mouseX, mouseY, searchX, searchY, searchW, Theme.SEARCH_HEIGHT)) {
			searchFocused = true;
			return true;
		} else {
			searchFocused = false;
		}

		// Card clicks.
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
		if (searchFocused && chr >= 32 && chr != 127) {
			searchQuery += chr;
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
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
