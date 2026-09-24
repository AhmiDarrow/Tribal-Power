package tk.darrow.tribalpower.client;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import tk.darrow.tribalpower.item.WeaponKind;

/**
 * The five arm poses the Spiritgear weapons swing with, one per {@link WeaponKind.Swing}, added to
 * {@link HumanoidModel.ArmPose} through {@code META-INF/enumextensions.json}. Each pose runs
 * {@link WeaponAnimations#thirdPerson} for its swing.
 */
public final class WeaponPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> THRUST = proxy(WeaponKind.Swing.THRUST);
    public static final EnumProxy<HumanoidModel.ArmPose> STAB = proxy(WeaponKind.Swing.STAB);
    public static final EnumProxy<HumanoidModel.ArmPose> CHOP = proxy(WeaponKind.Swing.CHOP);
    public static final EnumProxy<HumanoidModel.ArmPose> SWEEP = proxy(WeaponKind.Swing.SWEEP);
    public static final EnumProxy<HumanoidModel.ArmPose> SLASH = proxy(WeaponKind.Swing.SLASH);

    private WeaponPoses() {}

    private static EnumProxy<HumanoidModel.ArmPose> proxy(WeaponKind.Swing swing) {
        IArmPoseTransformer transformer = (model, entity, arm) -> WeaponAnimations.thirdPerson(model, entity, arm, swing);
        return new EnumProxy<>(HumanoidModel.ArmPose.class, false, transformer);
    }

    public static HumanoidModel.ArmPose of(WeaponKind.Swing swing) {
        return switch (swing) {
            case THRUST -> THRUST.getValue();
            case STAB -> STAB.getValue();
            case CHOP -> CHOP.getValue();
            case SWEEP -> SWEEP.getValue();
            case SLASH -> SLASH.getValue();
        };
    }
}
