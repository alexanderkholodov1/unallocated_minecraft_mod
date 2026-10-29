package org.unallocated.client.interfacefx;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;

/** Narrative is in resource language files, independent of the player's UI language. */
public final class InterfaceTexts {
    public enum LeakLanguage { ES, EN, MIXED;
        public static LeakLanguage parse(String value) { return valueOf(value.toUpperCase(Locale.ROOT)); }
    }

    private InterfaceTexts() { }

    public static Component line(String channel, int generation, LeakLanguage language, Object... args) {
        String variant = language.name().toLowerCase(Locale.ROOT);
        return Component.translatable("interface.unallocated." + channel + "." + variant + ".g" + generation, args);
    }

    public static Component inventedAdvancement(int generation, LeakLanguage language, long seed) {
        // Each generation has its own short vocabulary; seed variants never hide the ladder in a suffix.
        String variant = language.name().toLowerCase(Locale.ROOT);
        return Component.translatable("interface.unallocated.generated." + variant + ".g" + generation
                + "." + Math.floorMod(seed, 4));
    }

    /** Resource packs may replace our short titles. Preserve whole words if their text exceeds the native line. */
    public static FormattedText fitInventedTitle(Font font, FormattedText title, int width) {
        if (font.width(title) <= width) return title;
        String text = title.getString().strip();
        while (!text.isEmpty() && font.width(text + "…") > width) {
            int boundary = text.lastIndexOf(' ');
            text = boundary < 0 ? "" : text.substring(0, boundary).stripTrailing();
        }
        return Component.literal(font.width("…") <= width ? text + "…" : "");
    }
}
