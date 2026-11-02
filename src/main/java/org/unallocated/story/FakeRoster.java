package org.unallocated.story;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.unallocated.story.mixin.PlayerInfoEntriesAccessor;

/**
 * The server's player list (Tab), as a real public server shows it: a header and footer, and names that should not be
 * there (NARRATIVE 3, MAP 1). The names are only list entries; no entity exists for them.
 */
public final class FakeRoster {
    private FakeRoster() {}

    public static UUID id(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static void add(ServerPlayer player, String name, int latency) {
        var actions = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE);
        var packet = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(id(name), new GameProfile(id(name), name), true, latency,
            GameType.SURVIVAL, null, true, 0, null);
        ((PlayerInfoEntriesAccessor) (Object) packet).unallocated$setEntries(List.of(entry));
        player.connection.send(packet);
    }

    /** You, a second time: same name, another entry (the one that stayed last time). */
    public static void echo(ServerPlayer player) {
        String name = player.getGameProfile().name();
        UUID echo = UUID.nameUUIDFromBytes(("Echo:" + name).getBytes(StandardCharsets.UTF_8));
        var actions = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE);
        var packet = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(echo, new GameProfile(echo, name), true, 999,
            GameType.SURVIVAL, null, true, 0, null);
        ((PlayerInfoEntriesAccessor) (Object) packet).unallocated$setEntries(List.of(entry));
        player.connection.send(packet);
    }

    public static void remove(ServerPlayer player, String name) {
        player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(id(name))));
    }

    /** Header and footer are language keys, resolved by the client in its own language. */
    public static void frame(ServerPlayer player) {
        player.connection.send(new ClientboundTabListPacket(Component.translatable("tab.unallocated.header"),
            Component.translatable("tab.unallocated.footer")));
    }
}
