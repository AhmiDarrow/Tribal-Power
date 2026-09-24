package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/**
 * Spiritgear shears — Pulse spares the blades, and a linked totem voice changes what a trim is worth.
 * Earth takes the whole 3x3 face of foliage, Air trims for free, Water leaves a sheep its fleece,
 * Fire smokes a hive calm, Spirit mends what it shears and Loom sends the cuttings to hand.
 */
public class SpiritgearShearsItem extends ShearsItem {
    public SpiritgearShearsItem(Properties properties) {
        // Shears carry their mining rules in the TOOL component, not in a speed override the way the
        // tiered tools do. Without it these would cut nothing: no leaves, no wool, no cobweb drop.
        super(properties.durability(SpiritGear.TOOL_DURABILITY)
                .component(net.minecraft.core.component.DataComponents.TOOL, ShearsItem.createToolProperties()));
    }

    /** A free trim: the Air voice pays neither Pulse nor edge. */
    public static boolean freeTrim(ItemStack stack) {
        return SpiritGear.voice(stack).orElse(null) == Attunement.AIR;
    }

    static boolean foliage(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL) || state.is(Blocks.COBWEB)
                || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return SpiritGear.destroySpeed(stack, super.getDestroySpeed(stack, state));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.getAbilities().instabuild
                || state.getDestroySpeed(level, pos) == 0.0F) {
            return super.mineBlock(stack, level, state, pos, entity);
        }
        SpiritGear.Swing parent = SpiritGear.swingFor(player);
        boolean aoe = parent != null && parent.aoe();
        boolean paid = parent != null ? parent.pulsePaid()
                : freeTrim(stack) || SpiritGear.consumeForMine(player, stack);
        if (parent == null) SpiritGear.beginSwing(player, stack, paid, false);
        int before = stack.getDamageValue();
        boolean ok = super.mineBlock(stack, level, state, pos, entity);
        // Air asks nothing of the blades: undo the point vanilla shears take for every block.
        if (freeTrim(stack) && !stack.isEmpty()) stack.setDamageValue(before);
        if (!freeTrim(stack)) SpiritGear.finishDurability(player, stack, paid);
        if (ok && paid && !aoe && SpiritGear.voice(stack).orElse(null) == Attunement.EARTH && foliage(state)) {
            Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
            SpiritGearHooks.aoe(player, stack, pos, axis, SpiritgearShearsItem::foliage);
        }
        return ok;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        int before = stack.getDamageValue();
        InteractionResult result = super.interactLivingEntity(stack, player, target, hand);
        if (!result.consumesAction() || player.level().isClientSide || player.getAbilities().instabuild) return result;
        Attunement voice = SpiritGear.voice(stack).orElse(null);
        if (freeTrim(stack)) {
            // Air asks nothing of the blades: undo the point vanilla shears take for a shearing.
            if (!stack.isEmpty()) stack.setDamageValue(before);
        } else if (GearCell.spend(player, stack, SpiritGear.useCost(stack))) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
        } else {
            if (!SpiritGear.skipStarveHurt(stack)) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            SpiritgearHelper.notifyStarved(player);
            return result;
        }
        if (voice == Attunement.WATER && target instanceof Sheep sheep && sheep.isSheared()
                && (SpiritGear.rank(stack) >= 3 || player.getRandom().nextFloat() < 0.5F)) {
            sheep.setSheared(false);
        }
        if (voice == Attunement.SPIRIT) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        return result;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // Fire smokes a hive calm: the bees stay home instead of swarming the shearer.
        boolean hive = level.getBlockState(pos).is(BlockTags.BEEHIVES)
                && SpiritGear.voice(context.getItemInHand()).orElse(null) == Attunement.FIRE;
        if (hive && player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity beehive) {
            beehive.emptyAllLivingFromHive(server, level.getBlockState(pos),
                    net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
        }
        return super.useOn(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !selected || !(entity instanceof ServerPlayer player)) return;
        if (SpiritGear.voice(stack).orElse(null) != Attunement.SPIRIT || level.getGameTime() % 80 != 0) return;
        int range = SpiritGear.rank(stack) >= 3 ? 16 : 10;
        for (LivingEntity mob : ((ServerLevel) level).getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                mob -> mob instanceof net.neoforged.neoforge.common.IShearable shearable
                        && shearable.isShearable(player, stack, level, mob.blockPosition()))) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        tooltip.add(Component.translatable("item.tribalpower.spiritgear_shears.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
