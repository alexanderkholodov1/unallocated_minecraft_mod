package org.unallocated.client.ending;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.unallocated.ending.ContractReceiptLookup;
import org.unallocated.ending.EndingText;

/** Only session memory. No options, resource packs, splash files or consent files are written. */
public final class EndingClient {
    private static boolean endingSplash;
    private EndingClient() {}
    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> endingSplash = false);
    }
    public static boolean hasEndingSplash() { return endingSplash; }

    /** Called by vanilla's disconnect screen factory; Minecraft still owns the entire teardown. */
    public static Screen screen(DisconnectionDetails details) {
        if (!(details.reason().getContents() instanceof TranslatableContents text)
                || !EndingText.REASON_KEY.equals(text.getKey())) return null;
        var receipt = ContractReceiptLookup.read(Minecraft.getInstance().gameDirectory.toPath());
        endingSplash = true;
        return new DisconnectedScreen(new TitleScreen(), Component.literal(EndingText.TITLE),
            Component.literal(EndingText.message(receipt)), Component.literal(EndingText.RETURN_BUTTON));
    }
}
