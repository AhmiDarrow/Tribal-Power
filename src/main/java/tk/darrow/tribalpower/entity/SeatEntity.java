package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The thing a player actually rides when they sit on a stool. It is invisible, never saved, and
 * lives only as long as someone is on it and the stool is still there — so a broken stool or a
 * reloaded world leaves nothing behind.
 */
public class SeatEntity extends Entity {
    /** Where the stool is. Transient because the seat is never written to disk. */
    private BlockPos anchor = BlockPos.ZERO;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Sits the player on the block at {@code pos}, unless someone is already there.
     * {@code height} is how far up the block the seat sits, in blocks.
     */
    public static boolean sit(Level level, BlockPos pos, Player player, double height) {
        if (level.isClientSide) return true;
        AABB here = new AABB(pos).inflate(0.1);
        if (!level.getEntitiesOfClass(SeatEntity.class, here).isEmpty()) return false;   // taken

        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        seat.anchor = pos.immutable();
        seat.setPos(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5);
        level.addFreshEntity(seat);
        return player.startRiding(seat);
    }

    @Override
    public void tick() {
        if (level().isClientSide) return;
        BlockState under = level().getBlockState(anchor);
        boolean stoolGone = !(under.getBlock() instanceof tk.darrow.tribalpower.block.CampDecorBlock decor)
                || decor.kind() != tk.darrow.tribalpower.block.CampDecorBlock.Kind.STOOL;
        if (getPassengers().isEmpty() || stoolGone) discard();
    }

    /** The rider sits exactly where the seat is; the seat is already placed at sitting height. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return Vec3.ZERO;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        anchor = blockPosition();
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override public boolean isPickable() { return false; }

    @Override public boolean shouldBeSaved() { return false; }
}
