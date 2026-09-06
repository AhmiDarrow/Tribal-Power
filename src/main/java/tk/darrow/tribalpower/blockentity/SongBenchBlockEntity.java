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
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Lattice hub that advances Echo-stage materials when Pulse and the right attunement are present.
 */
public class SongBenchBlockEntity extends BlockEntity implements Container {
    public static final int SLOT = 0;
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
        if (!be.singing) {
            return;
        }

        ItemStack stack = be.items.get(SLOT);
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

        int needPulse = stage.pulsePerTick();
        if (LatticeNetwork.extractPulseNearby(level, pos, RADIUS, needPulse, true) < needPulse) {
            be.stall("pulse");
            return;
        }
        LatticeNetwork.extractPulseNearby(level, pos, RADIUS, needPulse, false);

        be.stallReason = "";
        be.progress++;
        if (be.progress >= stage.workTicks()) {
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

    public void startSong() {
        linkedTotems = level == null ? 0 : LatticeNetwork.countNearbyTotems(level, worldPosition, RADIUS);
        singing = linkedTotems > 0 && EchoStage.isProcessable(items.get(SLOT));
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
                        stage.workTicks(),
                        linkedTotems
                );
            }
        };
    }

    public ItemStack takeItem() {
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
        if (stack.isEmpty() || !EchoStage.isProcessable(stack) || !items.get(SLOT).isEmpty()) {
            return false;
        }
        items.set(SLOT, stack.split(1));
        progress = 0;
        singing = false;
        stallReason = "";
        setChanged();
        return true;
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
            progress = 0;
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
        progress = 0;
        setChanged();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        singing = tag.getBoolean("Singing");
        progress = tag.getInt("Progress");
        linkedTotems = tag.getInt("LinkedTotems");
        stallReason = tag.getString("StallReason");
    }
}
