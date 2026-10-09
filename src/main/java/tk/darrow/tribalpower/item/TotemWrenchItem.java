package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
import tk.darrow.tribalpower.logic.LogicPlateBlockEntity;
import tk.darrow.tribalpower.logic.PlateLinks;

import java.util.List;

/**
 * Totem Wrench — turns a block to face somewhere else, and sneak-used on a machine face it steps that face
 * through input, output, both and shut. It never breaks anything: a block that cannot turn simply does not.
 * On song plates it syncs instead: crouch-use one to hold its song, then use another to make it hear the held
 * one with no wire between (see {@link PlateLinks}).
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
        // A plate is never turned, and reading its syncs stays open to all, so PlateLinks asks about ownership
        // only for the uses that change a plate.
        if (be instanceof LogicPlateBlockEntity plate && level instanceof ServerLevel server)
            return PlateLinks.useWrench(server, player, context.getItemInHand(), plate, context.getClickLocation(),
                    () -> damage(context.getItemInHand(), player));
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
        // A bed, a double chest, a door or a tall plant is two blocks that must face together, and an extended piston
        // is a base and its head (or one mid-push); turning one half tears them apart.
        if (state.hasProperty(BlockStateProperties.BED_PART) || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE)
                && state.getValue(BlockStateProperties.CHEST_TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                || state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)
                || state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.piston.MovingPistonBlock) {
            player.displayClientMessage(Component.translatable("message.tribalpower.wrench.fixed"), true);
            return InteractionResult.FAIL;
        }
        BlockState turned = turn(state, level, pos, face);
        // a torch, lever or ladder turned off its wall would only fall off it
        if (turned == null || turned == state || !turned.canSurvive(level, pos)) {
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
                // skip a way it could not hang, so a wall torch steps round to the next wall that holds it
                BlockState candidate = state.setValue(property, next);
                if (candidate.canSurvive(level, pos)) return candidate;
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
        if (SpiritgearHelper.tryConsumePulse(player, tk.darrow.tribalpower.config.TribalConfig.wrenchUsePulse())) return;
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /** Crouch-use in the air lets go of a held plate's song. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || PlateLinks.held(stack) == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.remove(tk.darrow.tribalpower.logic.LogicRegistry.HELD_PLATE.get());
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.released"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.totem_wrench.desc"));
        GlobalPos held = PlateLinks.held(stack);
        if (held != null)
            tooltip.add(Component.translatable("item.tribalpower.totem_wrench.holding",
                    held.pos().getX(), held.pos().getY(), held.pos().getZ()).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
