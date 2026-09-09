package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
 */
public class PulseResonatorBlockEntity extends BlockEntity implements PulseHandler, Container, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CAPACITY = 2500;
    public static final int GAIN_INTERVAL = 20;
    public static final int SLOT = 0;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int harmonics;
    private int gain;

    public PulseResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_RESONATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PulseResonatorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % GAIN_INTERVAL != 0) return;
        var voices = java.util.EnumSet.noneOf(tk.darrow.tribalpower.api.pulse.Attunement.class);
        for (var totem : tk.darrow.tribalpower.lattice.LatticeNetwork.findNearbyTotems(level, pos, 8))
            voices.add(totem.getAttunement());
        be.harmonics = voices.size();
        be.harmonics += tk.darrow.tribalpower.lattice.LatticeNetwork.countKinshipTribes(level, pos, 8); // Kinship Totems: extra tribe voices
        int rank = catalystRank(be.items.get(SLOT));
        be.gain = rank > 0 && be.harmonics >= 2 && !level.hasNeighborSignal(pos) ? 2 * be.harmonics + 2 * rank : 0;
        boolean sounding = be.gain > 0 && be.insertPulse(be.gain, false) > 0;
        if (state.getValue(PulseResonatorBlock.LIT) != sounding)
            level.setBlock(pos, state.setValue(PulseResonatorBlock.LIT, sounding), 3);
        if (sounding && level instanceof net.minecraft.server.level.ServerLevel server)
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server, pos.getCenter().add(0, 0.4, 0),
                    tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT, 0.65, 8);
    }

    public int getGain() { return gain; }
    public int getHarmonics() { return harmonics; }
    public static int catalystRank(ItemStack stack) {
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        ContainerHelper.saveAllItems(tag, items, registries);

    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        // Legacy stored coal is retained for extraction, but no longer generates Pulse.
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        ItemStack catalyst = items.get(SLOT);
        int rank = catalystRank(catalyst);
        lines.add(rank > 0 ? net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.catalyst", catalyst.getHoverName(), rank)
                : net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.no_catalyst").withStyle(net.minecraft.ChatFormatting.YELLOW));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.harmonics", harmonics, gain));
        if (harmonics < 2) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.resonator.need_voices").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if (!canReceivePulse()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
