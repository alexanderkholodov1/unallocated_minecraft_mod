package org.unallocated.client.ending;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The ending told like the intro: on black, written letter by letter, one paragraph at a time. */
public final class EndingScreen extends Screen {
    private static final int CHARS_PER_TICK = 2;
    private final Component heading;
    private final List<String> cards = new ArrayList<>();
    private final Component back;
    private int card, shown;
    private boolean done;

    public EndingScreen(Component heading, Component message, Component back) {
        super(heading);
        this.heading = heading; this.back = back;
        for (String part : message.getString().split("\n")) if (!part.isBlank()) cards.add(part.strip());
        if (cards.isEmpty()) cards.add("");
    }

    @Override
    protected void init() {
        clearWidgets();
        if (done) addRenderableWidget(Button.builder(back, b -> minecraft.gui.setScreen(new TitleScreen())).bounds(width / 2 - 100, height - 40, 200, 20).build());
    }

    @Override
    public void tick() { if (!done) shown += CHARS_PER_TICK; }

    private void advance() {
        if (shown < cards.get(card).length()) { shown = cards.get(card).length(); return; }
        if (card + 1 < cards.size()) { card++; shown = 0; return; }
        done = true; init();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!done) { advance(); return true; }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!done && (event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN)) { advance(); return true; }
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
        int w = Math.min(380, width - 60), left = (width - w) / 2;
        graphics.text(font, heading, (width - font.width(heading)) / 2, 28, 0xFFB01020, false);
        String full = cards.get(card);
        List<FormattedCharSequence> lines = font.split(Component.literal(full.substring(0, Math.min(full.length(), shown))), w);
        int y = height / 2 - lines.size() * 6;
        for (FormattedCharSequence line : lines) { graphics.text(font, line, left, y, 0xFFD8D4CB, false); y += 12; }
        if (!done && shown >= full.length())
            graphics.text(font, Component.translatable("intro.unallocated.next"), left, height - 30, 0xFF6E6A63, false);
        graphics.text(font, Component.literal((card + 1) + "/" + cards.size()), width - 40, height - 30, 0xFF3A3A3A, false);
    }
}
