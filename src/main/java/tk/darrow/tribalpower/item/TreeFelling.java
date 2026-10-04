package tk.darrow.tribalpower.item;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import tk.darrow.tribalpower.TribalPower;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * An Earth axe fells the whole tree a struck log belongs to. A tree is every log of the struck kind joined to it
 * (diagonals too) with natural leaves on it in proportion, and nothing a builder lays touching it: a leafless
 * pillar, a build whose leaves were placed, or a cabin grown into a tree still stands. Logs come down from the top,
 * each broken as the player would break it, and each costs a point of the edge, so one felling is only as big as
 * the edge left on the axe.
 */
public final class TreeFelling {
    /** Most logs a tree may have before it is not treated as one. A young willow runs to ~220 logs. */
    public static final int CAP = 512;
    /** A Weeping Colossus is enormous: about 11,000 logs at the smallest grove size and 25,000 at the largest. */
    public static final int WILLOW_CAP = 32768;
    static final TagKey<Block> WILLOW_LOGS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "willow_logs"));
    /** Blocks a builder lays: wood touching one is a build, or a tree grown into one, and is never felled. */
    static final TagKey<Block> BUILDING = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "felling_stoppers"));
    /** Natural leaves that must touch the wood before it counts as a tree, and at least one per {@link #LOGS_PER_LEAF} logs. */
    static final int MIN_LEAVES = 3;
    static final int LOGS_PER_LEAF = 8;
    /** Logs broken in one tick; a bigger tree keeps falling over the next ticks. */
    public static final int PER_TICK = 48;
    /** Logs one swing's worth of Pulse fells. */
    public static final int LOGS_PER_CHARGE = 16;

    /** A tree coming down, and how many more logs the last swing's worth of Pulse still covers. */
    private static final class Felling {
        final Level level;
        final ItemStack tool;
        final Block wood;
        final ArrayDeque<BlockPos> logs;
        final int charge;
        int paid;

        Felling(Level level, ItemStack tool, Block wood, List<BlockPos> logs, int charge) {
            this.level = level;
            this.tool = tool;
            this.wood = wood;
            this.logs = new ArrayDeque<>(logs);
            this.charge = charge;
        }
    }

    /** Trees still coming down, one per player. Dropped on logout (and with the server); weak for anything missed. */
    private static final Map<ServerPlayer, Felling> FALLING = new WeakHashMap<>();

    private TreeFelling() {}

    /** Called from the axe's own swing on {@code hit}, before the game removes that log. */
    static void fell(ServerPlayer player, ItemStack tool, BlockPos hit, BlockState state) {
        // Crouching takes one log; and one tree at a time, so a second swing never pays twice for the same wood.
        if (player.isShiftKeyDown() || FALLING.containsKey(player)) return;
        List<BlockPos> logs = tree(player.level(), hit, state.getBlock(), state.is(WILLOW_LOGS) ? WILLOW_CAP : CAP, room(player.level(), tool));
        if (logs.isEmpty()) return;
        Felling felling = new Felling(player.level(), tool, state.getBlock(), logs, SpiritGear.mineCost(tool));
        // Block breakers and deployers never tick, so their tree comes down at once.
        if (player instanceof FakePlayer) {
            chop(player, felling, Integer.MAX_VALUE);
            finish(player, felling);
        } else if (chop(player, felling, PER_TICK)) {
            finish(player, felling);
        } else {
            FALLING.put(player, felling);
        }
    }

    /** Keeps a big tree falling, a batch each player tick. Public for GameTests, whose mock players never tick. */
    public static void tick(ServerPlayer player) {
        Felling felling = FALLING.get(player);
        if (felling != null && chop(player, felling, PER_TICK)) forget(player);
    }

    /** Stops a player's felling where it stands, handing back a swing's Pulse that has felled nothing yet. */
    public static void forget(ServerPlayer player) {
        Felling felling = FALLING.remove(player);
        if (felling != null) finish(player, felling);
    }

    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    public static void clear() {
        FALLING.clear();
    }

    /** Logs the edge can fell before its last point; Unbreaking stretches it about as far as it spares wear. */
    private static int room(Level level, ItemStack tool) {
        if (!tool.isDamageableItem()) return Integer.MAX_VALUE;
        int unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.UNBREAKING)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, tool)).orElse(0);
        return Math.max(0, tool.getMaxDamage() - 1 - tool.getDamageValue()) * (unbreaking + 1);
    }

    /** Fells up to {@code batch} logs; true once the felling is over, every log down or cut short. */
    private static boolean chop(ServerPlayer player, Felling felling, int batch) {
        ItemStack tool = felling.tool;
        SpiritGear.Swing prior = SpiritGear.SWING.get();
        try {
            for (int done = 0; done < batch && !felling.logs.isEmpty(); done++) {
                // The same axe in hand, in the same world, and never down to its last point.
                if (player.getMainHandItem() != tool || player.level() != felling.level || player.isRemoved() || !player.isAlive()
                        || (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1)) return true;
                BlockPos pos = felling.logs.poll();
                if (!player.level().isLoaded(pos) || !player.level().getBlockState(pos).is(felling.wood)
                        || !player.level().mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, tool)) continue;
                // A swing's worth of Pulse for each run of logs, paid as the run starts rather than for the whole tree.
                if (felling.paid == 0) {
                    if (!GearCell.spend(player, tool, felling.charge)) {
                        SpiritgearHelper.notifyStarved(player);
                        return true;
                    }
                    felling.paid = LOGS_PER_CHARGE;
                }
                // An area swing: rides on the payment above, and destroyBlock fires the break event protection hears.
                SpiritGear.beginSwing(player, tool, true, true);
                int wear = tool.getDamageValue();
                if (player.gameMode.destroyBlock(pos)) {
                    felling.paid--;
                    // Set aside the swing's own wear and mend (see mineBlock): a felled log costs one point, less Unbreaking.
                    tool.setDamageValue(wear);
                    tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
            }
            return felling.logs.isEmpty();
        } finally {
            if (prior == null) SpiritGear.endSwing();
            else SpiritGear.SWING.set(prior);
        }
    }

    /** A swing's Pulse paid for a run that then felled nothing (protected, or cut short) goes back. */
    private static void finish(ServerPlayer player, Felling felling) {
        if (felling.paid == LOGS_PER_CHARGE) GearCell.refund(player, felling.tool, felling.charge);
        felling.paid = 0;
    }

    static List<BlockPos> tree(Level level, BlockPos hit, Block wood, int cap, int room) {
        return tree(level, level::isLoaded, hit, wood, cap, room);
    }

    /**
     * The other logs of the tree {@code hit} belongs to, the top {@code room} of them, or none when it is not a
     * tree: too big, running into unloaded ground, touching a builder's blocks, or without natural leaves in
     * proportion. The struck log itself is left out; the game breaks that one.
     */
    public static List<BlockPos> tree(BlockGetter world, Predicate<BlockPos> loaded, BlockPos hit, Block wood, int cap, int room) {
        Comparator<BlockPos> topDown = Comparator.<BlockPos>comparingInt(pos -> -pos.getY()).thenComparingInt(pos -> pos.distManhattan(hit));
        // An ordinary tree is always seen whole. One bigger than the edge can fell is looked at only to twice that, so
        // its top `room` logs are the crown and not the path the flood climbed up the trunk to reach it.
        long budget = Math.max(CAP, 2L * room);
        LongOpenHashSet seen = new LongOpenHashSet();
        LongOpenHashSet leaves = new LongOpenHashSet();
        // Highest first: the flood climbs to the crown before it spreads, so a scan cut short holds the top of the tree.
        PriorityQueue<BlockPos> open = new PriorityQueue<>(topDown);
        List<BlockPos> logs = new ArrayList<>();
        seen.add(hit.asLong());
        open.add(hit);
        while (!open.isEmpty() && logs.size() <= budget) {
            BlockPos pos = open.poll();
            logs.add(pos);
            if (logs.size() > cap) return List.of();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos next = pos.offset(dx, dy, dz);
                // Above the sky or below bedrock there is nothing; a tree grown to the build limit still fells.
                if (world.isOutsideBuildHeight(next)) continue;
                // Never load a chunk to look; a tree that runs off into one is left standing.
                if (!loaded.test(next)) return List.of();
                BlockState there = world.getBlockState(next);
                if (there.is(wood)) {
                    if (seen.add(next.asLong())) open.add(next);
                } else if (natural(there)) {
                    leaves.add(next.asLong());
                } else if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1 && there.is(BUILDING)) {
                    return List.of();
                }
            }
        }
        if (leaves.size() < Math.max(MIN_LEAVES, logs.size() / LOGS_PER_LEAF)) return List.of();
        logs.removeFirst();
        logs.sort(topDown);
        return logs.size() > room ? new ArrayList<>(logs.subList(0, room)) : logs;
    }

    /** Leaves that grew there; placed leaves are persistent. */
    private static boolean natural(BlockState state) {
        return state.is(BlockTags.LEAVES)
                && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT));
    }
}
