package org.unallocated.client.interfacefx;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.resources.language.I18n;
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
        var random = new Random(seed);
        // Seeded grammatical fragments intentionally degrade; no external model/service is claimed.
        String variant = language.name().toLowerCase(Locale.ROOT);
        String prefix = I18n.get("interface.unallocated.generated.prefix." + variant + "." + random.nextInt(4));
        String object = I18n.get("interface.unallocated.generated.object." + variant + "." + random.nextInt(4));
        if (generation == 5) object = prefix + " / " + object;
        return Component.translatable("interface.unallocated.generated.title", prefix, object,
                String.format(Locale.ROOT, "%04X", seed & 0xffff), generation);
    }
}
