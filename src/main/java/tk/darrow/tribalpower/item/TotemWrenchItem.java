package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.lattice.HasSideIo;

import java.util.List;

/**
 * Totem Wrench — turns a block to face somewhere else, and sneak-used on a machine face it steps that face
 * through input, output, both and shut. It never breaks anything: a block that cannot turn simply does not.
 */
public class TotemWrenchItem extends Item {
    public TotemWrenchItem(Properties properties) {
        super(properties.stacksTo(1).durability(512));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, context.getItemInHand()))
            return InteractionResult.FAIL;
        var be = level.getBlockEntity(pos);
        if (be instanceof Ownership.Owned owned && !Ownership.check(level, owned.owner(), player))
            return InteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            if (HasSideIo.cycle(player, be, face)) {
                damage(context.getItemInHand(), player);
                level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, 1.4F);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.tribalpower.wrench.no_faces"), true);
            return InteractionResult.FAIL;
        }

        BlockState state = level.getBlockState(pos);
        // A bed, a double chest or a door is two blocks that must face together; turning one half tears them apart.
        if (state.hasProperty(BlockStateProperties.BED_PART) || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE)
                && state.getValue(BlockStateProperties.CHEST_TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
            player.displayClientMessage(Component.translatable("message.tribalpower.wrench.fixed"), true);
            return InteractionResult.FAIL;
        }
        BlockState turned = turn(state, level, pos, face);
        if (turned == null || turned == state) {
            player.displayClientMessage(Component.translatable("message.tribalpower.wrench.fixed"), true);
            return InteractionResult.FAIL;
        }
        level.setBlockAndUpdate(pos, turned);
        damage(context.getItemInHand(), player);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.1F);
        return InteractionResult.CONSUME;
    }

    /** Turn around the clicked face where the block has a facing, else the block's own quarter turn. */
    private static BlockState turn(BlockState state, Level level, BlockPos pos, Direction face) {
        for (DirectionProperty property : List.of(BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING)) {
            if (!state.hasProperty(property)) continue;
            Direction current = state.getValue(property);
            for (int step = 1; step <= 6; step++) {
                Direction next = Direction.from3DDataValue((current.get3DDataValue() + step) % 6);
                if (!property.getPossibleValues().contains(next)) continue;
                return state.setValue(property, next);
            }
            return state;
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            var axis = state.getValue(BlockStateProperties.AXIS);
            return state.setValue(BlockStateProperties.AXIS, switch (axis) {
                case X -> Direction.Axis.Y;
                case Y -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
            });
        }
        BlockState rotated = state.rotate(level, pos, Rotation.CLOCKWISE_90);
        return rotated == state ? null : rotated;
    }

    private static void damage(ItemStack stack, Player player) {
        if (player.getAbilities().instabuild) return;
        if (SpiritgearHelper.tryConsumePulse(player, 1)) return;
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.totem_wrench.desc"));
    }
}
