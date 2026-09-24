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
        if (level instanceof ServerLevel server && (level.getGameTime() + pos.asLong()) % 100 == 0 && tk.darrow.tribalpower.event.Festivals.active(be.tribe, level)) be.festival(server, pos);
    }

    /** Festival day: the camp's fires burn, and the hearth throws its tribe's colour into the air. */
    private void festival(ServerLevel server, BlockPos pos) {
        tk.darrow.tribalpower.effect.SpiritEffects.ring(server, pos.getCenter().add(0, 1, 0), tribe.attunement(), 3, 18);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, 8, 0.6, 0.6, 0.6, 0.05);
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-8, -2, -8), pos.offset(8, 3, 8))) {
            var state = server.getBlockState(near);
            if (state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock && !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
                    && !state.getValue(net.minecraft.world.level.block.CampfireBlock.WATERLOGGED))
                server.setBlock(near, state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true), 3);
        }
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
