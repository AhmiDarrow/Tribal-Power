package tk.darrow.tribalpower.familiar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible one-beat redstone source a sitting Storm Moth leaves in the air it occupies.
 * Scheduled tick removes it; the moth places the next click on its own clock.
 */
public class SpiritClickBlock extends Block {
    public static final MapCodec<SpiritClickBlock> CODEC=simpleCodec(SpiritClickBlock::new);
    public SpiritClickBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) { return 15; }
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) { return 15; }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        level.updateNeighborsAt(pos,this);
        for(Direction dir:Direction.values())level.updateNeighborsAt(pos.relative(dir),this);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState neu,boolean moved) {
        if(!state.is(neu.getBlock())) {
            level.updateNeighborsAt(pos,this);
            for(Direction dir:Direction.values())level.updateNeighborsAt(pos.relative(dir),this);
        }
        super.onRemove(state,level,pos,neu,moved);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        level.removeBlock(pos,false);
    }
}
