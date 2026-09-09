package tk.darrow.tribalpower.familiar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/**
 * Invisible light (level 10) carried by a bonded Lantern Fox. Air-like, replaceable, no collision, no item, no block
 * entity. Self-cleaning: a scheduled tick removes it when no fox claims it any more (covers foxes unloaded elsewhere).
 */
public class SpiritLightBlock extends Block {
    public static final MapCodec<SpiritLightBlock> CODEC=simpleCodec(SpiritLightBlock::new);
    public static final int SWEEP_TICKS=100;
    /** A fleeing fox can be ~4 blocks from a light placed up to {@link FamiliarAbilities#LIGHT_PERIOD} ticks ago; look a little further before sweeping. */
    public static final int SWEEP_REACH=6;
    public SpiritLightBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override protected boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos) { return true; }
    @Override protected float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) { return 1F; }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) { level.scheduleTick(pos,this,SWEEP_TICKS); }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        boolean claimed=!level.getEntitiesOfClass(LatticeAnimal.class,new AABB(pos).inflate(SWEEP_REACH),a->a.isBonded() && pos.equals(a.lastLight())).isEmpty();
        if(claimed)level.scheduleTick(pos,this,SWEEP_TICKS);else level.removeBlock(pos,false);
    }
    @Override protected BlockState updateShape(BlockState state,net.minecraft.core.Direction direction,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos) { return state; }
}
