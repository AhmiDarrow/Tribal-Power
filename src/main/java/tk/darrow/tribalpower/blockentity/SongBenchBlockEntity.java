package tk.darrow.tribalpower.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.RitualChalkItem;
import tk.darrow.tribalpower.song.Reagents;
import tk.darrow.tribalpower.song.SongBenchMenu;
import tk.darrow.tribalpower.song.SongSheetItem;
import tk.darrow.tribalpower.song.SongbookItem;
import tk.darrow.tribalpower.song.VerseArrowItem;

/**
 * Where a reagent becomes empowered, a sheet is written, and a verse arrow is fletched. Echo refining
 * lives on the dedicated stations. This bench only sings.
 */
public class SongBenchBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer, MenuProvider,
        tk.darrow.tribalpower.api.Diagnosable, tk.darrow.tribalpower.lattice.HasSideIo {
    public static final int PAPER = 0, CHALK = 1, BOOK = 2, OUTPUT = 3, SIZE = 4;
    private static final int[] INPUTS = {PAPER, CHALK};
    private static final int[] OUTPUTS = {OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final List<String> sequence = new ArrayList<>();
    private String voice = "";
    private final tk.darrow.tribalpower.lattice.SideIo sides =
            new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH);

    public SongBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SONG_BENCH.get(), pos, state);
    }

    /** Old worlds may still have an Echo item seated. Put it back in the world instead of deleting it. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SongBenchBlockEntity be) {
        for (int slot = 0; slot < BOOK; slot++) {
            ItemStack stack = be.items.get(slot);
            if (!stack.isEmpty() && !be.canPlaceItem(slot, stack)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                be.items.set(slot, ItemStack.EMPTY);
                be.setChanged();
            }
        }
        ItemStack book = be.items.get(BOOK);
        if (!book.isEmpty() && !(book.getItem() instanceof SongbookItem)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, book);
            be.items.set(BOOK, ItemStack.EMPTY);
            be.setChanged();
        }
    }

    public List<String> sequence() {
        return List.copyOf(sequence);
    }

    public @Nullable Attunement voice() {
        return voice.isEmpty() ? null : Attunement.byName(voice);
    }

    public void setVoice(Attunement attunement) {
        voice = attunement == null ? "" : attunement.getSerializedName();
        setChanged();
    }

    public boolean append(String reagentId) {
        if (sequence.size() >= 7 || Reagents.byId(reagentId) == null) return false;
        sequence.add(reagentId);
        setChanged();
        return true;
    }

    public void pop() {
        if (!sequence.isEmpty()) {
            sequence.remove(sequence.size() - 1);
            setChanged();
        }
    }

    public void clearSequence() {
        if (!sequence.isEmpty()) {
            sequence.clear();
            setChanged();
        }
    }

    /** Conductors used to feed this bench Pulse for Echo work. Songs pay in a single draw instead. */
    public boolean wantsPulseAssist() {
        return false;
    }

    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return INPUTS; }
    @Override public int[] outputSlots(Direction face) { return OUTPUTS; }
    @Override public int[] getSlotsForFace(Direction face) { return tk.darrow.tribalpower.lattice.SideIo.slots(this, face); }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return level != null && !level.hasNeighborSignal(worldPosition) && sides.get(face).insert() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return level != null && !level.hasNeighborSignal(worldPosition) && sides.get(face).extract() && slot == OUTPUT;
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
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
        sequence.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case PAPER -> stack.is(Items.PAPER);
            case CHALK -> stack.getItem() instanceof RitualChalkItem;
            case BOOK -> stack.getItem() instanceof SongbookItem;
            default -> false;
        };
    }

    public boolean canPlaceOutput(ItemStack stack) {
        return stack.getItem() instanceof SongSheetItem || stack.getItem() instanceof VerseArrowItem;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tribalpower.song_bench");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SongBenchMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        ListTag list = new ListTag();
        for (String id : sequence) list.add(StringTag.valueOf(id));
        tag.put("Sequence", list);
        tag.putString("Voice", voice);
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        sequence.clear();
        ListTag list = tag.getList("Sequence", Tag.TAG_STRING);
        for (int i = 0; i < list.size() && sequence.size() < 7; i++) {
            String id = list.getString(i);
            if (Reagents.byId(id) != null) sequence.add(id);
        }
        voice = tag.getString("Voice");
        sides.load(tag);
    }

    @Override
    public List<Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.song_bench.state", sequence.size()));
        Attunement singing = voice();
        if (singing != null) {
            lines.add(Component.translatable("diag.tribalpower.song_bench.voice",
                    Component.translatable("attunement.tribalpower." + singing.getSerializedName())));
        }
        lines.add(Component.translatable("diag.tribalpower.song_bench.empower", tk.darrow.tribalpower.song.SongBenchLogic.EMPOWER_PULSE));
        return lines;
    }
}
