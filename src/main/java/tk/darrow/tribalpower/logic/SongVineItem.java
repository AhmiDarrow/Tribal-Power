package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Places another tendril onto an existing vine cell, or starts a new cell on the clicked face. */
public class SongVineItem extends BlockItem {
    public SongVineItem(SongVineBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.is(LogicRegistry.SONG_VINE.get())) {
            Direction face = context.getClickedFace();
            if (!SongVineBlock.has(clickedState, face) && SongVineBlock.canAttach(level, clicked, face)) {
                if (!level.isClientSide) {
                    level.setBlock(clicked, SongVineBlock.withFace(clickedState, face, true), 3);
                    SoundType sound = clickedState.getSoundType(level, clicked, context.getPlayer());
                    level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS,
                            (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
                    if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                        context.getItemInHand().shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        Direction attach = context.getClickedFace().getOpposite();
        BlockPos vinePos = clicked.relative(context.getClickedFace());
        BlockState existing = level.getBlockState(vinePos);
        if (existing.is(LogicRegistry.SONG_VINE.get()) && !SongVineBlock.has(existing, attach)
                && SongVineBlock.canAttach(level, vinePos, attach)) {
            if (!level.isClientSide) {
                level.setBlock(vinePos, SongVineBlock.withFace(existing, attach, true), 3);
                if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                    context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }
}
