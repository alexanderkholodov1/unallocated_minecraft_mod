package org.unallocated.content;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Our own creature, blocks and sounds: nothing the player meets in the story is a vanilla asset. */
public final class UnallocatedContent {
    public static final String NAMESPACE = "unallocated";

    public static final SoundEvent THREAD_VIBRATE = sound("thread.vibrate");
    public static final SoundEvent THREAD_SNAP = sound("thread.snap");
    public static final SoundEvent RECLAIMER_CLICK = sound("reclaimer.click");
    public static final SoundEvent RECLAIMER_STEP = sound("reclaimer.step");
    public static final SoundEvent RECLAIMER_BREATH = sound("reclaimer.breath");
    public static final SoundEvent RECLAIMER_ROAR = sound("reclaimer.roar");
    public static final SoundEvent RECLAIMER_WHISPER = sound("reclaimer.whisper");
    public static final SoundEvent GLITCH = sound("fx.glitch");
    public static final SoundEvent HEARTBEAT = sound("ui.heartbeat");
    public static final SoundEvent TERMINAL = sound("ui.terminal");
    public static final SoundEvent SHRIEK = sound("screamer.shriek");

    public static final SoundType THREAD_SOUNDS = new SoundType(0.8F, 1.0F, THREAD_SNAP, THREAD_VIBRATE, THREAD_VIBRATE, THREAD_VIBRATE, THREAD_VIBRATE);
    public static final SoundType ROT_SOUNDS = new SoundType(0.6F, 0.7F, RECLAIMER_STEP, RECLAIMER_STEP, RECLAIMER_STEP, RECLAIMER_CLICK, RECLAIMER_STEP);

    public static Block THREAD;
    public static Block WEB;
    public static Block ROT;
    public static EntityType<ReclaimerEntity> RECLAIMER;
    private static AttributeSupplier reclaimerAttributes;

    private UnallocatedContent() {}

    public static void initialize() {
        for (SoundEvent event : new SoundEvent[] {THREAD_VIBRATE, THREAD_SNAP, RECLAIMER_CLICK, RECLAIMER_STEP, RECLAIMER_BREATH,
                RECLAIMER_ROAR, RECLAIMER_WHISPER, GLITCH, HEARTBEAT, TERMINAL, SHRIEK})
            Registry.register(BuiltInRegistries.SOUND_EVENT, event.location(), event);
        THREAD = block("thread", ThreadBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak()
            .noOcclusion().sound(THREAD_SOUNDS).lightLevel(state -> 2).pushReaction(PushReaction.POPPED));
        WEB = block("web", WebBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().forceSolidOn()
            .requiresCorrectToolForDrops().strength(4.0F).noOcclusion().sound(THREAD_SOUNDS).pushReaction(PushReaction.POPPED));
        ROT = block("rot", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(0.6F)
            .speedFactor(0.45F).jumpFactor(0.6F).sound(ROT_SOUNDS));
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id("reclaimer"));
        RECLAIMER = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(ReclaimerEntity::new, MobCategory.MONSTER)
            .sized(1.6F, 3.1F).eyeHeight(2.2F).clientTrackingRange(10).noLootTable().build(key));
    }

    /** Read by the DefaultAttributes mixin, which vanilla consults for every living entity type. */
    public static AttributeSupplier reclaimerAttributes() {
        if (reclaimerAttributes == null) reclaimerAttributes = ReclaimerEntity.createAttributes().build();
        return reclaimerAttributes;
    }

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(NAMESPACE, path); }

    private static SoundEvent sound(String path) { return SoundEvent.createVariableRangeEvent(id(path)); }

    private static Block block(String path, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
