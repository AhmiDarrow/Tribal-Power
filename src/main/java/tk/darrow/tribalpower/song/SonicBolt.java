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
import tk.darrow.tribalpower.config.TribalConfig;
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

    public static SonicBolt shoot(ServerPlayer player, ItemStack bow, @Nullable SongVerse verse, float pull) {
        float speed = (float) (TribalConfig.bowVelocity() * (0.75 + 0.25 * pull));
        return shoot(player, bow, verse, pull, speed, 1.0, (float) TribalConfig.bowSpread());
    }

    /**
     * What a bolt hits for, before it slows in flight. A draw hits for its share of the full-draw damage, as a vanilla
     * bow does, so tapping the bow loosely is no faster way to kill than drawing it. A verse adds its power on top, up
     * to a cap, and never hits softer than a plain bolt.
     */
    public static double damage(float pull, @Nullable SongVerse verse) {
        return (TribalConfig.bowDamage() + verseBonus(verse)) * Math.clamp(pull, 0F, 1F);
    }

    /** What a verse adds to a full bow draw: verseDamagePerPower for each copy of its lead reagent, up to the cap. */
    public static double verseBonus(@Nullable SongVerse verse) {
        return verse == null ? 0 : Math.min(TribalConfig.verseDamageCap(), TribalConfig.verseDamagePerPower() * verse.power());
    }

    /** A bolt at a given speed, damage multiplier and spread; the crossbow's is faster, heavier and truer. */
    public static SonicBolt shoot(ServerPlayer player, ItemStack bow, @Nullable SongVerse verse, float pull, float speed, double damageScale, float spread) {
        ServerLevel level = player.serverLevel();
        SonicBolt bolt = new SonicBolt(ModEntities.SONIC_BOLT.get(), level);
        bolt.setOwner(player);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        bolt.verse = verse;
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, spread);
        // An arrow hits for base damage times its speed, rounded up; divide by the speed it actually left at (the spread
        // nudges it) and shave a hair, so a bolt lands for the damage meant and not a stray point more.
        double launched = Math.max(0.1, bolt.getDeltaMovement().length());
        bolt.setBaseDamage(Math.max(0, damage(pull, verse) * damageScale - 1.0E-3) / launched);
        level.addFreshEntity(bolt);
        Attunement voice = verse == null ? Attunement.SPIRIT : verse.voice();
        SpiritEffects.ring(level, new Vec3(bolt.getX(), bolt.getY(), bolt.getZ()), voice, 0.35, 8);
        return bolt;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > TribalConfig.boltLifetimeTicks()) discard();
        if (level() instanceof ServerLevel server && tickCount % 2 == 0) {
            Attunement voice = verse == null ? Attunement.SPIRIT : verse.voice();
            SpiritEffects.ring(server, position(), voice, 0.15, 4);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        float before = hit.getEntity() instanceof LivingEntity living ? living.getHealth() : 0;
        super.onHitEntity(hit);
        // a shield turns the bolt and its verse with it
        if (verse != null && hit.getEntity() instanceof LivingEntity living && getOwner() instanceof ServerPlayer player
                && (living.getHealth() < before || living.hurtTime > 0 || living.isDeadOrDying())) {
            SongCast.onArrow(player, living, verse);
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        discard();
    }

    /**
     * A bolt is a note in the air for a moment, nothing to keep. An arrow with no pickup item cannot be
     * written to disk at all, so a world that saved with one in flight logged an error; now it is simply not saved.
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Never picked up, but an arrow with no item at all cannot be written out (a data read, an inspecting mod). */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(net.minecraft.world.item.Items.ARROW);
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
