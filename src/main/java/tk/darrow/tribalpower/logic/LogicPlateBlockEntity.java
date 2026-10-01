package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.config.TribalConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stateful song-plate logic. Binary gates read left and right of FACING; unary gates read the back.
 *
 * <p>A plate can also be synced with the Totem Wrench to hear other plates with no wire between them. Links are
 * one-way and live on the listener: each names a source plate and the ear it sings into, and the source never
 * knows it is heard. A link counts exactly like a wire into that ear, and the louder of the two wins.
 *
 * <p>A plate belongs to whoever set it down and their camp: only they may sync it, hold its song or change its
 * setting. Reading it stays open to everyone.
 */
public class LogicPlateBlockEntity extends BlockEntity implements Diagnosable, Ownership.Owned {
    /** Which input a synced source sings into. */
    public enum Ear {
        LEFT, RIGHT, BACK;

        public Component label() {
            return Component.translatable("message.tribalpower.plate_link.ear." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** One wireless input: the source plate's position and the ear it sings into. */
    public record Link(BlockPos source, Ear ear) {}

    private int memory;
    private int delay;
    private int period = 20;
    private int wait = 4;
    private int threshold = 4;
    private int step;
    private boolean lastLeft;
    private boolean lastBack;
    private String reason = "listening";
    private int reasonA;
    private int reasonB;
    private final List<Link> links = new ArrayList<>();
    /** Resolved source plates, parallel to {@link #links}. Never saved; filled lazily after a load or a chunk unload. */
    private final List<LogicPlateBlockEntity> sources = new ArrayList<>();
    private int heardLeft;
    private int heardRight;
    private int heardBack;
    /**
     * Output after this plate's latest tick, the output before it, and the game time of that tick. A listener hears
     * the value as of the end of the previous game tick whichever of the two ticks first, so synced plates do not
     * depend on tick order and a ring of them is one tick per hop, never a same-tick feedback loop.
     */
    private int sung;
    private int sungBefore;
    private long sungAt = Long.MIN_VALUE;
    private UUID owner;
    /** Verse's four steps, as signal strengths. A constant: every Verse plate reads it every tick. */
    private static final int[] VERSE_LEVELS = {4, 8, 12, 15};

    public LogicPlateBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.PLATE_TYPE.get(), pos, state);
    }

    @Override public UUID owner() { return owner; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }

    /** Whether crouch-use changes something on this kind of plate, rather than only reading it. */
    public boolean hasSetting() {
        return switch (kind()) {
            case HEARTBEAT, DRIFT, TALLY -> true;
            default -> false;
        };
    }

    public LogicKind kind() {
        if (getBlockState().getBlock() instanceof LogicPlateBlock plate) return plate.kind();
        return LogicKind.fromId(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath());
    }

    public Component status() {
        return switch (reason) {
            case "heartbeat" -> Component.translatable("message.tribalpower.logic.heartbeat", reasonA);
            case "drift" -> Component.translatable("message.tribalpower.logic.drift", reasonA, reasonB);
            case "tally" -> Component.translatable("message.tribalpower.logic.tally", reasonA, reasonB);
            case "verse" -> Component.translatable("message.tribalpower.logic.verse", reasonA);
            case "listening", "chorus_sings", "chorus_waits", "gathering_open", "gathering_quiet",
                    "discord_split", "discord_even", "hush_holds", "hush_breaks", "inverse_flipped",
                    "inverse_pressed", "echo_carries", "echo_silent", "memory_set", "memory_clear",
                    "strike", "strike_wait", "chance_yes", "chance_no"
                    -> Component.translatable("message.tribalpower.logic." + reason);
            default -> Component.literal(reason);
        };
    }

    /** Right-click status, with the synced count when there is one. */
    public Component statusWithLinks() {
        return links.isEmpty() ? status()
                : Component.translatable("message.tribalpower.plate_link.status", status(), links.size());
    }

    public Component cycle() {
        Component next = switch (kind()) {
            case HEARTBEAT -> {
                period = period >= 160 ? 20 : period * 2;
                yield Component.translatable("message.tribalpower.logic.cycle.heartbeat", period);
            }
            case DRIFT -> {
                wait = wait >= 32 ? 4 : wait * 2;
                yield Component.translatable("message.tribalpower.logic.cycle.drift", wait);
            }
            case TALLY -> {
                threshold = threshold >= 8 ? 1 : threshold + 1;
                yield Component.translatable("message.tribalpower.logic.cycle.tally", threshold);
            }
            default -> status();
        };
        setChanged();
        return next;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LogicPlateBlockEntity be) {
        if (level.isClientSide) return;
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        int left = input(level, pos, leftOf(facing));
        int right = input(level, pos, rightOf(facing));
        int back = input(level, pos, facing.getOpposite());
        if (!be.links.isEmpty()) {
            be.hearLinks(level, pos);
            left = Math.max(left, be.heardLeft);
            right = Math.max(right, be.heardRight);
            back = Math.max(back, be.heardBack);
        }
        int before = state.getValue(LogicPlateBlock.POWER);
        boolean leftOn = left > 0, rightOn = right > 0, backOn = back > 0;
        int out = 0;
        LogicKind kind = be.kind();
        // Memory, Tally, Chance and Verse hold state that must survive a reload even when the output does not change.
        int memoryBefore = be.memory, stepBefore = be.step;
        boolean backBefore = be.lastBack, leftBefore = be.lastLeft;
        switch (kind) {
            case CHORUS -> { out = leftOn && rightOn ? 15 : 0; be.setReason(out > 0 ? "chorus_sings" : "chorus_waits"); }
            case GATHERING -> { out = leftOn || rightOn ? 15 : 0; be.setReason(out > 0 ? "gathering_open" : "gathering_quiet"); }
            case DISCORD -> { out = leftOn ^ rightOn ? 15 : 0; be.setReason(out > 0 ? "discord_split" : "discord_even"); }
            case HUSH -> { out = !(leftOn && rightOn) ? 15 : 0; be.setReason(out > 0 ? "hush_holds" : "hush_breaks"); }
            case INVERSE -> { out = backOn ? 0 : 15; be.setReason(out > 0 ? "inverse_flipped" : "inverse_pressed"); }
            case ECHO -> { out = backOn ? 15 : 0; be.setReason(out > 0 ? "echo_carries" : "echo_silent"); }
            case MEMORY -> {
                if (leftOn) be.memory = 15;
                if (rightOn) be.memory = 0;
                out = be.memory;
                be.setReason(out > 0 ? "memory_set" : "memory_clear");
            }
            case HEARTBEAT -> {
                be.delay++;
                if (be.delay >= be.period) { be.delay = 0; out = 15; }
                be.setReason("heartbeat", be.period, 0);
            }
            case DRIFT -> {
                if (backOn) { if (be.delay < be.wait) be.delay++; }
                else be.delay = 0;
                out = be.delay >= be.wait ? 15 : 0;
                be.setReason("drift", be.delay, be.wait);
            }
            case TALLY -> {
                if (backOn && !be.lastBack) be.memory = Math.min(15, be.memory + 1);
                if (leftOn && !be.lastLeft) be.memory = 0;
                out = be.memory >= be.threshold ? 15 : 0;
                be.setReason("tally", be.memory, be.threshold);
            }
            case STRIKE -> {
                out = backOn && !be.lastBack ? 15 : 0;
                be.setReason(out > 0 ? "strike" : "strike_wait");
            }
            case CHANCE -> {
                if (backOn && !be.lastBack) be.memory = level.random.nextBoolean() ? 15 : 0;
                if (!backOn) be.memory = 0;
                out = be.memory;
                be.setReason(out > 0 ? "chance_yes" : "chance_no");
            }
            case VERSE -> {
                if (backOn && !be.lastBack) be.step = (be.step + 1) % 4;
                out = VERSE_LEVELS[be.step];
                be.setReason("verse", be.step + 1, 0);
            }
        }
        be.lastBack = backOn;
        be.lastLeft = leftOn;
        if (be.memory != memoryBefore || be.step != stepBefore || backOn != backBefore || leftOn != leftBefore)
            be.setChanged();
        long now = level.getGameTime();
        if (be.sungAt != now) be.sungBefore = before;   // a second tick in one game tick keeps the first one's "before"
        be.sung = out;
        be.sungAt = now;
        if (state.getValue(LogicPlateBlock.POWER) != out) {
            level.setBlock(pos, state.setValue(LogicPlateBlock.POWER, out), 3);
            level.updateNeighborsAt(pos.relative(facing), state.getBlock());
            be.setChanged();
        }
    }

    private void setReason(String key) { reason = key; reasonA = 0; reasonB = 0; }
    private void setReason(String key, int a, int b) { reason = key; reasonA = a; reasonB = b; }

    private static int input(Level level, BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        BlockState other = level.getBlockState(neighbor);
        if (other.getBlock() instanceof LogicPlateBlock && other.getValue(LogicPlateBlock.FACING) == side.getOpposite()) {
            return 0;
        }
        // Both reads ask the neighbour about the face turned toward this plate; the opposite face would hear a
        // repeater or comparator that points away from the plate.
        return Math.max(level.getSignal(neighbor, side), other.getDirectSignal(level, neighbor, side));
    }

    private static Direction leftOf(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            case UP, DOWN -> Direction.WEST;
        };
    }

    private static Direction rightOf(Direction facing) {
        return leftOf(facing).getOpposite();
    }

    /** The world direction an ear faces for a plate pointing {@code facing}: where a wire into that ear would come from. */
    public static Direction earSide(Direction facing, Ear ear) {
        return switch (ear) {
            case LEFT -> leftOf(facing);
            case RIGHT -> rightOf(facing);
            case BACK -> facing.getOpposite();
        };
    }

    // ---- wireless links --------------------------------------------------------------------

    /** What this plate sang as of the end of the game tick before {@code now}: the value a synced listener hears. */
    public int heardOver(long now) {
        if (sungAt == Long.MIN_VALUE) return getBlockState().getValue(LogicPlateBlock.POWER);
        return sungAt == now ? sungBefore : sung;
    }

    /**
     * Gathers every synced source into the three ears. An unloaded source reads 0 and is never loaded for it; a
     * source beyond the configured range reads 0 but is kept, since the range may be raised again; a source whose
     * chunk is loaded but which is no longer a plate is forgotten.
     */
    private void hearLinks(Level level, BlockPos pos) {
        heardLeft = heardRight = heardBack = 0;
        long now = level.getGameTime();
        long range = TribalConfig.plateLinkRange();
        long rangeSq = range * range;
        boolean dropped = false;
        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            BlockPos at = link.source();
            if (at.distSqr(pos) > rangeSq) continue;
            if (!level.isLoaded(at)) { sources.set(i, null); continue; }
            LogicPlateBlockEntity source = sources.get(i);
            if (source == null || source.isRemoved() || source.getLevel() != level) {
                source = level.getBlockEntity(at) instanceof LogicPlateBlockEntity plate && plate != this ? plate : null;
                if (source == null) {
                    links.remove(i);
                    sources.remove(i);
                    i--;
                    dropped = true;
                    continue;
                }
                sources.set(i, source);
            }
            int value = source.heardOver(now);
            switch (link.ear()) {
                case LEFT -> heardLeft = Math.max(heardLeft, value);
                case RIGHT -> heardRight = Math.max(heardRight, value);
                case BACK -> heardBack = Math.max(heardBack, value);
            }
        }
        if (dropped) setChanged();
    }

    public List<Link> links() { return Collections.unmodifiableList(links); }

    public Link linkFrom(BlockPos source) {
        for (Link link : links) if (link.source().equals(source)) return link;
        return null;
    }

    /** Adds a one-way link from {@code source} into {@code ear}. False for this plate itself, a source already heard, or a full plate. */
    public boolean link(BlockPos source, Ear ear) {
        if (source.equals(worldPosition) || linkFrom(source) != null || links.size() >= TribalConfig.plateLinkMax()) return false;
        links.add(new Link(source.immutable(), ear));
        sources.add(null);
        setChanged();
        return true;
    }

    public boolean unlink(BlockPos source) {
        for (int i = 0; i < links.size(); i++) {
            if (!links.get(i).source().equals(source)) continue;
            links.remove(i);
            sources.remove(i);
            setChanged();
            return true;
        }
        return false;
    }

    /** Whether this kind of plate listens with {@code ear} at all; a Heartbeat hears nothing. */
    public static boolean hearsWith(LogicKind kind, Ear ear) {
        return switch (kind) {
            case CHORUS, GATHERING, DISCORD, HUSH, MEMORY -> ear != Ear.BACK;
            case TALLY -> ear != Ear.RIGHT;
            case HEARTBEAT -> false;
            default -> ear == Ear.BACK;
        };
    }

    /** One line per synced source, as the wrench and the Codex both show them. Never loads a chunk. */
    public List<Component> linkLines(ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        long now = level.getGameTime();
        int range = TribalConfig.plateLinkRange();
        for (Link link : links) {
            BlockPos at = link.source();
            int distance = (int) Math.round(Math.sqrt(at.distSqr(worldPosition)));
            boolean loaded = level.isLoaded(at);
            Component state;
            if (distance > range) state = Component.translatable("message.tribalpower.plate_link.state.far", range);
            else if (!loaded) state = Component.translatable("message.tribalpower.plate_link.state.unloaded");
            else if (level.getBlockEntity(at) instanceof LogicPlateBlockEntity source)
                state = Component.translatable("message.tribalpower.plate_link.state.singing", source.heardOver(now));
            else state = Component.translatable("message.tribalpower.plate_link.state.gone");
            Component name = loaded ? level.getBlockState(at).getBlock().getName()
                    : Component.translatable("message.tribalpower.plate_link.unknown");
            lines.add(Component.translatable("message.tribalpower.plate_link.entry", name,
                    at.getX(), at.getY(), at.getZ(), link.ear().label(), distance, state));
        }
        return lines;
    }

    @Override
    public List<Component> diagnose(ServerLevel level, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.plate.status", status(), getBlockState().getValue(LogicPlateBlock.POWER)));
        if (links.isEmpty()) lines.add(Component.translatable("diag.tribalpower.plate.no_links"));
        else {
            lines.add(Component.translatable("diag.tribalpower.plate.links", links.size(), TribalConfig.plateLinkMax()));
            for (Component line : linkLines(level)) lines.add(Component.literal("  ").append(line));
        }
        return lines;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Memory", memory);
        tag.putInt("Delay", delay);
        tag.putInt("Period", period);
        tag.putInt("Wait", wait);
        tag.putInt("Threshold", threshold);
        tag.putInt("Step", step);
        tag.putBoolean("LastLeft", lastLeft);
        tag.putBoolean("LastBack", lastBack);
        tag.putString("Reason", reason);
        tag.putInt("ReasonA", reasonA);
        tag.putInt("ReasonB", reasonB);
        Ownership.save(tag, owner);
        if (!links.isEmpty()) {
            ListTag list = new ListTag();
            for (Link link : links) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", link.source().asLong());
                entry.putString("Ear", link.ear().name());
                list.add(entry);
            }
            tag.put("Links", list);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        memory = tag.getInt("Memory");
        delay = tag.getInt("Delay");
        period = Math.max(20, tag.getInt("Period"));
        wait = Math.max(4, tag.getInt("Wait"));
        threshold = Math.max(1, tag.getInt("Threshold"));
        step = Math.floorMod(tag.getInt("Step"), 4);   // indexes a four-entry table on the next tick
        lastLeft = tag.getBoolean("LastLeft");
        lastBack = tag.getBoolean("LastBack");
        reason = tag.getString("Reason");
        if (reason.isEmpty()) reason = "listening";
        reasonA = tag.getInt("ReasonA");
        reasonB = tag.getInt("ReasonB");
        owner = Ownership.load(tag);
        links.clear();
        sources.clear();
        for (Tag raw : tag.getList("Links", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            Ear ear;
            try { ear = Ear.valueOf(entry.getString("Ear")); }
            catch (IllegalArgumentException unknown) { continue; }
            BlockPos at = BlockPos.of(entry.getLong("Pos"));
            if (at.equals(worldPosition) || linkFrom(at) != null) continue;
            // A lowered cap never forgets links already made; it only refuses new ones.
            links.add(new Link(at, ear));
            sources.add(null);
        }
    }
}
