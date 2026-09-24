package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.block.LiquidBlock;
import tk.darrow.tribalpower.TribalPower;

import java.util.List;

/**
 * A tank you can carry. The flask fills and empties against any tank, machine or fluid it is used on, and
 * carries far more than a bucket without ever splitting into stacks. Sneak-use to pour a bucket back out.
 */
public class SpiritFlaskItem extends Item {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TribalPower.MOD_ID);

    /** The flask's contents; NeoForge's own fluid component type so other mods' pipes read it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> CONTENT =
            COMPONENTS.register("flask_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    private final int capacity;

    public SpiritFlaskItem(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static FluidStack contents(ItemStack stack) {
        return stack.getOrDefault(CONTENT.get(), SimpleFluidContent.EMPTY).copy();
    }

    private static void set(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty()) stack.remove(CONTENT.get());
        else stack.set(CONTENT.get(), SimpleFluidContent.copyOf(fluid));
    }

    /** The handler the capability hands out; also what other mods fill the flask through. */
    public static net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler(ItemStack stack) {
        int size = stack.getItem() instanceof SpiritFlaskItem flask ? flask.capacity : FluidType.BUCKET_VOLUME;
        return new FluidHandlerItemStack(CONTENT, stack, size);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        // A tank, machine or pipe end: fill from it, or pour into it when it takes what we carry.
        IFluidHandler block = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                pos, context.getClickedFace());
        if (block != null) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (drawFrom(stack, block, player) || pourInto(stack, block, player)) return InteractionResult.CONSUME;
            return InteractionResult.FAIL;
        }
        return interactWithWorld(level, player, context.getHand(), stack, pos, context.getClickedFace())
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player,
                player.isShiftKeyDown() ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack))
            return InteractionResultHolder.pass(stack);
        return interactWithWorld(level, player, hand, stack, pos, hit.getDirection())
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.pass(stack);
    }

    /** Sneak pours a bucket out in front of you; otherwise a fluid source in the world is drunk up. */
    private boolean interactWithWorld(Level level, Player player, InteractionHand hand, ItemStack stack,
                                      BlockPos pos, Direction face) {
        FluidStack held = contents(stack);
        if (player.isShiftKeyDown()) {
            if (held.getAmount() < FluidType.BUCKET_VOLUME) return false;
            BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(face);
            if (!level.mayInteract(player, target) || !player.mayUseItemAt(target, face, stack)) return false;
            if (level.isClientSide) return true;
            if (!place(level, target, held)) return false;
            if (!player.getAbilities().instabuild) {
                held.shrink(FluidType.BUCKET_VOLUME);
                set(stack, held);
            }
            level.playSound(null, target, held.getFluid().getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource() || (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held,
                new FluidStack(fluid.getType(), FluidType.BUCKET_VOLUME)))) return false;
        if (held.getAmount() + FluidType.BUCKET_VOLUME > capacity) return false;
        if (level.isClientSide) return true;
        BlockState state = level.getBlockState(pos);
        // Only a block that can give its fluid up is drunk from; kelp, seagrass and modded machines are left alone.
        if (!(state.getBlock() instanceof BucketPickup pickup)) return false;
        ItemStack taken = pickup.pickupBlock(player, level, pos, state);
        if (taken.isEmpty()) return false;
        FluidStack gained = new FluidStack(fluid.getType(), FluidType.BUCKET_VOLUME);
        set(stack, held.isEmpty() ? gained : grown(held, FluidType.BUCKET_VOLUME));
        level.playSound(null, pos, gained.getFluid().getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static FluidStack grown(FluidStack stack, int amount) {
        FluidStack copy = stack.copy();
        copy.grow(amount);
        return copy;
    }

    private static boolean place(Level level, BlockPos pos, FluidStack held) {
        BlockState state = level.getBlockState(pos);
        if (!(held.getFluid() instanceof net.minecraft.world.level.material.FlowingFluid flowing)) return false;
        if (state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(null, level, pos, state, held.getFluid())) {
            return container.placeLiquid(level, pos, state, flowing.getSource(false));
        }
        if (!state.canBeReplaced() && !state.isAir()) return false;
        if (level.dimensionType().ultraWarm() && held.getFluid().isSame(Fluids.WATER)) {
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.LAVA_FIZZ, pos, 0);
            return true;
        }
        if (!state.isAir() && !state.liquid()) level.destroyBlock(pos, true);
        return level.setBlock(pos, flowing.getSource(false).createLegacyBlock(), Block.UPDATE_ALL);
    }

    private boolean drawFrom(ItemStack stack, IFluidHandler source, Player player) {
        IFluidHandler flask = handler(stack);
        FluidStack offered = source.drain(capacity, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return false;
        int accepted = flask.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        FluidStack taken = source.drain(new FluidStack(offered.getFluid(), accepted), IFluidHandler.FluidAction.EXECUTE);
        if (taken.isEmpty()) return false;
        flask.fill(taken, IFluidHandler.FluidAction.EXECUTE);
        copyBack(stack, flask, player);
        return true;
    }

    private boolean pourInto(ItemStack stack, IFluidHandler sink, Player player) {
        IFluidHandler flask = handler(stack);
        FluidStack held = flask.drain(capacity, IFluidHandler.FluidAction.SIMULATE);
        if (held.isEmpty()) return false;
        int accepted = sink.fill(held, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        FluidStack taken = flask.drain(new FluidStack(held.getFluid(), accepted), IFluidHandler.FluidAction.EXECUTE);
        sink.fill(taken, IFluidHandler.FluidAction.EXECUTE);
        copyBack(stack, flask, player);
        return true;
    }

    /** The handler works on its own copy of the stack; carry the result back to the hand. */
    private static void copyBack(ItemStack stack, IFluidHandler flask, Player player) {
        if (flask instanceof FluidHandlerItemStack handler) {
            ItemStack result = handler.getContainer();
            set(stack, contents(result));
            player.getInventory().setChanged();
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.clamp(Math.round(13.0F * contents(stack).getAmount() / capacity), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid.isEmpty() ? 0x3FFFE0
                : net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid())
                        .getTintColor(fluid) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = contents(stack);
        tooltip.add(Component.translatable("item.tribalpower.spirit_flask.desc", capacity / FluidType.BUCKET_VOLUME));
        if (fluid.isEmpty()) tooltip.add(Component.translatable("item.tribalpower.spirit_flask.empty")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        else tooltip.add(Component.translatable("item.tribalpower.spirit_flask.holding",
                fluid.getHoverName(), fluid.getAmount(), capacity).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
