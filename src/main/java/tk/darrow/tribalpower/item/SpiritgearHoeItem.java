package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/**
 * Spiritgear hoe — Pulse spares the blade, and using it on a ripe crop reaps and sows it again in one motion.
 * Earth turns the whole 3x3 of soil, Water leaves its furrows wet, Air reaps a 3x3 of crops, Spirit doubles
 * part of the yield and Loom reaps a 5x5 for nothing.
 */
public class SpiritgearHoeItem extends HoeItem {
    public SpiritgearHoeItem(Properties properties) {
        super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(HoeItem.createAttributes(tk.darrow.tribalpower.item.SpiritGear.TIER, -3.0F, 0.0F)));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return SpiritGear.wear(stack, super.damageItem(stack, amount, entity, onBroken));
    }

    /** How far the reap-and-sow reaches, as a radius in blocks. */
    private static int reach(ItemStack stack) {
        return switch (SpiritGear.voice(stack).orElse(null)) {
            case AIR -> 1;
            case LOOM -> 2;
            case null, default -> 0;
        };
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
        return SpiritGear.mine(stack, level, state, pos, entity, () -> super.mineBlock(stack, level, state, pos, entity)).ok();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null) return super.useOn(context);
        if (level.isClientSide && ripe(level.getBlockState(context.getClickedPos()))) return InteractionResult.SUCCESS;
        // Reaping comes first: a ripe crop is harvested and sown again rather than walked past.
        if (level instanceof ServerLevel server && ripe(level.getBlockState(context.getClickedPos()))) {
            int radius = reach(stack);
            int reaped = 0;
            boolean free = SpiritGear.voice(stack).orElse(null) == Attunement.LOOM;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                BlockPos at = context.getClickedPos().offset(x, 0, z);
                if (!ripe(level.getBlockState(at)) || !level.mayInteract(player, at)
                        || !player.mayUseItemAt(at, context.getClickedFace(), stack)) continue;
                // Each crop is a paid use; the reap itself never touches the edge, and with no Pulse it stops.
                if (!player.getAbilities().instabuild && !free
                        && !GearCell.spend(player, stack, SpiritGear.useCost(stack))) {
                    SpiritgearHelper.notifyStarved(player);
                    break;
                }
                reap(server, player, stack, at);
                reaped++;
            }
            if (reaped > 0) {
                SpiritgearHelper.notifyFueled(player);
                level.playSound(null, context.getClickedPos(), net.minecraft.sounds.SoundEvents.CROP_BREAK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
                return InteractionResult.SUCCESS;
            }
        }
        if (level.isClientSide) return super.useOn(context);

        // Tilling is a paid use: the cell's Pulse instead of the edge, or a starved furrow that wears.
        int[] wear = {0};
        InteractionResult result = SpiritGear.held(stack, wear, () -> super.useOn(context));
        boolean paid = SpiritGear.settleUse(player, stack, LivingEntity.getSlotForHand(context.getHand()), result.consumesAction(), wear[0]);
        if (!result.consumesAction() || !paid) return result;
        Attunement voice = SpiritGear.voice(stack).orElse(null);
        if (voice == Attunement.EARTH) {
            BlockPos centre = context.getClickedPos();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos at = centre.offset(x, 0, z);
                if (!level.mayInteract(player, at) || !player.mayUseItemAt(at, context.getClickedFace(), stack)) continue;
                // The rest of the 3x3 rides on the one use's Pulse and wears nothing.
                UseOnContext neighbour = new UseOnContext(player, context.getHand(), new BlockHitResult(
                        context.getClickLocation().add(x, 0, z), context.getClickedFace(), at, false));
                SpiritGear.held(stack, new int[1], () -> super.useOn(neighbour));
            }
        }
        if (voice == Attunement.WATER) moisten(level, context.getClickedPos(), 1);
        return result;
    }

    private static boolean ripe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3;
    }

    /** Take the harvest, keep one seed back in the ground, and leave the plant standing at age zero. */
    private static void reap(ServerLevel level, Player player, ItemStack tool, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack seed = state.getBlock().getCloneItemStack(level, pos, state);
        // the plant goes back only when one of its own seeds came out of the harvest to sow
        boolean kept = false;
        boolean bounty = SpiritGear.voice(tool).orElse(null) == Attunement.SPIRIT
                && SpiritGear.chance(player, level.random, tool, 0.25F);
        for (ItemStack drop : Block.getDrops(state, level, pos, null, player, tool)) {
            if (!kept && !seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                kept = true;
            }
            if (drop.isEmpty()) continue;
            if (bounty && !ItemStack.isSameItem(drop, seed)) drop.grow(drop.getCount());
            Block.popResource(level, pos, drop);
        }
        level.setBlockAndUpdate(pos, kept ? state.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState());
    }

    /** Wet every furrow in range — the Water voice never lets its fields dry out. */
    private static void moisten(Level level, BlockPos centre, int radius) {
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 1, radius))) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < 7) {
                level.setBlock(at.immutable(), state.setValue(FarmBlock.MOISTURE, 7), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !selected || !(entity instanceof Player player)) return;
        if (SpiritGear.voice(stack).orElse(null) == Attunement.WATER && level.getGameTime() % 40 == 0) {
            moisten(level, player.blockPosition(), SpiritGear.rank(stack) >= 3 ? 5 : 3);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        tooltip.add(Component.translatable("item.tribalpower.spiritgear_hoe.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
