package tk.darrow.tribalpower.item;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.TribalPower;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * An Earth axe fells the whole tree a struck log belongs to. A tree is every log of the struck kind joined to it
 * (diagonals too) with natural leaves on it, so a leafless pillar, or a build whose leaves were placed, still stands.
 * Logs come down from the top, each broken as the player would break it, and each costs a point of the edge.
 */
public final class TreeFelling {
    /** Most logs a tree may have before it is not treated as one. A young willow runs to ~220 logs. */
    public static final int CAP = 512;
    /** A Weeping Colossus is enormous: about 11,000 logs at the smallest grove size and 25,000 at the largest. */
    public static final int WILLOW_CAP = 32768;
    static final TagKey<Block> WILLOW_LOGS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "willow_logs"));
    /** Natural leaves that must touch the wood before it counts as a tree. */
    static final int MIN_LEAVES = 3;
    /** Logs broken in one tick; a bigger tree keeps falling over the next ticks. */
    static final int PER_TICK = 48;
    /** Logs one swing's worth of Pulse fells. */
    public static final int LOGS_PER_CHARGE = 16;

    private record Felling(Level level, ItemStack tool, Block wood, ArrayDeque<BlockPos> logs) {}

    /** Trees still coming down, one per player. Weak so a player who leaves takes theirs along. */
    private static final Map<ServerPlayer, Felling> FALLING = new WeakHashMap<>();

    private TreeFelling() {}

    /** Called from the axe's own swing on {@code hit}, before the game removes that log. */
    static void fell(ServerPlayer player, ItemStack tool, BlockPos hit, BlockState state) {
        // Crouching takes one log; and one tree at a time, so a second swing never pays twice for the same wood.
        if (player.isShiftKeyDown() || FALLING.containsKey(player)) return;
        List<BlockPos> logs = tree(player.level(), hit, state.getBlock(), state.is(WILLOW_LOGS) ? WILLOW_CAP : CAP);
        if (logs.isEmpty()) return;
        // Never fell more than the edge can take: the last point of durability stays on the axe.
        int room = tool.isDamageableItem() ? tool.getMaxDamage() - 1 - tool.getDamageValue() : logs.size();
        int count = Math.min(logs.size(), room);
        if (count <= 0) return;
        int cost = SpiritGear.mineCost(tool) * Mth.positiveCeilDiv(count, LOGS_PER_CHARGE);
        if (!GearCell.spend(player, tool, cost)) {
            SpiritgearHelper.notifyStarved(player);
            return;
        }
        logs.sort(Comparator.<BlockPos>comparingInt(pos -> -pos.getY()).thenComparingInt(pos -> pos.distManhattan(hit)));
        Felling felling = new Felling(player.level(), tool, state.getBlock(), new ArrayDeque<>(logs.subList(0, count)));
        chop(player, felling);
        if (!felling.logs().isEmpty()) FALLING.put(player, felling);
    }

    /** Keeps a big tree falling, a batch each player tick. */
    static void tick(ServerPlayer player) {
        Felling felling = FALLING.get(player);
        if (felling == null) return;
        chop(player, felling);
        if (felling.logs().isEmpty()) FALLING.remove(player);
    }

    private static void chop(ServerPlayer player, Felling felling) {
        ItemStack tool = felling.tool();
        SpiritGear.Swing prior = SpiritGear.SWING.get();
        try {
            for (int done = 0; done < PER_TICK && !felling.logs().isEmpty(); done++) {
                // The same axe in hand, in the same world, and never down to its last point.
                if (player.getMainHandItem() != tool || player.level() != felling.level() || player.isRemoved()
                        || (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1)) {
                    felling.logs().clear();
                    return;
                }
                BlockPos pos = felling.logs().poll();
                if (!player.level().isLoaded(pos) || !player.level().getBlockState(pos).is(felling.wood())
                        || !player.level().mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, tool)) continue;
                // An area swing: rides on the payment above, and destroyBlock fires the break event protection hears.
                SpiritGear.beginSwing(player, tool, true, true);
                if (player.gameMode.destroyBlock(pos)) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        } finally {
            if (prior == null) SpiritGear.endSwing();
            else SpiritGear.SWING.set(prior);
        }
    }

    /**
     * The other logs of the tree {@code hit} belongs to, or none when it is not a tree: too big, running into
     * unloaded ground, or without natural leaves. The struck log itself is left out; the game breaks that one.
     */
    static List<BlockPos> tree(Level level, BlockPos hit, Block wood, int cap) {
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        List<BlockPos> logs = new ArrayList<>();
        seen.add(hit.asLong());
        open.add(hit);
        while (!open.isEmpty()) {
            BlockPos pos = open.poll();
            logs.add(pos);
            if (logs.size() > cap) return List.of();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos next = pos.offset(dx, dy, dz);
                if (!seen.add(next.asLong())) continue;
                // Never load a chunk to look; a tree that runs off into one is left standing.
                if (!level.isLoaded(next)) return List.of();
                if (level.getBlockState(next).is(wood)) open.add(next);
            }
        }
        if (!leafy(level, logs)) return List.of();
        logs.removeFirst();
        return logs;
    }

    private static boolean leafy(Level level, List<BlockPos> logs) {
        LongOpenHashSet leaves = new LongOpenHashSet();
        for (BlockPos pos : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (natural(level.getBlockState(next)) && leaves.add(next.asLong()) && leaves.size() >= MIN_LEAVES) return true;
            }
        }
        return false;
    }

    /** Leaves that grew there; placed leaves are persistent. */
    private static boolean natural(BlockState state) {
        return state.is(BlockTags.LEAVES)
                && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT));
    }
}
