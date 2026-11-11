package com.notheartyt.easygamemode;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EasyGamemodeScreen extends Screen {
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {}

    private static final int BTN = 20, GAP = 2, ROW = 24;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF9A9AB0, GOLD = 0xFFFFC83D;
    private static final int BORDER = 0xFF3A3A55, PANEL = 0xFF12121C, HEADER = 0xFF1A1A2A;
    private static final int CARD = 0xFF1C1C2A, CARD_HOVER = 0xFF26263A;
    private static final int[] ACCENT = {0xFFE0524A, 0xFF55D26E, 0xFFE8A93B, 0xFF6FA8FF};
    private static final Formatting[] TIP_COLOR = {Formatting.RED, Formatting.GREEN, Formatting.GOLD, Formatting.AQUA};
    private static final String[] LABEL = {"Survival", "Creative", "Adventure", "Spectator"};

    private final List<Hit> hits = new ArrayList<>();
    private final ItemStack[] icons = {
            new ItemStack(Items.IRON_SWORD),   // Survival
            new ItemStack(Items.GRASS_BLOCK),  // Creative
            new ItemStack(Items.MAP),          // Adventure
            new ItemStack(Items.ENDER_EYE)     // Spectator
    };

    private TextFieldWidget searchField;
    private int px, py, pw, ph;
    private int sfx, sfy, sfw, sfh;
    private int scroll = 0, maxScroll = 0;
    private boolean armed = false; // pressing the open key closes the GUI once the opening press was released
    private List<Text> tip;

    public EasyGamemodeScreen() {
        super(Text.literal("Easy Gamemode"));
        GamemodeController.clearStatus();
    }

    @Override
    public boolean shouldPause() {
        return false; // keep the integrated server running so commands are processed
    }

    @Override
    protected void init() {
        String keep = searchField != null ? searchField.getText() : "";
        pw = Math.min(330, width - 16);
        ph = Math.max(150, Math.min(280, height - 16));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sfx = px + 8;
        sfy = py + 28;
        sfw = pw - 16;
        sfh = 16;
        searchField = new TextFieldWidget(textRenderer, sfx, sfy, sfw, sfh, Text.literal("Search players"));
        searchField.setMaxLength(32);
        searchField.setText(keep);
        addDrawableChild(searchField);
        searchField.setFocused(false);
        setFocused(null);
    }

    private int boundKey() {
        return KeyBindingHelper.getBoundKeyOf(EasyGamemodeClient.openKey).getCode();
    }

    private static int shade(int c, double k) {
        int r = (int) (((c >> 16) & 0xFF) * k);
        int g = (int) (((c >> 8) & 0xFF) * k);
        int b = (int) ((c & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void modeButton(DrawContext ctx, int x, int y, int idx, boolean active, int mx, int my,
                            int clipTop, int clipBottom, String who, Runnable action) {
        int accent = ACCENT[idx];
        int hy1 = Math.max(y, clipTop);
        int hy2 = Math.min(y + BTN, clipBottom);
        boolean visible = hy1 < hy2;
        boolean hover = visible && mx >= x && mx < x + BTN && my >= hy1 && my < hy2;
        int border = active ? accent : (hover ? 0xFFB8B8D0 : 0xFF3A3A4E);
        int bg = active ? shade(accent, 0.38) : (hover ? 0xFF303046 : 0xFF1A1A26);
        ctx.fill(x, y, x + BTN, y + BTN, border);
        ctx.fill(x + 1, y + 1, x + BTN - 1, y + BTN - 1, bg);
        ctx.drawItem(icons[idx], x + 2, y + 2);
        if (visible) {
            hits.add(new Hit(x, hy1, x + BTN, hy2, action));
        }
        if (hover) {
            tip = List.<Text>of(
                    Text.literal(LABEL[idx]).formatted(TIP_COLOR[idx]),
                    Text.literal(active ? "Current gamemode" : "Set " + who + " to " + LABEL[idx]).formatted(Formatting.GRAY));
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        tip = null;
        GamemodeController.poll();

        if (!armed) {
            long handle = client.getWindow().getHandle();
            if (GLFW.glfwGetKey(handle, boundKey()) != GLFW.GLFW_PRESS) armed = true;
        }

        List<PlayerListEntry> all = GamemodeController.players();
        String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<PlayerListEntry> shown = new ArrayList<>();
        for (PlayerListEntry e : all) {
            if (q.isEmpty() || GamemodeController.name(e).toLowerCase(Locale.ROOT).contains(q)) shown.add(e);
        }

        // Gamemode shared by everybody (highlighted on the EVERYONE row), or null if mixed.
        GameMode common = null;
        boolean same = !all.isEmpty();
        for (PlayerListEntry e : all) {
            GameMode g = e.getGameMode();
            if (common == null) common = g;
            else if (g != common) {
                same = false;
                break;
            }
        }
        if (!same) common = null;

        // panel
        ctx.fill(0, 0, width, height, 0xA0000000);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        ctx.fill(px, py, px + pw, py + ph, PANEL);
        ctx.fill(px, py, px + pw, py + 22, HEADER);
        ctx.drawText(textRenderer, "Easy Gamemode", px + 10, py + 7, WHITE, true);
        String count = all.size() + (all.size() == 1 ? " player online" : " players online");
        ctx.drawText(textRenderer, count, px + pw - 10 - textRenderer.getWidth(count), py + 7, GRAY, true);

        // search bar
        searchField.render(ctx, mx, my, delta);
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            ctx.drawText(textRenderer, "Search players...", sfx + 4, sfy + (sfh - 8) / 2, 0xFF6A6A80, false);
        }

        int lx = px + 8, lw = pw - 16;
        int bx0 = lx + lw - 96; // left edge of the four buttons
        int nameX = lx + 28;
        int nameMax = Math.max(20, bx0 - nameX - 4);

        // EVERYONE row
        int ey = py + 50;
        ctx.fill(lx, ey, lx + lw, ey + ROW, GOLD);
        ctx.fill(lx + 1, ey + 1, lx + lw - 1, ey + ROW - 1, 0xFF2A2210);
        ctx.drawText(textRenderer, Text.literal("EVERYONE").formatted(Formatting.BOLD, Formatting.GOLD), lx + 8, ey + 4, GOLD, true);
        ctx.drawText(textRenderer, "all " + all.size() + " online", lx + 8, ey + 14, GRAY, false);
        for (int i = 0; i < 4; i++) {
            final int mi = i;
            modeButton(ctx, bx0 + i * (BTN + GAP), ey + 2, i, common == GamemodeController.MODES[i], mx, my,
                    ey, ey + ROW, "everyone", () -> GamemodeController.set(null, GamemodeController.MODES[mi]));
        }

        // player list
        int ly = ey + ROW + 6;
        int lh = Math.max(10, (py + ph - 26) - ly);
        ctx.fill(lx - 1, ly - 1, lx + lw + 1, ly + lh + 1, 0xFF2A2A40);
        ctx.fill(lx, ly, lx + lw, ly + lh, 0xFF0E0E16);

        maxScroll = Math.max(0, shown.size() * ROW - lh);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ctx.enableScissor(lx, ly, lx + lw, ly + lh);
        if (shown.isEmpty()) {
            String msg = all.isEmpty() ? "Join a server to see players." : "No players match your search.";
            ctx.drawText(textRenderer, msg, lx + (lw - textRenderer.getWidth(msg)) / 2, ly + 10, GRAY, true);
        }
        for (int i = 0; i < shown.size(); i++) {
            PlayerListEntry e = shown.get(i);
            int ry = ly + i * ROW - scroll;
            if (ry + ROW < ly || ry > ly + lh) continue;
            String name = GamemodeController.name(e);
            boolean rowHover = mx >= lx && mx < lx + lw && my >= Math.max(ry, ly) && my < Math.min(ry + ROW, ly + lh);
            ctx.fill(lx, ry, lx + lw, ry + ROW - 2, rowHover ? CARD_HOVER : CARD);
            PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), lx + 6, ry + 3, 16);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(name, nameMax), nameX, ry + (ROW - 2 - 8) / 2, WHITE, true);
            GameMode current = e.getGameMode();
            for (int b = 0; b < 4; b++) {
                final int mi = b;
                modeButton(ctx, bx0 + b * (BTN + GAP), ry + 1, b, current == GamemodeController.MODES[b], mx, my,
                        ly, ly + lh, name, () -> GamemodeController.set(name, GamemodeController.MODES[mi]));
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int total = shown.size() * ROW;
            int barH = Math.max(12, lh * lh / total);
            int barY = ly + (int) ((long) (lh - barH) * scroll / maxScroll);
            ctx.fill(lx + lw - 4, barY, lx + lw - 1, barY + barH, 0xFF6A6A90);
        }

        // footer: status + close button
        String st = GamemodeController.status();
        int sc = GamemodeController.statusColor();
        if (st.isEmpty()) {
            st = "Click a gamemode button. One click, instant change.";
            sc = GRAY;
        }
        ctx.drawText(textRenderer, textRenderer.trimToWidth(st, pw - 84), px + 10, py + ph - 16, sc, true);

        int cbx = px + pw - 58, cby = py + ph - 20, cbw = 50, cbh = 16;
        boolean ch = mx >= cbx && mx < cbx + cbw && my >= cby && my < cby + cbh;
        ctx.fill(cbx, cby, cbx + cbw, cby + cbh, ch ? 0xFFB8B8D0 : 0xFF3A3A4E);
        ctx.fill(cbx + 1, cby + 1, cbx + cbw - 1, cby + cbh - 1, ch ? 0xFF3A2020 : 0xFF241A1A);
        ctx.drawText(textRenderer, "Close", cbx + (cbw - textRenderer.getWidth("Close")) / 2, cby + 4, WHITE, true);
        hits.add(new Hit(cbx, cby, cbx + cbw, cby + cbh, this::close));

        if (tip != null) {
            ctx.drawTooltip(textRenderer, tip, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double cx = click.x(), cy = click.y();
            boolean inField = cx >= sfx && cx < sfx + sfw && cy >= sfy && cy < sfy + sfh;
            if (!inField) searchField.setFocused(false);
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (cx >= h.x1() && cx < h.x2() && cy >= h.y1() && cy < h.y2()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * 20);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Same key again closes the GUI (unless you are typing in the search bar).
        if (armed && !searchField.isFocused() && input.getKeycode() == boundKey()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
