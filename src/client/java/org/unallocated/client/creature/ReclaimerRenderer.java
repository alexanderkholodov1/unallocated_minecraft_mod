package org.unallocated.client.creature;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.unallocated.content.ReclaimerEntity;
import org.unallocated.content.UnallocatedContent;

/** Draws the Reclaimer with its own model and texture, and its eyes always lit, even in the dark. */
public final class ReclaimerRenderer extends MobRenderer<ReclaimerEntity, ReclaimerRenderState, ReclaimerModel> {
    private static final Identifier TEXTURE = UnallocatedContent.id("textures/entity/reclaimer.png");
    private static final RenderType EYES = RenderTypes.eyes(UnallocatedContent.id("textures/entity/reclaimer_eyes.png"));

    public ReclaimerRenderer(EntityRendererProvider.Context context) {
        super(context, new ReclaimerModel(ReclaimerModel.createLayer().bakeRoot()), 1.2F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() { return EYES; }
        });
    }

    @Override
    public Identifier getTextureLocation(ReclaimerRenderState state) { return TEXTURE; }

    @Override
    public ReclaimerRenderState createRenderState() { return new ReclaimerRenderState(); }

    @Override
    public void extractRenderState(ReclaimerEntity entity, ReclaimerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.speed = (float) entity.speed();
        state.seed = entity.getId();
    }
}
