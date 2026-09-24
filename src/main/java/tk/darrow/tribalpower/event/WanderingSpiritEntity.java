package tk.darrow.tribalpower.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.SpiritWispEntity;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritgearHelper;

/**
 * A wandering spirit: a rare, friendly light that drifts near a player for a while. Reach it and it leaves a
 * line of the biome's story and a small gift, then goes. Left alone, it fades on its own.
 */
public class WanderingSpiritEntity extends SpiritWispEntity {
    private int life;

    public WanderingSpiritEntity(EntityType<? extends WanderingSpiritEntity> type, Level level) {
        super(type, level);
        setGlowingTag(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.GLOW, getX(), getY() + 0.3, getZ(), 0, 0.01, 0);
            return;
        }
        if (++life >= TribalConfig.wanderingSpiritLifeSeconds() * 20) fade((ServerLevel) level());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(level() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        gift(server, player);
        return InteractionResult.CONSUME;
    }

    /** The biome's story and a gift from it: the guardian's reagent, or now and then a Resonant Core. */
    public void gift(ServerLevel server, Player player) {
        var biome = server.getBiome(blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("march_steppe");
        Guardian guardian = null;
        for (Guardian g : Guardian.values()) if (g.biome.equals(biome)) guardian = g;
        String place = guardian == null ? "steppe" : guardian.biome.replace("march_", "");
        int line = 1 + random.nextInt(3);
        player.sendSystemMessage(Component.translatable("lore.tribalpower.spirit." + place + "." + line).withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
        ItemStack gift = random.nextInt(8) == 0 ? new ItemStack(ModItems.RESONANT_CORE.get())
                : guardian == null ? new ItemStack(ModItems.SPIRIT_SHARD.get(), 2) : new ItemStack(guardian.callItem(), 2 + random.nextInt(3));
        SpiritgearHelper.give(player, gift);
        server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1F, 1.4F);
        fade(server);
    }

    private void fade(ServerLevel server) {
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 20, 0.3, 0.3, 0.3, 0.04);
        discard();
    }

    @Override public boolean removeWhenFarAway(double distance) { return true; }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getInt("Life");
    }
}
