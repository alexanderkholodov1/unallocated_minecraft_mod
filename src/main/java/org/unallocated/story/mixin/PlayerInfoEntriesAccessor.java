package org.unallocated.story.mixin;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla builds player-list packets only from real players; the server's fake roster needs its own entries. */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoEntriesAccessor {
    @Mutable
    @Accessor("entries")
    void unallocated$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
