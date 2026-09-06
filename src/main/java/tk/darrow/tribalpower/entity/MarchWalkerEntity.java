package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;

/**
 * Quiet March fauna — wanders the steppe, shy of hostiles, curious about spirit shards.
 */
public class MarchWalkerEntity extends PathfinderMob {
    public MarchWalkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.9D, stack -> stack.is(ModItems.SPIRIT_SHARD.get()), false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 1.0D);
    }

    public static boolean checkSpawnRules(
            EntityType<MarchWalkerEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
    ) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.MARCH_GRASS.get())
                || below.is(ModBlocks.MARCH_SOIL.get())
                || below.is(ModBlocks.MARCH_STONE.get());
    }
}
