package org.unallocated.client.creature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The death screen again, corrupted: the protocol asks for consent before every new trial. */
public final class ReviveScreen extends Screen {
    public ReviveScreen() {
        super(Component.translatable("puppet.unallocated.title"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("death.unallocated.consent"), button -> {
                Minecraft client = Minecraft.getInstance();
                if (client.getConnection() != null) client.getConnection().sendCommand("unallocated revive");
                onClose();
            }).bounds(width / 2 - 100, height / 4 + 72, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("death.unallocated.not_yet"), button -> onClose()).bounds(width / 2 - 100, height / 4 + 96, 200, 20).build());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA0400000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Component title = getTitle();
        graphics.text(font, title, (width - font.width(title)) / 2, height / 4 - 4, 0xFFFFFFFF, true);
        Component line = Component.translatable("puppet.unallocated.subject");
        graphics.text(font, line, (width - font.width(line)) / 2, height / 4 + 20, 0xFFE05050, true);
    }
}
