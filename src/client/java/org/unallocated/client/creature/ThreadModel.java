package org.unallocated.client.creature;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A taut thread made of short stretched segments: it sags a little in the middle and trembles, as if something at the
 * other end were listening. Model space: pixels, y down, the entity's feet at y = 24, world +z is model -z.
 */
public final class ThreadModel extends EntityModel<ThreadRenderState> {
    private static final int SEGMENTS = 10;
    private static final float LENGTH = 16F;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];
    private final ModelPart knot;
    /** The strand that rises from the knot into the sky, where the Reclaimer listens. */
    private final ModelPart sky;

    public ThreadModel(ModelPart root) {
        super(root);
        for (int i = 0; i < SEGMENTS; i++) segments[i] = root.getChild("segment_" + i);
        knot = root.getChild("knot");
        sky = root.getChild("sky");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < SEGMENTS; i++)
            root.addOrReplaceChild("segment_" + i, CubeListBuilder.create().texOffs(0, 0).addBox(0F, -0.4F, -0.4F, LENGTH, 0.8F, 0.8F), PartPose.ZERO);
        root.addOrReplaceChild("sky", CubeListBuilder.create().texOffs(0, 0).addBox(0F, -0.3F, -0.3F, LENGTH, 0.6F, 0.6F), PartPose.ZERO);
        root.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(0, 4).addBox(-1.5F, -1.5F, -1.5F, 3F, 3F, 3F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 16);
    }

    @Override
    public void setupAnim(ThreadRenderState state) {
        super.setupAnim(state);
        float sx = state.dx * 8F, sy = -state.dy * 8F, sz = -state.dz * 8F;
        float span = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        float tremble = Mth.sin(state.ageInTicks * 0.9F) * 0.25F;
        float px = -sx, py = 14F - sy, pz = -sz;
        for (int i = 0; i < SEGMENTS; i++) {
            float t1 = (i + 1) / (float) SEGMENTS;
            float sag = Mth.sin(t1 * Mth.PI) * (span * 0.06F + tremble);
            float x = -sx + 2F * sx * t1, y = 14F - sy + 2F * sy * t1 + sag, z = -sz + 2F * sz * t1;
            segment(segments[i], px, py, pz, x, y, z);
            px = x; py = y; pz = z;
        }
        knot.y = 14F + Mth.sin(0.5F * Mth.PI) * (span * 0.06F + tremble);
        knot.yRot = state.ageInTicks * 0.05F;
        float sway = Mth.sin(state.ageInTicks * 0.05F) * 6F;
        segment(sky, 0F, knot.y, 0F, sway, knot.y - 640F, sway * 0.5F);
    }

    private static void segment(ModelPart part, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-3F) return;
        part.x = x0; part.y = y0; part.z = z0;
        part.yRot = (float) Math.asin(Mth.clamp(-dz / length, -1F, 1F));
        part.zRot = (float) Mth.atan2(dy, dx);
        part.xScale = length / LENGTH;
    }
}
