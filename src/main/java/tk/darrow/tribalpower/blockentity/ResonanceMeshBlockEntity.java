package tk.darrow.tribalpower.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.grit.GritRegistry;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;
import tk.darrow.tribalpower.pit.OreBand;
import tk.darrow.tribalpower.tribe.KinshipTotemBlockEntity;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHearthBlockEntity;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeStanding;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The Listening Pit (design 3.1 section 7): a mesh that gives the ore somewhere to land.
 *
 * <p>Each voice trades one resource for another rather than multiplying yield. Water washes for an extra
 * output every third cycle at the price of 250 mB. Air quickens the cycle and charges more Pulse a second
 * for it, so the Pulse per item is unchanged and only throughput moves. Fire opens the Nether's ores at
 * twice the Pulse. Spirit listens a band deeper. The Loom halves the substrate.
 *
 * <p>Design note: the design says the depth band selects the table while Fire and Spirit "unlock" the hot
 * and rare bands. Taken literally that would mean lighting a Fire totem costs you access to iron. Resolved
 * here as: depth picks the default band, and a sample naming something in an unlocked deeper band asks for
 * that band instead. Unlocks widen what can be asked for; they never narrow it.
 */
public class ResonanceMeshBlockEntity extends LatticeDeviceBlockEntity implements PulseHandler {
    public static final int SAMPLE = 0;
    public static final int SUBSTRATE_A = 1;
    public static final int SUBSTRATE_B = 2;
    public static final int OUTPUT_FIRST = 3;
    public static final int SLOTS = 7;

    public static final int TANK_CAPACITY = 4000;
    public static final int WASH_COST = 250;
    public static final int WASH_EVERY = 3;
    /** A buffer so bursty generation -- a mob farm's Wake Bell, say -- does not stutter the cycle. */
    public static final int PULSE_BUFFER = 200;

    private static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;

    public final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return stilled() ? 0 : super.fill(resource, action);
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private final PulseStorage buffer = new PulseStorage(PULSE_BUFFER);
    private int work;
    private int cycles;
    private String state = "idle";
    private OreBand band = OreBand.COMMON;
    private String calling = "";

    public ResonanceMeshBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_MESH.get(), pos, state, SLOTS, ModPatterns.LISTENING_PIT);
    }

    // ---- inventory -------------------------------------------------------------------------

    @Override protected boolean isOutputSlot(int slot) { return slot >= OUTPUT_FIRST; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SAMPLE -> GritRegistry.inputMaterial(stack) != null || GritRegistry.materialOf(stack) != null;
            case SUBSTRATE_A, SUBSTRATE_B -> OreBand.isSubstrate(stack);
            default -> false;
        };
    }

    /** Top is the sample, sides feed substrate, the bottom gives back what the ground offered. */
    @Override
    public int[] getSlotsForFace(Direction face) {
        return switch (face) {
            case UP -> new int[]{SAMPLE};
            case DOWN -> new int[]{OUTPUT_FIRST, OUTPUT_FIRST + 1, OUTPUT_FIRST + 2, OUTPUT_FIRST + 3};
            default -> new int[]{SUBSTRATE_A, SUBSTRATE_B};
        };
    }

    // ---- pulse -----------------------------------------------------------------------------

    @Override public int getPulseStored() { return buffer.getPulseStored(); }
    @Override public int getPulseCapacity() { return buffer.getPulseCapacity(); }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = buffer.insertPulse(amount, simulate);
        if (!simulate && n > 0) setChanged();
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        // The buffer feeds this mesh only: a pit is not a battery for the rest of the base.
        return 0;
    }

    /** Pull from the lattice into the buffer, then spend from the buffer. */
    private boolean spend(Level level, BlockPos pos, int cost) {
        if (buffer.getPulseStored() < cost) {
            int want = Math.min(PULSE_BUFFER - buffer.getPulseStored(), Math.max(cost * 2, cost));
            buffer.insertPulse(LatticeNetwork.extractPulseNearby(level, pos, RADIUS, want, false), false);
        }
        if (buffer.getPulseStored() < cost) return false;
        buffer.extractPulse(cost, false);
        return true;
    }

    // ---- state -----------------------------------------------------------------------------

    public OreBand band() { return band; }
    public int work() { return work; }
    public String state() { return state; }

    /** Comparator: progress through the cycle, and nothing at all when the pattern is broken. */
    public int progressSignal() {
        if (!"working".equals(state)) return 0;
        int seconds = seconds(band, voices(level, worldPosition));
        return seconds <= 0 ? 0 : Math.max(1, Math.min(15, 1 + 14 * work / seconds));
    }

    public Component status() {
        return Component.translatable("message.tribalpower.mesh." + state,
                Component.translatable("band.tribalpower." + band.key()), work, seconds(band, voices(level, worldPosition)));
    }

    private Set<Attunement> voices(Level level, BlockPos pos) {
        return level == null ? EnumSet.noneOf(Attunement.class) : LatticeNetwork.collectAttunements(level, pos, RADIUS);
    }

    /** Air's Quickening: a shorter cycle bought with a heavier draw, so Pulse per item does not move. */
    private int seconds(OreBand band, Set<Attunement> voices) {
        int seconds = band.seconds();
        if (voices.contains(Attunement.AIR)) seconds = Math.max(1, (int) Math.round(seconds * 0.65));
        return seconds;
    }

    private int pulsePerSecond(OreBand band, Set<Attunement> voices) {
        int pulse = band.pulsePerSecond();
        if (voices.contains(Attunement.AIR)) pulse = (int) Math.round(pulse * 1.55);
        return pulse;
    }

    /** The Loom's Threading: half the substrate, rounded up, because half a stone is still a stone. */
    private List<ItemStack> substrate(OreBand band, Set<Attunement> voices) {
        List<ItemStack> cost = new ArrayList<>();
        boolean threaded = voices.contains(Attunement.LOOM);
        for (ItemStack stack : band.substrate()) {
            int count = threaded ? (stack.getCount() + 1) / 2 : stack.getCount();
            cost.add(stack.copyWithCount(count));
        }
        return cost;
    }

    // ---- requirements ----------------------------------------------------------------------

    /** A Tribe Hearth or a Grit-singer Kinship Totem standing inside the pattern (section 7.2). */
    public boolean kinshipPresent(Level level, BlockPos pos) {
        BoundingBox box = pattern.pattern().boundsAround(pos);
        for (KinshipTotemBlockEntity totem : LatticeNetwork.findNearbyKinshipTotems(level, pos, RADIUS))
            if (totem.tribe() == TribeDefinition.STONE && box.isInside(totem.getBlockPos())) return true;
        for (BlockPos cursor : BlockPos.betweenClosed(
                new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ())))
            if (level.hasChunkAt(cursor) && level.getBlockEntity(cursor) instanceof TribeHearthBlockEntity) return true;
        return false;
    }

    /** The Ancestral Cache the pit empties into. Tier 1 asks for one adjacent; the pit will not run blind. */
    public Container cache(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos side = pos.relative(face);
            if (level.hasChunkAt(side) && level.getBlockEntity(side) instanceof AncestralCacheBlockEntity cache
                    && !level.hasNeighborSignal(side)) return cache;
        }
        return null;
    }

    /** The owner's standing with the Grit-singers, which is what gates how deep the pit may listen. */
    private TribeRank standing(Level level) {
        if (owner() == null) return TribeRank.VOICE;
        MinecraftServer server = level.getServer();
        return server == null ? TribeRank.STRANGER : TribeStanding.rank(server, owner(), TribeDefinition.STONE);
    }

    private boolean unlocked(OreBand band, int tier, Set<Attunement> voices, TribeRank rank, boolean kinship) {
        if (tier < band.patternTier()) return false;
        if (band.patternTier() >= 2 && !kinship) return false;
        return switch (band) {
            case COMMON -> rank.ordinal() >= TribeRank.FRIEND.ordinal();
            case DEEP -> rank.ordinal() >= TribeRank.KIN.ordinal();
            case HOT -> rank.ordinal() >= TribeRank.KIN.ordinal() && voices.contains(Attunement.FIRE);
            case RARE -> rank.ordinal() >= TribeRank.VOICE.ordinal() && voices.contains(Attunement.SPIRIT);
        };
    }

    /** How deep the pit listens by depth alone: below zero is a band down, and Spirit is another. */
    private OreBand depthBand(BlockPos pos, Set<Attunement> voices) {
        int depth = (pos.getY() < 0 ? 1 : 0) + (voices.contains(Attunement.SPIRIT) ? 1 : 0);
        return depth >= 1 ? OreBand.DEEP : OreBand.COMMON;
    }

    /** The band this cycle listens to, or null when the pit may not listen at all. */
    public OreBand selectBand(Level level, BlockPos pos, int tier, Set<Attunement> voices) {
        TribeRank rank = standing(level);
        boolean kinship = kinshipPresent(level, pos);
        String sample = sampleMaterial();
        if (sample != null) {
            OreBand asked = OreBand.of(sample);
            // A sample naming a deeper band asks for it; one the band does not hold is simply ignored.
            if (asked != null && unlocked(asked, tier, voices, rank, kinship)) return asked;
        }
        OreBand wanted = depthBand(pos, voices);
        if (unlocked(wanted, tier, voices, rank, kinship)) return wanted;
        return unlocked(OreBand.COMMON, tier, voices, rank, kinship) ? OreBand.COMMON : null;
    }

    /** The material named by the sample slot. The sample is a filter, never an ingredient. */
    public String sampleMaterial() {
        ItemStack sample = getItem(SAMPLE);
        if (sample.isEmpty()) return null;
        String grit = GritRegistry.materialOf(sample);
        if (grit != null) return grit;
        GritRegistry.Material input = GritRegistry.inputMaterial(sample);
        return input == null ? null : input.name();
    }

    // ---- the beat --------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState blockState, ResonanceMeshBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (be.stilled()) { be.state = "paused"; return; }

        be.drain(level, pos);

        PatternMatcher.Match match = be.pattern.get(level, pos);
        if (!match.found()) { be.stall("pattern"); return; }
        int tier = match.tier();

        Set<Attunement> voices = be.voices(level, pos);
        if (!voices.contains(Attunement.EARTH)) { be.stall("attunement"); return; }

        Container cache = be.cache(level, pos);
        if (cache == null) { be.stall("cache"); return; }

        OreBand band = be.selectBand(level, pos, tier, voices);
        if (band == null) { be.stall("standing"); return; }
        if (band != be.band) { be.band = band; be.work = 0; be.calling = ""; }

        List<ItemStack> substrate = be.substrate(band, voices);
        if (!be.hasSubstrate(substrate)) { be.stall("substrate"); return; }

        if (be.calling.isEmpty()) {
            OreBand.Entry entry = band.roll(level.random, be.sampleMaterial());
            if (entry == null) { be.stall("nothing"); return; }
            be.calling = entry.material();
        }
        ItemStack result = be.result(level, pos, band);
        if (result.isEmpty()) { be.calling = ""; be.stall("nothing"); return; }
        if (!be.placeOutput(result, true)) { be.state = "full"; return; }

        int cost = be.pulsePerSecond(band, voices);
        if (!be.spend(level, pos, cost)) { be.state = "pulse"; return; }

        be.state = "working";
        be.work++;
        SpiritEffects.ring((ServerLevel) level, pos.getCenter().add(0, 0.55, 0), Attunement.EARTH, 0.5, 8);

        if (be.work >= be.seconds(band, voices)) {
            be.finish(level, pos, band, voices, substrate, result);
        }
        be.setChanged();
        level.updateNeighbourForOutputSignal(pos, blockState.getBlock());
    }

    private void finish(Level level, BlockPos pos, OreBand band, Set<Attunement> voices,
                        List<ItemStack> substrate, ItemStack result) {
        takeSubstrate(substrate);
        cycles++;
        ItemStack yield = result.copy();
        // Rare bands sometimes give twice; Washing gives an extra every third cycle for its 250 mB.
        if (band == OreBand.RARE && level.random.nextFloat() < 0.25F) yield.grow(result.getCount());
        if (voices.contains(Attunement.WATER) && cycles % WASH_EVERY == 0
                && tank.drain(WASH_COST, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount() >= WASH_COST)
            yield.grow(1);
        placeOutput(yield, false);
        work = 0;
        calling = "";
        tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, owner(), "journey/circle");
        tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, owner(), "journey/ask_the_ground");
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.8F);
    }

    private ItemStack result(Level level, BlockPos pos, OreBand band) {
        boolean deepslate = pos.getY() < 0 || level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.DEEPSLATE);
        for (OreBand.Entry entry : band.entries())
            if (entry.material().equals(calling)) return entry.result(deepslate);
        return ItemStack.EMPTY;
    }

    private void stall(String why) {
        state = why;
        work = 0;
        setChanged();
    }

    private boolean hasSubstrate(List<ItemStack> cost) {
        for (ItemStack want : cost) {
            int found = 0;
            for (int slot : new int[]{SUBSTRATE_A, SUBSTRATE_B}) {
                ItemStack held = getItem(slot);
                if (ItemStack.isSameItemSameComponents(held, want)) found += held.getCount();
            }
            if (found < want.getCount()) return false;
        }
        return true;
    }

    private void takeSubstrate(List<ItemStack> cost) {
        for (ItemStack want : cost) {
            int remaining = want.getCount();
            for (int slot : new int[]{SUBSTRATE_A, SUBSTRATE_B}) {
                if (remaining <= 0) break;
                ItemStack held = getItem(slot);
                if (!ItemStack.isSameItemSameComponents(held, want)) continue;
                int taken = Math.min(remaining, held.getCount());
                held.shrink(taken);
                remaining -= taken;
            }
        }
    }

    /** Empty the output slots into the cache, so the normal build needs no hopper. */
    private void drain(Level level, BlockPos pos) {
        Container cache = cache(level, pos);
        if (cache == null) return;
        for (int slot = OUTPUT_FIRST; slot < SLOTS; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            int moved = insert(cache, stack);
            if (moved > 0) { stack.shrink(moved); setChanged(); }
        }
    }

    private static int insert(Container container, ItemStack stack) {
        int moved = 0;
        for (int slot = 0; slot < container.getContainerSize() && moved < stack.getCount(); slot++) {
            ItemStack held = container.getItem(slot);
            if (held.isEmpty()) {
                int count = stack.getCount() - moved;
                container.setItem(slot, stack.copyWithCount(count));
                moved += count;
            } else if (ItemStack.isSameItemSameComponents(held, stack)) {
                int room = Math.min(container.getMaxStackSize(), held.getMaxStackSize()) - held.getCount();
                int count = Math.min(room, stack.getCount() - moved);
                if (count > 0) { held.grow(count); moved += count; }
            }
        }
        if (moved > 0) container.setChanged();
        return moved;
    }

    // ---- diagnostics -----------------------------------------------------------------------

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.mesh.state", status()));
        PatternMatcher.Match match = pattern.get(server, pos);
        lines.addAll(match.report(3));
        lines.add(Component.translatable("diag.tribalpower.mesh.water", tank.getFluidAmount(), TANK_CAPACITY));
        lines.add(Component.translatable("diag.tribalpower.mesh.buffer", buffer.getPulseStored(), PULSE_BUFFER));
        if (!match.found()) return lines;

        Set<Attunement> voices = voices(server, pos);
        lines.add(Component.translatable("diag.tribalpower.mesh.standing",
                Component.translatable(standing(server).translationKey()),
                Component.translatable(kinshipPresent(server, pos)
                        ? "diag.tribalpower.mesh.kinship_yes" : "diag.tribalpower.mesh.kinship_no")));
        if (cache(server, pos) == null)
            lines.add(Component.translatable("diag.tribalpower.mesh.no_cache").withStyle(ChatFormatting.YELLOW));

        OreBand selected = selectBand(server, pos, match.tier(), voices);
        if (selected == null) {
            lines.add(Component.translatable("diag.tribalpower.mesh.no_band").withStyle(ChatFormatting.YELLOW));
            return lines;
        }
        lines.add(Component.translatable("diag.tribalpower.mesh.band",
                Component.translatable("band.tribalpower." + selected.key()),
                seconds(selected, voices), pulsePerSecond(selected, voices)));
        StringBuilder substrates = new StringBuilder();
        for (ItemStack stack : substrate(selected, voices)) {
            if (!substrates.isEmpty()) substrates.append(", ");
            substrates.append(stack.getCount()).append(' ').append(stack.getHoverName().getString());
        }
        lines.add(Component.translatable("diag.tribalpower.mesh.substrate", substrates.toString()));

        String sample = sampleMaterial();
        if (sample == null) lines.add(Component.translatable("diag.tribalpower.mesh.no_sample").withStyle(ChatFormatting.GRAY));
        else if (selected.biasedBy(sample))
            lines.add(Component.translatable("diag.tribalpower.mesh.sample", sample));
        else
            lines.add(Component.translatable("diag.tribalpower.mesh.sample_absent", sample,
                    Component.translatable("band.tribalpower." + selected.key())).withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    // ---- persistence -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Work", work);
        tag.putInt("Cycles", cycles);
        tag.putString("Band", band.key());
        tag.putString("Calling", calling);
        buffer.save(tag);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        work = Math.max(0, tag.getInt("Work"));
        cycles = Math.max(0, tag.getInt("Cycles"));
        calling = tag.getString("Calling");
        for (OreBand value : OreBand.values()) if (value.key().equals(tag.getString("Band"))) band = value;
        buffer.load(tag);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
