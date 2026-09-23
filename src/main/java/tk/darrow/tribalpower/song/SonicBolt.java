package tk.darrow.tribalpower.song;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.entity.ModEntities;

/** The Pulse Bow's shot. It does not stick in the ground and it does not drop as an item. */
public class SonicBolt extends AbstractArrow {
    private @Nullable SongVerse verse = null;

    public SonicBolt(EntityType<? extends SonicBolt> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
        this.setNoGravity(true);
    }

    public static void shoot(ServerPlayer player, ItemStack bow, @Nullable SongVerse verse, float pull) {
        ServerLevel level = player.serverLevel();
        SonicBolt bolt = new SonicBolt(ModEntities.SONIC_BOLT.get(), level);
        bolt.setOwner(player);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        bolt.verse = verse;
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.4F + pull * 0.8F, 0.3F);
        double damage = verse == null ? 4.0 + 3.0 * pull : 3.0 + verse.power();
        bolt.setBaseDamage(damage);
        level.addFreshEntity(bolt);
        Attunement voice = verse == null ? Attunement.SPIRIT : verse.voice();
        SpiritEffects.ring(level, new Vec3(bolt.getX(), bolt.getY(), bolt.getZ()), voice, 0.35, 8);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 30) discard();
        if (level() instanceof ServerLevel server && tickCount % 2 == 0) {
            Attunement voice = verse == null ? Attunement.SPIRIT : verse.voice();
            SpiritEffects.ring(server, position(), voice, 0.15, 4);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (verse != null && hit.getEntity() instanceof LivingEntity living && getOwner() instanceof ServerPlayer player) {
            SongCast.onArrow(player, living, verse);
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        discard();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (verse != null) verse.write(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        verse = SongVerse.read(tag);
    }
}
