package org.unallocated.content;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.unallocated.creature.Creatures;

/**
 * One of the Reclaimer's threads: a listener stretched across the world. Touching it makes it vibrate, which the
 * creature feels; running through it is louder. Shears cut it silently (Adventure mode allows it through can_break).
 */
public final class ThreadBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape ALONG_X = Block.box(0, 0, 6, 16, 3, 10);
    private static final VoxelShape ALONG_Z = Block.box(6, 0, 0, 10, 3, 16);
    private static final DustParticleOptions PULSE = new DustParticleOptions(0x8A1020, 0.7F);
    private static final Map<Entity, Long> LAST_TOUCH = new WeakHashMap<>();

    public ThreadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player) || player.isSpectator()) return;
        long now = server.getGameTime();
        Long last = LAST_TOUCH.get(player);
        boolean loud = player.isSprinting();
        if (last != null && now - last < (loud ? 8 : 16)) return;
        LAST_TOUCH.put(player, now);
        if (player.isShiftKeyDown()) return;
        server.playSound(null, pos, UnallocatedContent.THREAD_VIBRATE, SoundSource.BLOCKS, loud ? 1.4F : 0.7F,
            0.75F + server.getRandom().nextFloat() * 0.5F);
        server.sendParticles(PULSE, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, loud ? 14 : 6, 0.45, 0.05, 0.45, 0.0);
        Creatures.vibration(player, loud);
    }
}
