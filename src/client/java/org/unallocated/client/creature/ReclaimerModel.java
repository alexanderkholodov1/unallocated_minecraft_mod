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
 * The Reclaimer's body, built from what it has data of: its victims. A hunched human torso carried like a spider's
 * thorax, a sagging abdomen with three more faces sunk into it, a head hanging from a long neck, two arms that are too
 * long, and eight legs whose knees rise above the body. Limbs are solved every frame from hip to foot and stretched to
 * fit ("the skin smears like a generated image gone wrong"), and the pose drops frames because it thinks on the same
 * CPU as the server.
 *
 * Model space: pixels, y grows downward, the ground is y = 24, the front is -z.
 */
public final class ReclaimerModel extends EntityModel<ReclaimerRenderState> {
    private static final int LEGS = 8;
    private static final float SEGMENT = 16F;
    private static final float BODY_Y = -4F;
    private static final float[] HIP_Z = {-7F, -2.5F, 2F, 6.5F};
    private static final float[] FOOT_Z = {-24F, -9F, 7F, 23F};
    private static final float[] FOOT_X = {21F, 26F, 26F, 21F};

    private final ModelPart torso;
    private final ModelPart abdomen;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] upperLegs = new ModelPart[LEGS];
    private final ModelPart[] lowerLegs = new ModelPart[LEGS];
    private final ModelPart[] upperArms = new ModelPart[2];
    private final ModelPart[] foreArms = new ModelPart[2];

    public ReclaimerModel(ModelPart root) {
        super(root);
        torso = root.getChild("torso");
        abdomen = root.getChild("abdomen");
        neck = root.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        for (int i = 0; i < LEGS; i++) {
            upperLegs[i] = root.getChild("upper_leg_" + i);
            lowerLegs[i] = root.getChild("lower_leg_" + i);
        }
        for (int i = 0; i < 2; i++) {
            upperArms[i] = root.getChild("upper_arm_" + i);
            foreArms[i] = root.getChild("fore_arm_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -4F, -9F, 10F, 8F, 18F)
            .texOffs(0, 104).addBox(-1F, -7F, -7F, 2F, 3F, 14F), PartPose.offset(0F, BODY_Y, 0F));
        PartDefinition abdomen = root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 28).addBox(-7F, -6F, 0F, 14F, 12F, 16F),
            PartPose.offsetAndRotation(0F, BODY_Y - 1F, 7F, 0.5F, 0F, 0F));
        abdomen.addOrReplaceChild("face_0", CubeListBuilder.create().texOffs(64, 32).addBox(-3F, -3F, -3F, 6F, 6F, 6F),
            PartPose.offsetAndRotation(-3.5F, -6F, 6F, -0.9F, 0.4F, 0.3F));
        abdomen.addOrReplaceChild("face_1", CubeListBuilder.create().texOffs(88, 32).addBox(-3F, -3F, -3F, 6F, 6F, 6F),
            PartPose.offsetAndRotation(4F, -5.5F, 10F, -1.2F, -0.5F, -0.2F));
        abdomen.addOrReplaceChild("face_2", CubeListBuilder.create().texOffs(100, 12).addBox(-2.5F, -2.5F, -2.5F, 5F, 5F, 5F),
            PartPose.offsetAndRotation(0F, 3F, 16F, 0.3F, 3.1F, 0.9F));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(64, 0).addBox(-2F, -2F, -8F, 4F, 4F, 8F),
            PartPose.offsetAndRotation(0F, BODY_Y - 1F, -8F, 0.6F, 0F, 0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 12).addBox(-4.5F, -5F, -9F, 9F, 10F, 9F),
            PartPose.offsetAndRotation(0F, 0F, -7F, 0.4F, 0F, 0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(100, 0).addBox(-3.5F, 0F, -7F, 7F, 3F, 7F),
            PartPose.offset(0F, 4F, -1.5F));
        for (int i = 0; i < LEGS; i++) {
            root.addOrReplaceChild("upper_leg_" + i, CubeListBuilder.create().texOffs(0, 60).addBox(0F, -1.5F, -1.5F, SEGMENT, 3F, 3F), PartPose.ZERO);
            root.addOrReplaceChild("lower_leg_" + i, CubeListBuilder.create().texOffs(0, 68).addBox(0F, -1F, -1F, SEGMENT, 2F, 2F), PartPose.ZERO);
        }
        for (int i = 0; i < 2; i++) {
            root.addOrReplaceChild("upper_arm_" + i, CubeListBuilder.create().texOffs(0, 74).addBox(0F, -1.5F, -1.5F, SEGMENT, 3F, 3F), PartPose.ZERO);
            root.addOrReplaceChild("fore_arm_" + i, CubeListBuilder.create().texOffs(0, 82).addBox(0F, -1F, -1F, SEGMENT, 2F, 2F)
                .texOffs(40, 82).addBox(SEGMENT - 1F, -2F, -2F, 4F, 4F, 4F), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ReclaimerRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks + state.seed * 37F;
        // Dropped frames: in some windows the whole pose only updates every few ticks, like a model losing frames.
        boolean stutter = hash(Mth.floor(t / 30F)) < 0.4F;
        float tq = stutter ? Mth.floor(t / 4F) * 4F : t;
        float walk = state.walkAnimationPos * 0.55F;
        float stride = Mth.clamp(state.walkAnimationSpeed * 1.4F, 0F, 1F);
        float hunting = Mth.clamp(state.speed * 6F, 0F, 1F);

        // Glitch jitter: a frame now and then is displaced, as if the pose were computed twice.
        float jx = 0F, jz = 0F;
        if (hash(Mth.floor(t) * 7 + 3) < 0.06F) { jx = (hash(Mth.floor(t) + 11) - 0.5F) * 3F; jz = (hash(Mth.floor(t) + 23) - 0.5F) * 3F; }
        float breathe = Mth.sin(tq * 0.09F);
        float bob = -Math.abs(Mth.sin(walk)) * 2.5F * stride;

        torso.x += jx; torso.z += jz; torso.y += bob + breathe * 0.4F;
        torso.yScale = 1F + breathe * 0.05F;
        torso.xRot = -0.08F + hunting * 0.15F;
        abdomen.x += jx; abdomen.z += jz; abdomen.y += bob;
        abdomen.xRot = 0.5F + breathe * 0.06F - hunting * 0.2F;
        abdomen.zRot = Mth.sin(tq * 0.05F) * 0.06F;
        neck.x += jx; neck.z += jz; neck.y += bob;

        // The head follows the player in snaps and sometimes settles at an angle no neck allows.
        float snap = Mth.floor(tq / 9F);
        float twitchYaw = (hash((int) snap * 5 + 1) - 0.5F) * 0.5F;
        float twitchRoll = hash((int) snap * 3 + 2) < 0.18F ? (hash((int) snap + 9) < 0.5F ? -1.25F : 1.25F) : (hash((int) snap + 4) - 0.5F) * 0.3F;
        neck.yRot = Mth.clamp(state.yRot * Mth.DEG_TO_RAD, -1.2F, 1.2F) + twitchYaw * 0.5F;
        neck.xRot = 0.6F + Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -0.6F, 0.6F) * 0.5F;
        head.zRot = twitchRoll;
        head.yRot = twitchYaw;
        jaw.xRot = 0.12F + hunting * 0.85F + (hunting > 0.3F ? Mth.sin(t * 2.3F) * 0.12F : 0F)
            + (hash((int) snap + 31) < 0.15F ? 0.6F : 0F);

        float bodyY = BODY_Y + bob;
        for (int i = 0; i < LEGS; i++) {
            int pair = i / 2;
            float side = i % 2 == 0 ? -1F : 1F;
            float phase = ((pair + (i % 2)) % 2 == 0) ? 0F : Mth.PI;
            float cycle = walk + phase + pair * 0.35F;
            float swing = Mth.cos(cycle) * 7F * stride;
            float lift = Math.max(0F, Mth.sin(cycle)) * 6F * stride;
            float idle = Mth.sin(tq * 0.07F + i) * 0.8F;
            float hipX = side * 5F + jx, hipY = bodyY - 1F, hipZ = HIP_Z[pair] + jz;
            float footX = side * (FOOT_X[pair] + idle), footY = 24F - lift, footZ = FOOT_Z[pair] + swing;
            float kneeX = hipX + (footX - hipX) * 0.42F;
            float kneeZ = hipZ + (footZ - hipZ) * 0.42F;
            float kneeY = Math.min(hipY, footY) - 17F - lift * 0.5F + idle;
            segment(upperLegs[i], hipX, hipY, hipZ, kneeX, kneeY, kneeZ);
            segment(lowerLegs[i], kneeX, kneeY, kneeZ, footX, footY, footZ);
        }

        // Two human arms reaching ahead of it, dragging their knuckles; while hunting they stretch further.
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1F : 1F;
            float sway = Mth.sin(tq * 0.06F + i * 2F) * 2F;
            float shoulderX = side * 4.5F + jx, shoulderY = bodyY + 2F, shoulderZ = -7F + jz;
            float handX = side * (7F + sway * 0.5F), handY = 23F, handZ = -20F - hunting * 8F + sway;
            float elbowX = shoulderX + side * 7F, elbowY = (shoulderY + handY) * 0.5F - 4F, elbowZ = (shoulderZ + handZ) * 0.5F + 2F;
            segment(upperArms[i], shoulderX, shoulderY, shoulderZ, elbowX, elbowY, elbowZ);
            segment(foreArms[i], elbowX, elbowY, elbowZ, handX, handY, handZ);
        }
    }

    /** Places a part's pivot at (x0, y0, z0), points its local +x at (x1, y1, z1) and stretches it to reach. */
    private static void segment(ModelPart part, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-3F) return;
        part.x = x0; part.y = y0; part.z = z0;
        part.xRot = 0F;
        part.yRot = (float) Math.asin(Mth.clamp(-dz / length, -1F, 1F));
        part.zRot = (float) Mth.atan2(dy, dx);
        part.xScale = length / SEGMENT;
    }

    private static float hash(int n) {
        int h = n * 0x27d4eb2d;
        h ^= h >>> 15; h *= 0x2c1b3c6d; h ^= h >>> 12;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
