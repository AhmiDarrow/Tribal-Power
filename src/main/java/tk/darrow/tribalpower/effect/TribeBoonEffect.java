package tk.darrow.tribalpower.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * A tribe's boon: a small, flavourful perk in that tribe's way of doing things. Kin at Kin standing carry it
 * always; food and rites hand it out for a while.
 * <ul>
 *   <li><b>Soil</b> (Pad-keepers): your fields are never trampled, and you go hungry slower.</li>
 *   <li><b>Stone</b> (Grit-singers): ore you break sometimes gives a little more.</li>
 *   <li><b>Sprout</b> (Rootbinders): crops and saplings grow faster around you.</li>
 *   <li><b>Claw</b> (Edge-walkers): no fall hurts you while you sneak.</li>
 *   <li><b>Spark</b> (Drumhearts): a quicker hand: you swing faster.</li>
 *   <li><b>Clock</b> (Pattern-weavers): Echo stations near you work faster.</li>
 *   <li><b>Swarm</b> (Colony-keepers): bees never turn on you.</li>
 *   <li><b>Sigil</b> (Seal-carvers): your charms ask half the Pulse.</li>
 *   <li><b>Spindle</b> (Loom-stitchers): songs cost less to sing.</li>
 * </ul>
 */
public class TribeBoonEffect extends MobEffect {
    public final TribeDefinition tribe;
    private final ResourceLocation id;

    public TribeBoonEffect(TribeDefinition tribe) {
        super(MobEffectCategory.BENEFICIAL, tribe.colour());
        this.tribe = tribe;
        this.id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, tribe.id() + "_boon");
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(attributes, amplifier);
        if (tribe != TribeDefinition.SPARK) return;
        var speed = attributes.getInstance(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        speed.removeModifier(id);
        double amount = TribalConfig.sparkBoonAttackSpeed();
        if (amount != 0) speed.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        super.removeAttributeModifiers(attributes);
        var speed = attributes.getInstance(Attributes.ATTACK_SPEED);
        if (speed != null) speed.removeModifier(id);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    /** Every two seconds: Sprout hurries the growing things nearby; Soil keeps the belly full a little longer. */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        switch (tribe) {
            case SPROUT -> {
                int reach = TribalConfig.sproutBoonReach();
                BlockPos at = entity.blockPosition();
                var random = level.random;
                for (int i = 0; i < TribalConfig.sproutBoonTicks(); i++) {
                    BlockPos pos = at.offset(random.nextInt(reach * 2 + 1) - reach, random.nextInt(3) - 1, random.nextInt(reach * 2 + 1) - reach);
                    BlockState state = level.getBlockState(pos);
                    if ((state.getBlock() instanceof CropBlock || state.getBlock() instanceof SaplingBlock)
                            && state.getBlock() instanceof BonemealableBlock growing && growing.isValidBonemealTarget(level, pos, state))
                        state.randomTick(level, pos, random);
                }
            }
            case SOIL -> {
                if (entity instanceof Player player && player.getFoodData().getSaturationLevel() < 1
                        && level.random.nextDouble() < TribalConfig.soilBoonSaturation())
                    player.getFoodData().setSaturation(Math.min(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel() + 1));
            }
            default -> {}
        }
        return true;
    }
}
