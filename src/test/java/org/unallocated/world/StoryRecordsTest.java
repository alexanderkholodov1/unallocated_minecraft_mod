package org.unallocated.world;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.unallocated.core.SessionState;
import org.unallocated.core.network.CorePayloads.KnowledgePayload;
import static org.junit.jupiter.api.Assertions.*;

class StoryRecordsTest {
    @Test void largeUnicodeRecordsSurviveReloadWithoutOverflowingKnowledgePackets() {
        var state = new SessionState(17); var data = new JsonObject();
        data.addProperty("body", "{custom_name:'víctima 🧵'}".repeat(100));
        String key = "story.creature.world.figure";
        StoryRecords.write(state, key, data);
        var restored = SessionState.fromTag(state.toTag());
        assertEquals(data, StoryRecords.read(restored, key));
        assertEquals(java.util.Set.of(key), restored.flags());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            KnowledgePayload.CODEC.encode(buffer, new KnowledgePayload(restored.flags()));
            assertEquals(restored.flags(), KnowledgePayload.CODEC.decode(buffer).flags());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void replacingShorterRecordsAndRemovingMarkersNeverResurrectsOldData() {
        var state = new SessionState(0); var data = new JsonObject(); data.addProperty("rules", "stone".repeat(100));
        StoryRecords.write(state, "story.world.job.1", data);
        var smaller = new JsonObject(); smaller.addProperty("seed", 17);
        StoryRecords.write(state, "story.world.job.1", smaller);
        assertEquals(smaller, StoryRecords.read(SessionState.fromTag(state.toTag()), "story.world.job.1"));
        StoryRecords.remove(state, "story.world.job.1");
        var restored = SessionState.fromTag(state.toTag()); assertTrue(restored.flags().isEmpty());
        assertThrows(IllegalStateException.class, () -> StoryRecords.read(restored, "story.world.job.1"));
    }
}
