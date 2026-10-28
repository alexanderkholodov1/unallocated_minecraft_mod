package org.unallocated.story.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.story.StoryRuntime;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class StoryChatMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    private void unallocated$chat(PlayerChatMessage message, CallbackInfo ci) {
        if (StoryRuntime.get(player.level().getServer()).chat(player, message.signedContent())) ci.cancel();
    }
}
