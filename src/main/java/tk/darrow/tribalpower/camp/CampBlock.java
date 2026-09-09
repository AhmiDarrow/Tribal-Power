package tk.darrow.tribalpower.camp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.item.PulseCellItem;

public class CampBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public static final MapCodec<CampBlock> CODEC=simpleCodec(CampBlock::new);
    public CampBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(LIT,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LIT);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return CampShapes.shape(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CampBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,CampRegistry.TYPE.get(),CampBlockEntity::tick);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(placer instanceof Player player&&level.getBlockEntity(pos) instanceof CampBlockEntity be){be.owner=player.getUUID();be.ownerName=player.getGameProfile().getName();be.setChanged();}
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof PulseCellItem){
            if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CampBlockEntity be&&!level.hasNeighborSignal(pos)){int n=PulseCellItem.extractPulse(stack,CampBlockEntity.CAPACITY-be.pulse,false);be.pulse+=n;be.setChanged();player.displayClientMessage(be.status(),true);}
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CampBlockEntity be){
            player.displayClientMessage(be.status(),true);
            if(be.hasInventory()&&!level.hasNeighborSignal(pos)&&!player.isShiftKeyDown()&&be.canAccess(player))player.openMenu(be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moved){
        if(!level.isClientSide&&level.hasNeighborSignal(pos)&&level.getBlockEntity(pos) instanceof CampBlockEntity be){be.deactivate();}
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof CampBlockEntity be?be.signal():0;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof CampBlockEntity be){be.deactivate();Containers.dropContents(level,pos,be);level.updateNeighbourForOutputSignal(pos,this);}
        super.onRemove(state,level,pos,next,moved);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.world.level.material.PushReaction.BLOCK;}
}
