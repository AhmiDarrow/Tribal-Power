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
        BlockState pattern = level.getBlockState(origin);
        if (pattern.isAir() || pattern.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return InteractionResult.FAIL;
        }
        List<BlockPos> targets = spread(level, player, origin, face, pattern, wand);
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
            if (!player.getAbilities().instabuild
                    && !SpiritgearHelper.tryConsumePulse(player, PULSE_PER_BLOCK)) {
                SpiritgearHelper.notifyStarved(player);
                break;
            }
            if (!level.setBlock(target, pattern, Block.UPDATE_ALL)) continue;
            if (!player.getAbilities().instabuild) take(player, block);
            laid++;
        }
        if (laid == 0) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) wand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        level.playSound(null, origin, pattern.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    /** Every open spot on the clicked face, reached across neighbours of the same block. */
    private static List<BlockPos> spread(Level level, Player player, BlockPos origin, Direction face,
                                         BlockState pattern, ItemStack wand) {
        List<BlockPos> targets = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && targets.size() < MAX_BLOCKS) {
            BlockPos at = queue.poll();
            if (!level.getBlockState(at).is(pattern.getBlock())) continue;
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

    private static int count(Player player, net.minecraft.world.item.Item block) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(block)) total += stack.getCount();
        }
        return total;
    }

    private static void take(Player player, net.minecraft.world.item.Item block) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(block)) continue;
            stack.shrink(1);
            player.getInventory().setChanged();
            return;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.weavers_wand.desc", MAX_BLOCKS, PULSE_PER_BLOCK));
    }
}
