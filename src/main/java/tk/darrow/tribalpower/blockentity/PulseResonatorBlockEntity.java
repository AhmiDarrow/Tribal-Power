package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.block.PulseResonatorBlock;

/**
 * Harmonic generator: a reusable Echo catalyst amplifies distinct nearby totem voices.
 *
 * <p>3.1 adds the Voice Ring (design 3.1 section 5). Scattered totems still work exactly as they did, but
 * they are capped at {@link #UNARRANGED_VOICES}: six-voice resonance only counts when the voices are
 * actually arranged in a ring at radius 3. Placement is the ritual.
 */
public class PulseResonatorBlockEntity extends BlockEntity implements PulseHandler, WorldlyContainer, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CAPACITY = 2500;
    public static final int GAIN_INTERVAL = 20;
    public static final int SLOT = 0;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int harmonics;
    private int gain;
    private boolean ringed;
    private int wear;
    private final tk.darrow.tribalpower.pattern.PatternState ring =
            new tk.darrow.tribalpower.pattern.PatternState(tk.darrow.tribalpower.pattern.ModPatterns.VOICE_RING);

    /** Voices a heap of unarranged totems is worth. The sixth has to be asked for properly. */
    public static final int UNARRANGED_VOICES = 5;

    public PulseResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_RESONATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PulseResonatorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % GAIN_INTERVAL != 0) return;
        var voices = java.util.EnumSet.noneOf(tk.darrow.tribalpower.api.pulse.Attunement.class);
        for (var totem : tk.darrow.tribalpower.lattice.LatticeNetwork.findNearbyTotems(level, pos, 8))
            if (totem.keeping() == tk.darrow.tribalpower.lattice.Keeping.State.ANSWERED)
                voices.add(totem.getAttunement());
        int raw = voices.size()
                + tk.darrow.tribalpower.lattice.LatticeNetwork.countKinshipTribes(level, pos, 8); // Kinship Totems: extra tribe voices
        be.ringed = be.ring.satisfied(level, pos, 1);
        be.harmonics = be.ringed ? raw : Math.min(UNARRANGED_VOICES, raw);
        int rank = catalystRank(be.items.get(SLOT));
        be.gain = rank > 0 && voices.size() >= 1 && be.harmonics >= 2 && !level.hasNeighborSignal(pos)
                ? gainFor(be.harmonics, rank) : 0;
        be.gain += tk.darrow.tribalpower.item.MachineRank.bonusGain(be, be.gain);
        int made = be.gain > 0 ? be.insertPulse(be.gain, false) : 0;
        boolean sounding = made > 0;
        if (sounding) be.wearCatalyst(made);
        if (state.getValue(PulseResonatorBlock.LIT) != sounding)
            level.setBlock(pos, state.setValue(PulseResonatorBlock.LIT, sounding), 3);
        if (sounding && level instanceof net.minecraft.server.level.ServerLevel server)
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server, pos.getCenter().add(0, 0.4, 0),
                    tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT, 0.65, 8);
    }

    /**
     * What each catalyst is worth as a multiplier, in quarters.
     *
     * <p>Rank used to be an addend: at six voices the four catalysts paid 48, 60, 72 and 84, so
     * carrying a Resonant Core instead of the Echo Shard you already had was worth a third more and
     * not worth the walk. A catalyst is the thing the whole block is named around, so it multiplies
     * the voices now -- the same six voices pay 48, 72, 96 and 144.
     */
    private static final int[] TIER_QUARTERS = {0, 4, 6, 8, 12};

    /**
     * What the resonator makes a second: the voices, squared-ish, times the catalyst.
     *
     * <p>The original {@code 2 * voices + 2 * rank} topped out at 20 a second for six voices in a
     * ring under a Resonant Core -- less than a single Drumheart on a one-second clock. Voices
     * multiply, so the sixth is worth more than the second and the ring is worth arranging, and the
     * catalyst multiplies on top of that, so every tier is a real upgrade rather than a rounding
     * error. Integer division rounds down, which only ever costs a Pulse or two.
     */
    public static int gainFor(int harmonics, int rank) {
        if (rank <= 0 || rank >= TIER_QUARTERS.length) return 0;
        return harmonics * (harmonics + 2) * TIER_QUARTERS[rank] / 4;
    }

    /**
     * Pulse a catalyst is worth before it crumbles.
     *
     * <p>The catalyst used to be permanent, which made a built resonator free forever. It now wears
     * by what it has actually produced, not by how long it has sat there, so a big resonator eats
     * catalysts faster than a small one and the buff above pays for itself. A Resonant Core under a
     * full six-voice ring lasts about thirty-five minutes.
     */
    public static int endurance(int rank) {
        // A better catalyst both makes more and lasts longer, so upgrading pays twice. Roughly
        // seven minutes on a shard up to three quarters of an hour on a core, at each tier's own
        // six-voice output.
        return switch (rank) {
            case 1 -> 20_000;       // Echo Shard
            case 2 -> 60_000;       // Attuned Echo
            case 3 -> 150_000;      // Bound Echo
            case 4 -> 400_000;      // Resonant Core
            default -> 0;
        };
    }

    /**
     * Charge the catalyst for Pulse actually delivered; crumble it when it is spent.
     *
     * <p>Public so the gate in LatticeGameTests can prove a catalyst is not permanent without
     * having to stand up six answered totems to make the resonator run on its own.
     */
    public void wearCatalyst(int made) {
        ItemStack catalyst = items.get(SLOT);
        int rank = catalystRank(catalyst);
        int life = endurance(rank);
        if (life <= 0) return;
        wear += made;
        while (wear >= life && !catalyst.isEmpty()) {
            wear -= life;
            catalyst.shrink(1);
            if (level instanceof net.minecraft.server.level.ServerLevel server)
                tk.darrow.tribalpower.effect.SpiritEffects.ring(server, worldPosition.getCenter().add(0, 0.4, 0),
                        tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT, 0.35, 4);
        }
        if (catalyst.isEmpty()) {
            items.set(SLOT, ItemStack.EMPTY);
            wear = 0;
        }
        setChanged();
    }

    /** How much of the seated catalyst is left, 0..1, or 0 when there is none. */
    public float catalystLife() {
        int life = endurance(catalystRank(items.get(SLOT)));
        return life <= 0 ? 0F : Math.max(0F, 1F - (float) wear / life);
    }

    public int getGain() { return gain; }
    public int getHarmonics() { return harmonics; }
    /** True when the totems stand in a ring rather than in a heap. */
    public boolean isRinged() { return ringed; }
    public static int catalystRank(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(tk.darrow.tribalpower.item.ModItems.RESONANT_CORE.get())) return 4;
        if (stack.is(tk.darrow.tribalpower.item.ModItems.BOUND_ECHO.get())) return 3;
        if (stack.is(tk.darrow.tribalpower.item.ModItems.ATTUNED_ECHO.get())) return 2;
        if (stack.is(tk.darrow.tribalpower.item.ModItems.ECHO_SHARD.get())) return 1;
        return 0;
    }
    public static boolean isCatalyst(ItemStack stack) { return catalystRank(stack) > 0; }

    /** Seat one reusable Echo catalyst from the held stack; returns true if accepted. */
    public boolean acceptCatalyst(ItemStack stack) {
        if (!isCatalyst(stack)) {
            return false;
        }
        ItemStack slot = items.get(SLOT);
        if (slot.isEmpty()) {
            items.set(SLOT, stack.split(1));
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack takeCatalyst() {
        ItemStack taken = items.get(SLOT);
        items.set(SLOT, ItemStack.EMPTY);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    public int catalystCount() {
        return items.get(SLOT).getCount();
    }

    @Override
    public int getPulseStored() {
        return pulse.getPulseStored();
    }

    @Override
    public int getPulseCapacity() {
        return pulse.getPulseCapacity();
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isCatalyst(stack);
    }

    private static final int[] NO_SLOTS = new int[0];
    @Override public int[] getSlotsForFace(Direction face) { return NO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("CatalystWear", wear);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        wear = tag.getInt("CatalystWear");
        // Legacy stored coal is retained for extraction, but no longer generates Pulse.
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        ItemStack catalyst = items.get(SLOT);
        int rank = catalystRank(catalyst);
        lines.add(rank > 0 ? net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.catalyst", catalyst.getHoverName(), rank)
                : net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.no_catalyst").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if (rank > 0) lines.add(net.minecraft.network.chat.Component.translatable(
                "diag.tribalpower.resonator.catalyst_life", Math.round(catalystLife() * 100))
                .withStyle(catalystLife() < 0.15F ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.GRAY));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.harmonics", harmonics, gain));
        if (harmonics < 2) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.need_voices").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if (!canReceivePulse()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
