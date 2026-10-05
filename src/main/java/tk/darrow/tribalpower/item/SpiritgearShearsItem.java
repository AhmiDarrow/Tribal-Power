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
        super(properties.durability(SpiritGear.UNTIERED_DURABILITY)
                .component(net.minecraft.core.component.DataComponents.TOOL, ShearsItem.createToolProperties()));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return SpiritGear.wear(stack, super.damageItem(stack, amount, entity, onBroken));
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
        // Air trims count as paid (see SpiritGear.consumeForMine), so they too leave the blades untouched.
        SpiritGear.Break mined = SpiritGear.mine(stack, level, state, pos, entity, () -> super.mineBlock(stack, level, state, pos, entity));
        if (mined.perks() && entity instanceof ServerPlayer player && SpiritGear.voice(stack).orElse(null) == Attunement.EARTH && foliage(state)) {
            Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
            SpiritGearHooks.aoe(player, stack, pos, axis, SpiritgearShearsItem::foliage);
        }
        return mined.ok();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // A shearing is a paid use: the cell's Pulse instead of the blades, or a starved cut that wears. Air asks neither.
        int[] wear = {0};
        InteractionResult result = SpiritGear.held(stack, wear, () -> super.interactLivingEntity(stack, player, target, hand));
        if (player.level().isClientSide) return result;
        boolean paid = (freeTrim(stack) && result.consumesAction())
                || SpiritGear.settleUse(player, stack, LivingEntity.getSlotForHand(hand), result.consumesAction(), wear[0]);
        if (!result.consumesAction() || !paid) return result;
        Attunement voice = SpiritGear.voice(stack).orElse(null);
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
        if (player == null) return super.useOn(context);
        // A trim the shears make on a block (the game's own) is a paid use like any other.
        ItemStack stack = context.getItemInHand();
        int[] wear = {0};
        InteractionResult result = SpiritGear.held(stack, wear, () -> super.useOn(context));
        if (!freeTrim(stack) || !result.consumesAction())
            SpiritGear.settleUse(player, stack, LivingEntity.getSlotForHand(context.getHand()), result.consumesAction(), wear[0]);
        return result;
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
