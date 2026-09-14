package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.echo.EchoStage;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Lattice hub that advances Echo-stage materials when Pulse and the right attunement are present.
 */
public class SongBenchBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer, tk.darrow.tribalpower.api.Diagnosable, tk.darrow.tribalpower.lattice.HasSideIo {
    public static final int SLOT = 0;
    private static final int[] SLOTS_ARR = {0};
    private final tk.darrow.tribalpower.lattice.SideIo sides = new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH);
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(net.minecraft.core.Direction face) { return SLOTS_ARR; }
    @Override public int[] outputSlots(net.minecraft.core.Direction face) { return SLOTS_ARR; }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction face) { return tk.darrow.tribalpower.lattice.SideIo.slots(this, face); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return !level.hasNeighborSignal(worldPosition) && sides.get(face).insert() && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return !level.hasNeighborSignal(worldPosition) && sides.get(face).extract() && !singing; }
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private boolean singing;
    private int progress;
    private int linkedTotems;
    private String stallReason = "";

    public SongBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SONG_BENCH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SongBenchBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if (!be.singing || level.hasNeighborSignal(pos)) {
            return;
        }

        ItemStack stack = be.items.get(SLOT);
        // Other mods and saved data can bypass slot limits. Preserve excess input for recovery.
        if (stack.getCount() > 1) {
            be.singing = false;
            be.progress = 0;
            be.setChanged();
            return;
        }
        EchoStage stage = EchoStage.forInput(stack);
        if (stage == null) {
            be.stall("empty");
            return;
        }

        be.linkedTotems = LatticeNetwork.countNearbyTotems(level, pos, RADIUS);
        if (be.linkedTotems <= 0) {
            be.stall("no_totems");
            return;
        }

        Attunement needed = stage.requiredAttunement();
        if (!LatticeNetwork.hasAttunement(level, pos, RADIUS, needed)) {
            be.stall("attunement:" + needed.getSerializedName());
            return;
        }

        var keeping = Keeping.voice(level, pos, needed);
        if (keeping == Keeping.State.QUIET && be.progress == 0) {
            be.stall("quiet");
            return;
        }
        int needPulse = be.pulsePerTick(stage);
        if (LatticeNetwork.extractPulseNearby(level, pos, RADIUS, needPulse, true) < needPulse) {
            be.stall("pulse");
            return;
        }
        LatticeNetwork.extractPulseNearby(level, pos, RADIUS, needPulse, false);

        be.stallReason = "";
        be.progress++;
        if (keeping != Keeping.State.QUIET) Keeping.feedWork(level, pos, needed);
        if (be.progress >= Keeping.stretch(keeping, be.workTicks(stage))) {
            ItemStack out = new ItemStack(stage.output());
            be.items.set(SLOT, out);
            be.progress = 0;
            // Continue into the next Echo stage when still processable; stop on Manifested Ingot.
            if (!EchoStage.isProcessable(out)) {
                be.singing = false;
            }
            be.setChanged();
        } else {
            be.setChanged();
        }
    }

    private void stall(String reason) {
        if (!reason.equals(stallReason)) {
            stallReason = reason;
            setChanged();
        }
    }

    private int workTicks(EchoStage stage) {
        return MachineRank.scaleTime(this, stage.workTicks());
    }

    private int pulsePerTick(EchoStage stage) {
        return MachineRank.scalePulse(this, stage.pulsePerTick());
    }

    public void startSong() {
        linkedTotems = level == null ? 0 : LatticeNetwork.countNearbyTotems(level, worldPosition, RADIUS);
        singing = linkedTotems > 0 && EchoStage.isProcessable(items.get(SLOT)) && items.get(SLOT).getCount() == 1;
        if (!singing) {
            progress = 0;
            if (linkedTotems <= 0) {
                stallReason = "no_totems";
            } else if (!EchoStage.isProcessable(items.get(SLOT))) {
                stallReason = "empty";
            }
        } else {
            stallReason = "";
        }
        setChanged();
    }

    public void stopSong() {
        singing = false;
        setChanged();
    }

    public boolean isSinging() {
        return singing;
    }

    public int getProgress() {
        return progress;
    }

    public int getLinkedTotems() {
        return linkedTotems;
    }

    public String getStallReason() {
        return stallReason;
    }

    /**
     * Conductor assist target: singing and Pulse-starved, or seated grit waiting to sing.
     */
    public boolean wantsPulseAssist() {
        ItemStack stack = items.get(SLOT);
        if (!EchoStage.isProcessable(stack)) {
            return false;
        }
        if (singing) {
            return "pulse".equals(stallReason) || stallReason.isEmpty();
        }
        return true;
    }

    public Component statusMessage() {
        ItemStack stack = items.get(SLOT);
        EchoStage stage = EchoStage.forInput(stack);
        if (!singing) {
            return Component.translatable("message.tribalpower.song_bench.idle");
        }
        if (stage == null) {
            return Component.translatable("message.tribalpower.song_bench.need_item");
        }
        return switch (stallReason) {
            case "no_totems" -> Component.translatable("message.tribalpower.song_bench.no_totems");
            case "pulse" -> Component.translatable("message.tribalpower.song_bench.no_pulse");
            case "quiet" -> Component.translatable("message.tribalpower.song_bench.quiet");
            case "empty" -> Component.translatable("message.tribalpower.song_bench.need_item");
            default -> {
                if (stallReason.startsWith("attunement:")) {
                    String name = stallReason.substring("attunement:".length());
                    yield Component.translatable(
                            "message.tribalpower.song_bench.need_attunement",
                            Component.translatable("attunement.tribalpower." + name)
                    );
                }
                yield Component.translatable(
                        "message.tribalpower.song_bench.working",
                        stage.name(),
                        progress,
                        workTicks(stage),
                        linkedTotems
                );
            }
        };
    }

    public ItemStack takeItem() {
        if (level != null && level.hasNeighborSignal(worldPosition)) return ItemStack.EMPTY;
        ItemStack stack = items.get(SLOT);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(SLOT, ItemStack.EMPTY);
        progress = 0;
        singing = false;
        setChanged();
        return stack;
    }

    public boolean insertItem(ItemStack stack) {
        if (level != null && level.hasNeighborSignal(worldPosition)) return false;
        if (stack.isEmpty() || !EchoStage.isProcessable(stack) || !items.get(SLOT).isEmpty()) {
            return false;
        }
        items.set(SLOT, stack.split(1));
        progress = 0;
        stallReason = "";
        startSong();
        return true;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() { return 1; }

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
            progress = 0;
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            progress = 0;
            singing = false;
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        progress = 0;
        if (EchoStage.isProcessable(stack) && stack.getCount() == 1) startSong();
        else {
            singing = false;
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        progress = 0;
        singing = false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return EchoStage.isProcessable(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("Singing", singing);
        tag.putInt("Progress", progress);
        tag.putInt("LinkedTotems", linkedTotems);
        tag.putString("StallReason", stallReason);
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        singing = tag.getBoolean("Singing");
        progress = tag.getInt("Progress");
        EchoStage stage = EchoStage.forInput(items.get(SLOT));
        if (stage == null || progress < 0 || progress >= workTicks(stage)) progress = 0;
        linkedTotems = tag.getInt("LinkedTotems");
        stallReason = tag.getString("StallReason");
        sides.load(tag);
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.song_bench.state", statusMessage()));
        EchoStage stage = EchoStage.forInput(items.get(SLOT));
        if (stage != null) {
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.song_bench.stage", stage.name(), progress, workTicks(stage), pulsePerTick(stage)));
            if (!LatticeNetwork.hasAttunement(server, pos, RADIUS, stage.requiredAttunement()))
                lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.missing_attunement",
                        net.minecraft.network.chat.Component.translatable("attunement.tribalpower." + stage.requiredAttunement().getSerializedName())).withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        if (!stallReason.isEmpty()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.song_bench.stall", stallReason).withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
