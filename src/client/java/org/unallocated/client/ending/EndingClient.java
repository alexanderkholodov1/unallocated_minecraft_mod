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
    private static com.google.gson.JsonObject story;
    private static Component storyTitle, storyMessage, storySplash;
    private EndingClient() {}
    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> { endingSplash = false; story = null; });
    }
    public static boolean hasEndingSplash() { return endingSplash; }

    public static void storyEnding(com.google.gson.JsonObject envelope) {
        story = envelope;
        var data = org.unallocated.story.StoryData.object(envelope, "data");
        storyTitle = org.unallocated.client.story.StoryClient.text(org.unallocated.story.StoryData.object(data, "title"), envelope);
        var message = Component.empty();
        for (var line : org.unallocated.story.StoryData.array(data, "lines")) {
            if (!message.getString().isEmpty()) message.append("\n");
            message.append(org.unallocated.client.story.StoryClient.text(line.getAsJsonObject(), envelope));
        }
        if (org.unallocated.story.StoryData.bool(data, "quote_agreement", false)) {
            ContractReceiptLookup.read(Minecraft.getInstance().gameDirectory.toPath()).ifPresent(text ->
                message.append("\nDijiste «" + text + "». Gracias por tus datos."));
        }
        storyMessage = message;
        storySplash = org.unallocated.client.story.StoryClient.text(org.unallocated.story.StoryData.object(data, "splash"), envelope);
    }
    public static Component splash() { return story != null ? storySplash : Component.literal(EndingText.SPLASH); }

    /** Called by vanilla's disconnect screen factory; Minecraft still owns the entire teardown. */
    public static Screen screen(DisconnectionDetails details) {
        if (!(details.reason().getContents() instanceof TranslatableContents text)
                || !EndingText.REASON_KEY.equals(text.getKey())) return null;
        var receipt = ContractReceiptLookup.read(Minecraft.getInstance().gameDirectory.toPath());
        endingSplash = true;
        if (story != null) return new DisconnectedScreen(new TitleScreen(), storyTitle, storyMessage, Component.literal(EndingText.RETURN_BUTTON));
        return new DisconnectedScreen(new TitleScreen(), Component.literal(EndingText.TITLE),
            Component.literal(EndingText.message(receipt)), Component.literal(EndingText.RETURN_BUTTON));
    }
}
