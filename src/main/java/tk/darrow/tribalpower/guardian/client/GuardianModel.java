package tk.darrow.tribalpower.guardian.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianEntity;

/**
 * Animates a guardian by part name, the way the March roster is animated, plus the attack pose its ability
 * calls for: a slam drops the arms, a charge lowers the head, a swoop banks the wings, a sweep swings.
 */
public class GuardianModel extends HierarchicalModel<GuardianEntity> {
    private final ModelPart root;

    public GuardianModel(ModelPart root) { this.root = root; }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(GuardianEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        Guardian guardian = entity.guardian();
        float attack = attackPose(entity, age);
        if (root.hasChild("head")) {
            ModelPart head = root.getChild("head");
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD + (guardian.ability == Guardian.Ability.CHARGE ? attack * 0.6F : 0);
        }
        for (int i = 0; i < 8; i++) if (root.hasChild("leg" + i)) {
            ModelPart leg = root.getChild("leg" + i);
            leg.xRot = Mth.cos(swing * 0.6662F + (i % 2 == 0 ? Mth.PI : 0) + (i / 2) * 0.4F) * amount * 0.8F;
        }
        for (int i = 0; i < 2; i++) {
            if (root.hasChild("arm" + i)) {
                ModelPart arm = root.getChild("arm" + i);
                arm.xRot = Mth.cos(swing * 0.6662F + i * Mth.PI) * amount * 0.5F;
                switch (guardian.ability) {
                    case SLAM -> arm.xRot -= attack * 2.4F;
                    case SWEEP -> arm.yRot = (i == 0 ? -1 : 1) * attack * 1.8F;
                    case SNARE, TIDE -> arm.xRot -= attack * 1.2F;
                    default -> {}
                }
            }
            if (root.hasChild("wing" + i)) {
                ModelPart wing = root.getChild("wing" + i);
                float flap = Mth.sin(age * 0.5F) * 0.7F * (i == 0 ? 1 : -1);
                wing.zRot = guardian.flying ? flap - attack * 0.9F * (i == 0 ? 1 : -1) : Mth.sin(age * 0.2F) * 0.15F * (i == 0 ? 1 : -1);
            }
        }
        for (int i = 0; i < 12; i++) {
            if (root.hasChild("seg" + i)) {
                ModelPart seg = root.getChild("seg" + i);
                seg.yRot = Mth.sin(age * 0.14F - i * 0.5F) * 0.25F + Mth.sin(swing * 0.6F - i * 0.5F) * amount * 0.35F;
            }
            if (root.hasChild("tendril" + i)) {
                ModelPart t = root.getChild("tendril" + i);
                t.xRot = Mth.cos(age * 0.1F + i * 0.7F) * 0.25F - attack * 0.8F;
                t.zRot = Mth.sin(age * 0.08F + i * 0.9F) * 0.25F;
            }
        }
        if (root.hasChild("tail")) root.getChild("tail").yRot = Mth.sin(age * 0.1F) * 0.2F;
        if (root.hasChild("jaw")) root.getChild("jaw").xRot = attack * 0.5F + Mth.sin(age * 0.07F) * 0.05F;
        if (guardian.flying) root.y = Mth.sin(age * 0.1F) * 1.6F;
        if (guardian.ability == Guardian.Ability.FROST && root.hasChild("head")) root.getChild("head").xRot += attack * 0.4F;
        if (entity.isWarded()) root.y -= 1.5F;
        root.xScale = root.yScale = root.zScale = guardian.scale;
        root.y -= (guardian.scale - 1) * 24F;
    }

    /** 1 at the moment of an attack, easing back to 0 over half a second. */
    private static float attackPose(GuardianEntity entity, float age) {
        long now = entity.level().getGameTime();
        int at = entity.attackTime();
        if (at == 0) return 0;
        float since = (now - at) + (age % 1F);
        return since < 0 || since > 12 ? 0 : 1 - since / 12F;
    }
}
