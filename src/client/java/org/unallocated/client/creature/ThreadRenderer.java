package org.unallocated.client.creature;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.unallocated.content.ThreadEntity;
import org.unallocated.content.UnallocatedContent;

/** Threads exist for everyone but only a Puppet sees them (LORE 1.10); they glow faintly in the dark. */
public final class ThreadRenderer extends MobRenderer<ThreadEntity, ThreadRenderState, ThreadModel> {
    private static final Identifier TEXTURE = UnallocatedContent.id("textures/entity/thread.png");
    private static final RenderType GLOW = RenderTypes.eyes(TEXTURE);

    public ThreadRenderer(EntityRendererProvider.Context context) {
        super(context, new ThreadModel(ThreadModel.createLayer().bakeRoot()), 0F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() { return GLOW; }
        });
    }

    @Override
    public boolean shouldRender(ThreadEntity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return ThreadEntity.clientPuppet && super.shouldRender(entity, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    protected AABB getBoundingBoxForCulling(ThreadEntity entity, float partialTicks) {
        var span = entity.span();
        return entity.getBoundingBox().inflate(Math.abs(span.x()) / 2 + 1, Math.abs(span.y()) / 2 + 1, Math.abs(span.z()) / 2 + 1);
    }

    @Override
    public Identifier getTextureLocation(ThreadRenderState state) { return TEXTURE; }

    @Override
    public ThreadRenderState createRenderState() { return new ThreadRenderState(); }

    @Override
    public void extractRenderState(ThreadEntity entity, ThreadRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        var span = entity.span();
        state.dx = span.x(); state.dy = span.y(); state.dz = span.z();
    }

    @Override
    protected boolean shouldShowName(ThreadEntity entity, double distanceToCameraSq) { return false; }
}
