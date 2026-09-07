package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.rite.RiteHelper;

/**
 * Pedestal for Seals & Rites — apply seals to enact shamanic effects.
 */
public class RitePedestalBlock extends Block {
    public static final MapCodec<RitePedestalBlock> CODEC = simpleCodec(RitePedestalBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public RitePedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) return ItemInteractionResult.CONSUME;

        if (stack.is(ModItems.BLANK_SEAL.get()) || stack.is(ModItems.SPIRIT_SEAL.get())
                || stack.is(ModItems.EARTH_SEAL.get()) || stack.is(ModItems.FIRE_SEAL.get())
                || stack.is(ModItems.WATER_SEAL.get()) || stack.is(ModItems.AIR_SEAL.get())) {
            if (!level.isClientSide) {
                boolean ok = RiteHelper.performSealRite(level, pos, player, stack);
                player.displayClientMessage(Component.translatable(
                        ok ? "message.tribalpower.rite.success" : "message.tribalpower.rite.fail"
                ), true);
                if (ok && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
