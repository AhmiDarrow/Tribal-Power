package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Weaver's Wand — lays a whole face of matching blocks at once. Click a block and the wand carries its
 * face outward across every neighbour like it, spending the blocks from your pack and Pulse from your cells.
 */
public class WeaversWandItem extends Item {
    /** Pulse per block laid, and the most blocks one sweep will lay. */
    public static final int PULSE_PER_BLOCK = 2;
    public static final int MAX_BLOCKS = 32;

    public WeaversWandItem(Properties properties) {
        super(properties.stacksTo(1).durability(768));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos origin = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack wand = context.getItemInHand();
        BlockState clicked = level.getBlockState(origin);
        BlockState pattern = layable(clicked);
        if (pattern == null) {
            if (!clicked.isAir()) player.displayClientMessage(Component.translatable("message.tribalpower.wand.cannot_copy", clicked.getBlock().getName()), true);
            return InteractionResult.FAIL;
        }
        List<BlockPos> targets = spread(level, player, origin, face, clicked, pattern, wand);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.wand.nowhere"), true);
            return InteractionResult.FAIL;
        }
        net.minecraft.world.item.Item block = pattern.getBlock().asItem();
        int available = player.getAbilities().instabuild ? targets.size() : count(player, block);
        if (available <= 0) {
            player.displayClientMessage(Component.translatable("message.tribalpower.wand.no_blocks",
                    pattern.getBlock().getName()), true);
            return InteractionResult.FAIL;
        }
        int laid = 0;
        for (BlockPos target : targets) {
            if (laid >= available) break;
            BlockState before = level.getBlockState(target);
            if (!level.setBlock(target, pattern, Block.UPDATE_ALL)) continue;
            // Pulse is paid for blocks that actually land; a sweep that runs dry puts the last one back.
            if (!player.getAbilities().instabuild && !SpiritgearHelper.tryConsumePulse(player, PULSE_PER_BLOCK)) {
                level.setBlock(target, before, Block.UPDATE_ALL);
                SpiritgearHelper.notifyStarved(player);
                break;
            }
            if (!player.getAbilities().instabuild) take(player, block);
            laid++;
        }
        if (laid == 0) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) wand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        level.playSound(null, origin, pattern.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    /**
     * What the wand lays for a clicked block: the block itself, fresh, turned the same way -- or null when copying
     * it would make something from nothing. A double slab, a stack of candles or pickles, layered snow, one half of
     * a door or bed, or a block with an inventory all hold more than the single item the wand takes for it.
     */
    public static @org.jetbrains.annotations.Nullable BlockState layable(BlockState clicked) {
        Block block = clicked.getBlock();
        if (clicked.isAir() || block.asItem() == net.minecraft.world.item.Items.AIR || clicked.hasBlockEntity()) return null;
        if (clicked.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)
                && clicked.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) return null;
        for (var multiple : java.util.List.<net.minecraft.world.level.block.state.properties.Property<?>>of(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BlockStateProperties.CANDLES, net.minecraft.world.level.block.state.properties.BlockStateProperties.PICKLES, net.minecraft.world.level.block.state.properties.BlockStateProperties.EGGS, net.minecraft.world.level.block.state.properties.BlockStateProperties.FLOWER_AMOUNT, net.minecraft.world.level.block.state.properties.BlockStateProperties.LAYERS))
            if (clicked.hasProperty(multiple)) return null;
        BlockState fresh = block.defaultBlockState();
        for (var turn : java.util.List.<net.minecraft.world.level.block.state.properties.Property<?>>of(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.BlockStateProperties.STAIRS_SHAPE, net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16))
            fresh = copy(clicked, fresh, turn);
        if (fresh.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) fresh = fresh.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
        return fresh;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return from.hasProperty(property) && to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    /** Every open spot on the clicked face, reached across neighbours of the same block. */
    private static List<BlockPos> spread(Level level, Player player, BlockPos origin, Direction face,
                                         BlockState clicked, BlockState pattern, ItemStack wand) {
        List<BlockPos> targets = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && targets.size() < MAX_BLOCKS) {
            BlockPos at = queue.poll();
            if (!level.getBlockState(at).is(clicked.getBlock())) continue;
            BlockPos target = at.relative(face);
            if (open(level, player, target, pattern, wand)) targets.add(target);
            for (Direction step : Direction.values()) {
                if (step.getAxis() == face.getAxis()) continue;
                BlockPos next = at.relative(step);
                if (next.distManhattan(origin) > MAX_BLOCKS || !seen.add(next)) continue;
                queue.add(next);
            }
        }
        return targets;
    }

    private static boolean open(Level level, Player player, BlockPos pos, BlockState pattern, ItemStack wand) {
        if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, wand)) return false;
        return level.isUnobstructed(pattern, pos, CollisionContext.empty());
    }

    /** Blocks come from the pack and hotbar only, never the armour or the offhand. */
    private static int count(Player player, net.minecraft.world.item.Item block) {
        int total = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(block)) total += stack.getCount();
        }
        return total;
    }

    private static void take(Player player, net.minecraft.world.item.Item block) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.isEmpty() || !stack.is(block)) continue;
            stack.shrink(1);
            if (stack.isEmpty()) player.getInventory().removeItem(stack);
            player.getInventory().setChanged();
            return;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.weavers_wand.desc", MAX_BLOCKS, PULSE_PER_BLOCK));
    }
}
