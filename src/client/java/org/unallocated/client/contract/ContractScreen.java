package org.unallocated.client.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.unallocated.contract.ContractSubmission;

/** Static, silent agreement; warnings are visible without opening the optional data list. */
public final class ContractScreen extends Screen {
    private final Screen next;
    private EditBox input;
    private String draft = "";
    private boolean reading;
    private int page;
    private List<FormattedCharSequence> dataLines = List.of();
    private int linesPerPage;

    public ContractScreen(Screen next) {
        super(text("title"));
        this.next = next;
    }

    private static Component text(String key) {
        return Component.translatable("contract.unallocated." + key);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, width - 40);
        int left = (width - contentWidth) / 2;
        if (reading) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (String key : List.of("uses.current", "uses.local", "uses.absent", "uses.future", "uses.lore")) {
                lines.addAll(font.split(text(key), contentWidth));
                lines.add(FormattedCharSequence.EMPTY);
            }
            dataLines = List.copyOf(lines);
            linesPerPage = Math.max(1, (height - 100) / 11);
            page = Math.min(page, pageCount() - 1);
            int navWidth = (contentWidth - 8) / 3;
            var previous = addRenderableWidget(new ContractButton(left, height - 72, navWidth,
                    text("previous"), button -> changePage(-1)));
            previous.active = page > 0;
            addRenderableWidget(new ContractButton(left + navWidth + 4, height - 72, navWidth,
                    text("back"), button -> showAgreement()));
            var forward = addRenderableWidget(new ContractButton(left + 2 * (navWidth + 4), height - 72,
                    navWidth, text("next"), button -> changePage(1)));
            forward.active = page + 1 < pageCount();
        } else {
            input = addRenderableWidget(new EditBox(font, left, height - 79, contentWidth, 20, text("input")));
            input.setMaxLength(64);
            input.setValue(draft);
            input.setHint(text("input"));
            input.setResponder(value -> draft = value);
            addRenderableWidget(new ContractButton(left, height - 53, contentWidth,
                    text("read"), button -> {
                        reading = true;
                        rebuildWidgets();
                    }));
            // Immediate typing works; an empty Enter is the same agreement as the button.
            setInitialFocus(input);
        }
        addRenderableWidget(new ContractButton(left, height - 27, contentWidth,
                text("accept"), button -> accept(false)));
    }

    private int pageCount() {
        return Math.max(1, (dataLines.size() + linesPerPage - 1) / linesPerPage);
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private void showAgreement() {
        reading = false;
        rebuildWidgets();
    }

    private void accept(boolean typed) {
        ContractClient.accept(new ContractSubmission(
                typed ? ContractSubmission.Method.TYPED : ContractSubmission.Method.CLICKED, draft), next);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!reading && input.isFocused() && event.isConfirmation()) {
            if (ContractSubmission.isAgreement(draft)) accept(true);
            else if (draft.isEmpty()) accept(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return reading;
    }

    @Override
    public void onClose() {
        // Escape only leaves the optional list. It never fabricates acceptance.
        if (reading) showAgreement();
    }

    @Override
    public Component getNarrationMessage() {
        return getTitle().copy().append(". ").append(text("warnings")).append(". ").append(text("body"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF111519);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int contentWidth = Math.min(420, width - 40);
        int left = (width - contentWidth) / 2;
        graphics.text(font, reading ? text("uses.title") : getTitle(), left, 14, 0xFFE3DED2, false);
        if (reading) {
            int end = Math.min(dataLines.size(), (page + 1) * linesPerPage);
            int y = 34;
            for (int i = page * linesPerPage; i < end; i++) {
                graphics.text(font, dataLines.get(i), left, y, 0xFFE3DED2, false);
                y += 11;
            }
            graphics.text(font, Component.literal((page + 1) + " / " + pageCount()),
                    left, height - 88, 0xFFB6B6AF, false);
        } else {
            int y = drawWrapped(graphics, text("warnings"), left, 35, contentWidth, 0xFFFFCD89);
            drawWrapped(graphics, text("body"), left, y + 8, contentWidth, 0xFFE3DED2);
            graphics.text(font, text("input.hint"), left, height - 93, 0xFFB6B6AF, false);
        }
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Component content, int x, int y, int maxWidth, int color) {
        for (FormattedCharSequence line : font.split(content, maxWidth)) {
            graphics.text(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private static final class ContractButton extends Button.Plain {
        private ContractButton(int x, int y, int width, Component label, Button.OnPress action) {
            super(x, y, width, 20, label, action, Supplier::get);
        }
    }
}
