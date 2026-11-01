package org.unallocated.client.launcher;

import java.util.List;
import java.util.function.Supplier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Before the world: who you are and why you are joining (LORE 3, three arrivals drawn per copy), told slowly on black,
 * then the server as it appears in a multiplayer list. A player who knows nothing about the mod gets the context here.
 */
public final class IntroScreen extends Screen {
    public static final List<String> ARRIVALS = List.of("explorer", "former", "invited");
    private static final int CARDS = 4;
    private static final int CHARS_PER_TICK = 2;
    private final String arrival;
    private final Runnable join;
    private int card;
    private int shown;
    private boolean list;

    public IntroScreen(String arrival, Runnable join) {
        super(Component.translatable("intro.unallocated.title"));
        this.arrival = arrival;
        this.join = join;
    }

    private Component cardText(int index) {
        return Component.translatable("intro.unallocated." + arrival + "." + index);
    }

    @Override
    protected void init() {
        clearWidgets();
        if (list) {
            int w = Math.min(305, width - 40);
            addRenderableWidget(new Button.Plain((width - w) / 2, height / 2 + 34, w, 20,
                Component.translatable("intro.unallocated.join"), button -> join.run(), Supplier::get));
        }
    }

    @Override
    public void tick() {
        if (!list) shown += CHARS_PER_TICK;
    }

    private void advance() {
        int length = cardText(card).getString().length();
        if (shown < length) { shown = length; return; }
        if (card + 1 < CARDS) { card++; shown = 0; return; }
        list = true;
        init();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!list) { advance(); return true; }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!list && (event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN)) { advance(); return true; }
        if (!list && event.key() == InputConstants.KEY_ESCAPE) { list = true; init(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF050505);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (list) { serverRow(graphics, mouseX, mouseY); return; }
        int w = Math.min(360, width - 60);
        int left = (width - w) / 2;
        String full = cardText(card).getString();
        Component visible = Component.literal(full.substring(0, Math.min(full.length(), shown)));
        List<FormattedCharSequence> lines = font.split(visible, w);
        int y = height / 2 - lines.size() * 6;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, left, y, 0xFFD8D4CB, false);
            y += 12;
        }
        if (shown >= full.length()) {
            graphics.text(font, Component.translatable("intro.unallocated.next"), left, height - 30, 0xFF6E6A63, false);
        }
    }

    /** One row of a multiplayer list, as the abandoned server shows itself: 0/20, yet names on hover. */
    private void serverRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int w = Math.min(305, width - 40);
        int left = (width - w) / 2;
        int top = height / 2 - 30;
        graphics.text(font, Component.translatable("intro.unallocated.list"), left, top - 22, 0xFFFFFFFF, false);
        graphics.fill(left - 2, top - 2, left + w + 2, top + 36, 0xFF2B2B2B);
        graphics.fill(left, top, left + 32, top + 32, 0xFF3C5A2E);
        graphics.fill(left + 6, top + 6, left + 26, top + 26, 0xFF5C8A3F);
        graphics.fill(left + 12, top + 12, left + 20, top + 20, 0xFF2A1A12);
        graphics.text(font, Component.literal("ValleCraft Network"), left + 36, top + 1, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable("intro.unallocated.motd.0"), left + 36, top + 12, 0xFFAAAAAA, false);
        graphics.text(font, Component.translatable("intro.unallocated.motd.1"), left + 36, top + 22, 0xFF7F7F7F, false);
        Component count = Component.literal("0/20");
        int countX = left + w - font.width(count) - 14;
        graphics.text(font, count, countX, top + 1, 0xFFAAAAAA, false);
        for (int bar = 0; bar < 5; bar++) {
            int h = 2 + bar * 2;
            graphics.fill(left + w - 11 + bar * 2, top + 9 - h, left + w - 10 + bar * 2, top + 9, 0xFF55FF55);
        }
        graphics.text(font, Component.literal("26.3"), left + w - font.width("26.3"), top + 22, 0xFF7F7F7F, false);
        if (mouseX >= countX && mouseX <= countX + font.width(count) && mouseY >= top && mouseY <= top + 10) {
            int y = mouseY + 12;
            graphics.fill(mouseX + 6, y - 2, mouseX + 70, y + 7 * 10 + 1, 0xF0100010);
            for (String name : List.of("Kai", "Noor", "Efe", "Makena", "Zuri", "Sunny", "Ari")) {
                graphics.text(font, Component.literal(name), mouseX + 9, y, 0xFFAAAAAA, false);
                y += 10;
            }
        }
    }
}
