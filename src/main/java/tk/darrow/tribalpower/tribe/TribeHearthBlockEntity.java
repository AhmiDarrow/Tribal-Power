package tk.darrow.tribalpower.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Stores the hearth's tribe ({@code Tribe} int NBT) and the rank of the last player who touched it. */
public class TribeHearthBlockEntity extends BlockEntity {
    private TribeDefinition tribe = TribeDefinition.SOIL;
    @Nullable private UUID lastPlayer;
    private int lastRank;
    private boolean registered;

    public TribeHearthBlockEntity(BlockPos pos, BlockState state) {
        super(TribeRegistry.HEARTH_TYPE.get(), pos, state);
    }

    public TribeDefinition tribe() { return tribe; }

    public void setTribe(TribeDefinition tribe) {
        this.tribe = tribe;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (level instanceof ServerLevel server) TribeHooks.hearth(server, worldPosition, tribe, true);
        }
    }

    /** Comparator: rank (0-4) of the last player who touched the hearth, scaled to 0-15. */
    public int signal() { return lastRank * 15 / (TribeRank.values().length - 1); }

    public void touched(UUID player, TribeRank rank) {
        lastPlayer = player;
        lastRank = rank.ordinal();
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    @Nullable public UUID lastPlayer() { return lastPlayer; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TribeHearthBlockEntity be) {
        if (!be.registered && level instanceof ServerLevel server) { TribeHooks.hearth(server, pos, be.tribe, true); be.registered = true; }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) TribeHooks.hearth(server, worldPosition, tribe, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TribeDefinition.NBT_KEY, tribe.ordinal());
        tag.putInt("LastRank", lastRank);
        if (lastPlayer != null) tag.putUUID("LastPlayer", lastPlayer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tribe = TribeDefinition.byOrdinal(tag.getInt(TribeDefinition.NBT_KEY));
        lastRank = tag.getInt("LastRank");
        lastPlayer = tag.hasUUID("LastPlayer") ? tag.getUUID("LastPlayer") : null;
        registered = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(TribeDefinition.NBT_KEY, tribe.ordinal());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
