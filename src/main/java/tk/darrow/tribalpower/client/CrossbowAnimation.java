package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.song.PulseCrossbowItem;

/**
 * The Pulse Crossbow carried like the game's own crossbow: cranked low across the body while it loads, held
 * level and sideways once the bolt is seated, and the crossbow arm poses in the third-person view to match.
 * The game keys those motions to its own crossbow item, so the Pulse Crossbow declares them here.
 */
public final class CrossbowAnimation implements IClientItemExtensions {
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (entity.isUsingItem() && entity.getUseItemRemainingTicks() > 0 && entity.getUsedItemHand() == hand)
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        if (!entity.swinging && PulseCrossbowItem.loaded(stack)) return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        return null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
            armTransform(pose, i, equipProcess);
            pose.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
            pose.mulPose(Axis.XP.rotationDegrees(-11.935F));
            pose.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
            pose.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
            float held = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
            float load = Math.min(1.0F, held / TribalConfig.crossbowLoadTicks());
            if (load > 0.1F) pose.translate(0.0F, Mth.sin((held - 0.1F) * 1.3F) * (load - 0.1F) * 0.004F, 0.0F);
            pose.translate(0.0F, 0.0F, load * 0.04F);
            pose.scale(1.0F, 1.0F, 1.0F + load * 0.2F);
            pose.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
        } else {
            float f = -0.4F * Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI);
            float f1 = 0.2F * Mth.sin(Mth.sqrt(swingProcess) * (float) (Math.PI * 2));
            float f2 = -0.2F * Mth.sin(swingProcess * (float) Math.PI);
            pose.translate(i * f, f1, f2);
            armTransform(pose, i, equipProcess);
            attackTransform(pose, i, swingProcess);
            if (PulseCrossbowItem.loaded(stack) && swingProcess < 0.001F && hand == InteractionHand.MAIN_HAND) {
                pose.translate(i * -0.641864F, 0.0F, 0.0F);
                pose.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
            }
        }
        return true;
    }

    private static void armTransform(PoseStack pose, int i, float equip) {
        pose.translate(i * 0.56F, -0.52F + equip * -0.6F, -0.72F);
    }

    private static void attackTransform(PoseStack pose, int i, float swing) {
        float f = Mth.sin(swing * swing * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
        float f1 = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        pose.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
        pose.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
    }
}
