package tk.darrow.tribalpower.leyheart;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseGenerator;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.HasSideIo;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.SideIo;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.ley.LeyMagnets;
import tk.darrow.tribalpower.ley.LeyMath;
import tk.darrow.tribalpower.pattern.BlockPredicate;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;
import tk.darrow.tribalpower.pattern.PatternState;
import tk.darrow.tribalpower.pattern.RitualPattern;
import tk.darrow.tribalpower.rite.world.LeyLines;
import tk.darrow.tribalpower.song.ReagentThread;
import tk.darrow.tribalpower.song.Reagents;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Ley Heart: a Pulse Resonator and a Ley Collector made one, standing in a ring of all six voices.
 *
 * <p>It burns three things at once, each from its own intake. The crystals a Resonator seats (Echo Shard up
 * to Resonant Core) and any creature reagent go in item slots and burn slowly, minutes an item; water goes in
 * a tank and burns fastest of the three, {@value #WATER_PER_TICK} mB a tick. Every fuel that is burning adds
 * its share, and the more kinds burn together the more the whole song is worth.
 *
 * <p>Quality is the lever. A better crystal and a reagent of a stronger {@link ReagentThread} are each worth more
 * quarters of the song, and each burns faster for it: a complete heart on an Echo Shard, a plain reagent and
 * water makes about 1,100 Pulse a second, and a Manifested heart on a Resonant Core, a Thread III reagent and
 * water on a perfect site makes {@code 4,489}.
 *
 * <p>Its buffer holds {@value #CAPACITY}, about 45 seconds of its best, and the lattice draws on it like any
 * other generator.
 *
 * <p>Once the {@link ModPatterns#LEY_HEART} pattern is whole the heart raises six ley threads, one per voice,
 * from itself out through each totem and {@value LeyLines#HEART_REACH} blocks on into the world. They are real
 * threads: the Ley Lens draws them and Ley Collectors along them feel them. They are kept in the rite store,
 * so they outlast a save and an unloaded chunk, and they come down the moment the heart or a totem breaks.
 *
 * <p>What a beat makes is {@link #outputFor}: six times the Resonator's song over the answered voices and every
 * burning fuel's quarters, plus a ley share, all times the harmony of how many kinds are burning.
 */
public class LeyHeartBlockEntity extends BlockEntity implements PulseGenerator, Diagnosable, WorldlyContainer, MenuProvider, HasSideIo {
    /** About 45 seconds of a Manifested heart at its best, so a beat never waits on a draw. */
    public static final int CAPACITY = 200_000;
    public static final int TANK_CAPACITY = 8000;
    /** One beat a second, like the voice generators. */
    public static final int BEAT = 20;
    /** Water burns fastest: 200 mB a beat, a bucket every five seconds. */
    public static final int WATER_PER_TICK = 10;
    public static final int WATER_PER_BEAT = WATER_PER_TICK * BEAT;
    /** A plain reagent burns two minutes, Thread I a minute and a half, II one minute, III forty seconds. */
    private static final int[] REAGENT_TICKS = {2400, 1800, 1200, 800};
    /** What a reagent is worth in the song, in quarters: plain 4, Thread I 6, II 9, III 12 (a Resonant Core's). */
    private static final int[] REAGENT_QUARTERS = {4, 6, 9, 12};
    /** A seated crystal burns 15, 12, 10 or 8 minutes, Echo Shard to Resonant Core: the louder, the faster. */
    private static final int[] CRYSTAL_TICKS = {0, 18000, 14400, 12000, 9600};
    /** Water joins the crystal's and the reagent's quarters in the song. The crystal's are the Resonator's own. */
    public static final int WATER_QUARTERS = 2;
    /** The heart sings six times a Resonator's song for the same voices and quarters. */
    public static final int SONG_SCALE = 6;
    /** Pulse a second per point of ley strength at six answered voices: eight times the Ley Collector's 1.5. */
    public static final int LEY_YIELD = 12;
    /** {@link #outputFor}'s reagent Thread when no reagent is burning. 0 is a plain reagent. */
    public static final int NO_REAGENT = -1;
    /** Each kind of fuel burning beyond the first adds a quarter to the whole beat. */
    public static final double HARMONY = 0.25;
    /** The land survey is a few thousand block reads: once every other beat is plenty. */
    public static final int SURVEY_BEATS = 2;

    public static final int CRYSTAL = 0;
    public static final int REAGENT = 1;
    public static final int SIZE = 2;
    private static final int[] INPUTS = {CRYSTAL, REAGENT};
    private static final int[] NONE = new int[0];

    /**
     * Menu readout slots. ContainerData travels as shorts, so burn times go as seconds and the output and the
     * store go as two 15-bit halves, {@code _HI} the upper ({@link LeyHeartMenu#wide}).
     * {@code DATA_REAGENT_THREAD} is the burning (or waiting) reagent's Thread plus one, 0 for none.
     */
    public static final int DATA_OUTPUT = 0, DATA_WATER = 1, DATA_LEY = 2, DATA_ANSWERED = 3, DATA_STATE = 4,
            DATA_CRYSTAL = 5, DATA_CRYSTAL_TOTAL = 6, DATA_REAGENT = 7, DATA_REAGENT_TOTAL = 8, DATA_STORED = 9,
            DATA_THREADS = 10, DATA_OUTPUT_HI = 11, DATA_STORED_HI = 12, DATA_CRYSTAL_RANK = 13, DATA_REAGENT_THREAD = 14,
            DATA_RANK = 15, DATA_COUNT = 16;
    public static final int STATE_INCOMPLETE = 0, STATE_SINGING = 1, STATE_STILLED = 2, STATE_FULL = 3, STATE_IDLE = 4;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final SideIo sides = new SideIo(SideIo.Mode.INPUT);
    private final PatternState star = new PatternState(ModPatterns.LEY_HEART);

    public final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return stilled() ? 0 : super.fill(resource, action);
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };

    /** Ticks left on the crystal now burning, and its rank. */
    private int crystalBurn;
    private int crystalRank;
    /** Ticks left on the reagent now burning, and its Thread. */
    private int reagentBurn;
    private int reagentThread;

    // Live readings, worked out each beat and not saved.
    private boolean complete;
    private int answered;
    private int leyGain;
    private double surge = 1.0;
    private int output;
    /** The crystal rank and reagent Thread the last beat sang with ({@link #NO_REAGENT} for none). */
    private int singingRank;
    private int singingThread = NO_REAGENT;
    private int state = STATE_INCOMPLETE;
    private int beats;
    private List<LeyMagnets.Magnet> lastMagnets;
    private LeyField.Reading lastReading;
    private int lastHearts = -1;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_OUTPUT -> output & 0x7FFF;
                case DATA_OUTPUT_HI -> output >>> 15;
                case DATA_WATER -> tank.getFluidAmount();
                case DATA_LEY -> leyGain;
                case DATA_ANSWERED -> answered;
                case DATA_STATE -> state;
                case DATA_CRYSTAL -> crystalBurn / 20;
                case DATA_CRYSTAL_TOTAL -> crystalTicks(crystalRank) / 20;
                case DATA_REAGENT -> reagentBurn / 20;
                case DATA_REAGENT_TOTAL -> reagentTicks(reagentThread) / 20;
                case DATA_STORED -> pulse.getPulseStored() & 0x7FFF;
                case DATA_STORED_HI -> pulse.getPulseStored() >>> 15;
                case DATA_THREADS -> level == null ? 0 : LeyLines.raisedBy(level, worldPosition).size();
                case DATA_CRYSTAL_RANK -> singingRank;
                case DATA_REAGENT_THREAD -> singingThread + 1;
                case DATA_RANK -> tk.darrow.tribalpower.item.MachineRank.rank(LeyHeartBlockEntity.this);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    public LeyHeartBlockEntity(BlockPos pos, BlockState state) {
        super(LeyHeartRegistry.LEY_HEART_TYPE.get(), pos, state);
    }

    // ---- the song --------------------------------------------------------------------------------

    /** Burn time of one crystal of this rank, in ticks. */
    public static int crystalTicks(int rank) {
        return rank <= 0 || rank >= CRYSTAL_TICKS.length ? 0 : CRYSTAL_TICKS[rank];
    }

    /** Burn time of one reagent of this Thread (0 plain to 3), in ticks: the stronger, the faster. */
    public static int reagentTicks(int thread) {
        return REAGENT_TICKS[Math.clamp(thread, 0, REAGENT_TICKS.length - 1)];
    }

    /** What a reagent of this Thread (0 plain to 3) is worth in the song, in quarters. */
    public static int reagentQuarters(int thread) {
        return REAGENT_QUARTERS[Math.clamp(thread, 0, REAGENT_QUARTERS.length - 1)];
    }

    /** One for one kind of fuel burning, then a quarter more for each further kind; 0 with nothing burning. */
    public static double harmony(int kinds) {
        return kinds <= 0 ? 0 : 1 + HARMONY * (kinds - 1);
    }

    /**
     * Pulse a second for a beat, before machine rank and the pack's generation scale.
     *
     * <p>{@code voices} are the answered totems of the six (0 to 6). The song is {@link #SONG_SCALE} times the
     * Resonator's {@code voices x (voices + 2)} times the burning fuels' quarters over four: a crystal brings the
     * quarters it brings a Resonator (4, 6, 8 or 12), a reagent 4, 6, 9 or 12 by its Thread ({@code reagentThread}
     * 0 to 3, {@link #NO_REAGENT} for none) and water 2. The ley share is {@link #LEY_YIELD} a second per point of
     * strength at the heart, scaled by the answered voices. Both are then multiplied by the harmony, one and a
     * quarter for two kinds burning and one and a half for all three. Nothing burning, nothing made.
     */
    public static int outputFor(int voices, int crystalRank, int reagentThread, boolean water, int leyGain) {
        boolean reagent = reagentThread >= 0;
        int kinds = (crystalRank > 0 ? 1 : 0) + (reagent ? 1 : 0) + (water ? 1 : 0);
        if (kinds == 0 || voices <= 0) return 0;
        int quarters = PulseResonatorBlockEntity.quarters(crystalRank) + (reagent ? reagentQuarters(reagentThread) : 0)
                + (water ? WATER_QUARTERS : 0);
        double song = voices * (voices + 2) * quarters * SONG_SCALE / 4.0;
        double ley = LEY_YIELD * Math.max(0, leyGain) * voices / 6.0;
        return (int) Math.floor((song + ley) * harmony(kinds));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeyHeartBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if (!(level instanceof ServerLevel server)) return;
        if ((level.getGameTime() + pos.asLong()) % BEAT != 0) return;
        be.beat(server);
    }

    /** One beat: check the star, keep its threads, read the ley, burn and sing. Public for the GameTests. */
    public void beat(ServerLevel level) {
        BlockPos pos = worldPosition;
        Map<Attunement, ResonanceTotemBlockEntity> totems = star(level);
        answered = 0;
        if (complete) for (ResonanceTotemBlockEntity totem : totems.values())
            // answered by its keeping, and voiced: a totem whose lattice buffer has run dry is silent
            if (totem.keeping() == Keeping.State.ANSWERED && totem.voiced()) answered++;
        if (beats++ % SURVEY_BEATS == 0 || lastReading == null) survey(level);

        boolean stilled = stilled();
        int rank = crystalBurn > 0 ? crystalRank : PulseResonatorBlockEntity.catalystRank(items.get(CRYSTAL));
        int thread = reagentBurn > 0 ? reagentThread
                : Reagents.isReagent(items.get(REAGENT)) ? ReagentThread.get(items.get(REAGENT)) : NO_REAGENT;
        boolean reagent = thread != NO_REAGENT;
        boolean water = tank.getFluidAmount() >= WATER_PER_BEAT;
        singingRank = rank;
        singingThread = thread;
        int raw = complete && !stilled ? outputFor(answered, rank, thread, water, (int) Math.round(leyGain * surge)) : 0;
        int made = raw + tk.darrow.tribalpower.item.MachineRank.bonusGain(this, raw);
        made = tk.darrow.tribalpower.config.TribalConfig.scaleGeneration(made);
        output = made;
        int accepted = made > 0 ? pulse.insertPulse(made, false) : 0;
        if (accepted > 0) {
            burn(rank, reagent, water);
            // A totem you use stays answered: the heart's song is work for all six.
            for (ResonanceTotemBlockEntity totem : totems.values())
                if (totem.keeping() != Keeping.State.QUIET) totem.feed();
            setChanged();   // which also tells the comparators
        }
        state = !complete ? STATE_INCOMPLETE : stilled ? STATE_STILLED : made <= 0 ? STATE_IDLE
                : accepted <= 0 ? STATE_FULL : STATE_SINGING;
        boolean lit = accepted > 0;
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(LeyHeartBlock.LIT) && blockState.getValue(LeyHeartBlock.LIT) != lit)
            level.setBlock(pos, blockState.setValue(LeyHeartBlock.LIT, lit), 3);
        if (lit && level.getGameTime() % 60 < BEAT)
            for (Map.Entry<Attunement, ResonanceTotemBlockEntity> entry : totems.entrySet())
                tk.darrow.tribalpower.effect.SpiritEffects.ring(level, entry.getValue().getBlockPos().getCenter().add(0, 1.2, 0),
                        entry.getKey(), 0.45, 4);
    }

    /**
     * Whether the star is whole, keeping its six threads raised exactly while it is. A totem broken since the
     * last look has already lowered the threads (the rite store drops them with the totem), so a whole match
     * with no threads is looked at again before anything is raised.
     */
    private Map<Attunement, ResonanceTotemBlockEntity> star(ServerLevel level) {
        BlockPos pos = worldPosition;
        complete = star.satisfied(level, pos, 1);
        int raised = LeyLines.raisedBy(level, pos).size();
        if (complete && raised != Attunement.values().length) {
            star.invalidate();
            complete = star.satisfied(level, pos, 1);
        }
        Map<Attunement, ResonanceTotemBlockEntity> totems = complete ? totems(level) : Map.of();
        if (complete && totems.size() != Attunement.values().length) complete = false;
        if (complete && raised != Attunement.values().length) {
            Map<Attunement, BlockPos> at = new EnumMap<>(Attunement.class);
            totems.forEach((voice, totem) -> at.put(voice, totem.getBlockPos()));
            LeyLines.raise(level, pos, at);
            for (Map.Entry<Attunement, BlockPos> entry : at.entrySet())
                tk.darrow.tribalpower.effect.SpiritEffects.ring(level, entry.getValue().getCenter().add(0, 1.2, 0), entry.getKey(), 0.8, 12);
        } else if (!complete && raised > 0) {
            LeyLines.lower(level, pos);
        }
        return complete ? totems : Map.of();
    }

    /** The six totems of the matched star, by voice. */
    private Map<Attunement, ResonanceTotemBlockEntity> totems(Level level) {
        Map<Attunement, ResonanceTotemBlockEntity> out = new EnumMap<>(Attunement.class);
        PatternMatcher.Match match = star.cached();
        if (match == null || !match.found()) return out;
        RitualPattern.Tier tier = ModPatterns.LEY_HEART.tier(match.tier());
        if (tier == null) return out;
        for (RitualPattern.Cell cell : tier.cells()) {
            if (cell.predicate().role() != BlockPredicate.Role.TOTEM) continue;
            if (level.getBlockEntity(match.cell(cell, worldPosition)) instanceof ResonanceTotemBlockEntity totem)
                out.put(totem.getAttunement(), totem);
        }
        return out;
    }

    /**
     * The ley strength at the heart, as a Ley Collector standing here would read it, except that the heart's own
     * six threads are left out: they leave the heart, they do not feed it. What the six totems draw in from the
     * planet's veins does count, and that is most of what makes a good site.
     */
    private void survey(ServerLevel level) {
        BlockPos pos = worldPosition;
        var magnets = LeyMagnets.near(level, pos);
        int hearts = LeyLines.heartEpoch(level);
        if (lastReading == null || !magnets.equals(lastMagnets) || hearts != lastHearts) {
            lastReading = LeyField.sample(level, pos, magnets, pos);
            lastMagnets = magnets;
            lastHearts = hearts;
        }
        leyGain = LeyMath.factors(level, pos, lastReading).gain();
        surge = tk.darrow.tribalpower.event.LeySurges.multiplier(level, lastReading);
    }

    /** Charge each burning fuel for the beat that was just delivered, lighting a fresh one where needed. */
    private void burn(int rank, boolean reagent, boolean water) {
        if (rank > 0) {
            if (crystalBurn <= 0) {
                items.get(CRYSTAL).shrink(1);
                if (items.get(CRYSTAL).isEmpty()) items.set(CRYSTAL, ItemStack.EMPTY);
                crystalRank = rank;
                crystalBurn = crystalTicks(rank);
            }
            crystalBurn = Math.max(0, crystalBurn - BEAT);
        }
        if (reagent) {
            if (reagentBurn <= 0) {
                reagentThread = ReagentThread.get(items.get(REAGENT));
                items.get(REAGENT).shrink(1);
                if (items.get(REAGENT).isEmpty()) items.set(REAGENT, ItemStack.EMPTY);
                reagentBurn = reagentTicks(reagentThread);
            }
            reagentBurn = Math.max(0, reagentBurn - BEAT);
        }
        if (water) tank.drain(WATER_PER_BEAT, IFluidHandler.FluidAction.EXECUTE);
    }

    /** Force the next beat to survey the land again. For the GameTests, which do not wait two beats. */
    public void resurvey() {
        lastReading = null;
        beats = 0;
    }

    public boolean stilled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    public boolean complete() { return complete; }
    public int answered() { return answered; }
    public int leyGain() { return leyGain; }
    public int crystalBurn() { return crystalBurn; }
    public int reagentBurn() { return reagentBurn; }
    public int reagentThread() { return reagentThread; }

    /** The star's current match, for the Codex's missing-piece report and ghosts. */
    public PatternMatcher.Match match(Level level) {
        return star.get(level, worldPosition);
    }

    public void invalidatePattern() {
        star.invalidate();
    }

    // ---- PulseGenerator ----------------------------------------------------------------------------

    /** The Loom is the voice that weaves: the heart speaks for the six as one thread-maker. */
    @Override public Attunement voice() { return Attunement.LOOM; }

    @Override public int currentOutput() { return stilled() ? 0 : output; }

    @Override
    public List<Component> breakdown() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(complete ? "diag.tribalpower.ley_heart.complete" : "diag.tribalpower.ley_heart.incomplete")
                .withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        lines.add(Component.translatable("diag.tribalpower.ley_heart.voices", answered));
        lines.add(Component.translatable("diag.tribalpower.ley_heart.ley", leyGain, LeyMath.MAX_GAIN));
        int rank = crystalBurn > 0 ? crystalRank : 0;
        lines.add(rank > 0 ? Component.translatable("diag.tribalpower.ley_heart.crystal",
                        PulseResonatorBlockEntity.quarters(rank), crystalBurn / 20)
                : Component.translatable("diag.tribalpower.ley_heart.no_crystal"));
        lines.add(reagentBurn > 0 ? Component.translatable("diag.tribalpower.ley_heart.reagent",
                        ReagentThread.name(reagentThread), reagentQuarters(reagentThread), reagentBurn / 20)
                : Component.translatable("diag.tribalpower.ley_heart.no_reagent"));
        lines.add(Component.translatable("diag.tribalpower.ley_heart.water", tank.getFluidAmount(), TANK_CAPACITY));
        int kinds = (singingRank > 0 ? 1 : 0) + (singingThread != NO_REAGENT ? 1 : 0) + (tank.getFluidAmount() >= WATER_PER_BEAT ? 1 : 0);
        lines.add(Component.translatable("diag.tribalpower.ley_heart.harmony", String.valueOf(harmony(kinds)), kinds,
                15 * tk.darrow.tribalpower.item.MachineRank.rank(this)));
        lines.add(Component.translatable("diag.tribalpower.ley_heart.output", currentOutput()));
        return lines;
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>(breakdown());
        PatternMatcher.Match match = match(server);
        if (!match.found()) lines.addAll(match.report(6));
        else lines.add(Component.translatable("diag.tribalpower.ley_heart.threads", LeyLines.raisedBy(server, pos).size()));
        if (complete && answered < Attunement.values().length)
            lines.add(Component.translatable("diag.tribalpower.ley_heart.wake").withStyle(ChatFormatting.YELLOW));
        if (stilled()) lines.add(Component.translatable("diag.tribalpower.paused").withStyle(ChatFormatting.RED));
        if (!canReceivePulse()) lines.add(Component.translatable("diag.tribalpower.output_full").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    @Override public int getPulseStored() { return pulse.getPulseStored(); }
    @Override public int getPulseCapacity() { return pulse.getPulseCapacity(); }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) setChanged();
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) setChanged();
        return n;
    }

    /** Comparator: how full the heart's own buffer is. */
    public int signal() {
        int stored = getPulseStored();
        return stored == 0 ? 0 : 1 + 14 * stored / Math.max(1, getPulseCapacity());
    }

    // ---- intakes -----------------------------------------------------------------------------------

    @Override public SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return INPUTS; }
    @Override public int[] outputSlots(Direction face) { return NONE; }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(items, slot, count);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = ContainerHelper.takeItem(items, slot);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == CRYSTAL ? PulseResonatorBlockEntity.isCatalyst(stack) : slot == REAGENT && Reagents.isReagent(stack);
    }

    @Override public int[] getSlotsForFace(Direction face) { return SideIo.slots(this, face); }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return face != null && !stilled() && sides.get(face).insert() && canPlaceItem(slot, stack);
    }

    /** Nothing comes back out of a fire that is burning it. */
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public Component getDisplayName() { return Component.translatable("block.tribalpower.ley_heart"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LeyHeartMenu(id, inventory, this, data);
    }

    // ---- saving ------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CrystalBurn", crystalBurn);
        tag.putInt("CrystalRank", crystalRank);
        tag.putInt("ReagentBurn", reagentBurn);
        tag.putInt("ReagentThread", reagentThread);
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        crystalBurn = Math.max(0, tag.getInt("CrystalBurn"));
        crystalRank = Math.clamp(tag.getInt("CrystalRank"), 0, CRYSTAL_TICKS.length - 1);
        reagentBurn = Math.max(0, tag.getInt("ReagentBurn"));
        reagentThread = Math.clamp(tag.getInt("ReagentThread"), 0, ReagentThread.MAX);
        sides.load(tag);
    }
}
