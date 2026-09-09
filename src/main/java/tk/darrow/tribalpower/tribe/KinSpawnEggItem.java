package tk.darrow.tribalpower.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

/** Spawn egg for one Kin role; sneak-use cycles the tribe stored in the egg's custom data. */
public class KinSpawnEggItem extends DeferredSpawnEggItem {
    private final KinRole role;

    public KinSpawnEggItem(KinRole role, int primary, int secondary, Properties properties) {
        super(TribeRegistry.TRIBAL_KIN, primary, secondary, properties);
        this.role = role;
    }

    public KinRole role() { return role; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            if (!level.isClientSide) {
                TribeDefinition next = TribeDefinition.byOrdinal(TribeDefinition.ofOrDefault(stack).ordinal() + 1);
                TribeDefinition.stamp(stack, next);
                ctx.getPlayer().displayClientMessage(next.displayNameComponent().withStyle(TribeStanding.colour(next)), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos();
        BlockPos spawnPos = level.getBlockState(pos).getBlock() instanceof LiquidBlock ? pos : pos.relative(ctx.getClickedFace());
        spawnKin((net.minecraft.server.level.ServerLevel) level, spawnPos, stack, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }

    /** Right-clicking water: vanilla's fluid path would spawn a bare Weaver of tribe 0, so route it through ours. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (level.isClientSide) return net.minecraft.world.InteractionResultHolder.success(stack);
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return net.minecraft.world.InteractionResultHolder.fail(stack);
        return spawnKin((net.minecraft.server.level.ServerLevel) level, pos, stack, player) != null
                ? net.minecraft.world.InteractionResultHolder.consume(stack) : net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    /** Spawns this egg's role with the egg's stamped tribe, anchored where it lands; consumes one egg in survival. */
    public TribalKinEntity spawnKin(net.minecraft.server.level.ServerLevel level, BlockPos spawnPos, ItemStack stack,
                                    @org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player player) {
        TribalKinEntity kin = TribeRegistry.TRIBAL_KIN.get().spawn(level, spawnPos, MobSpawnType.SPAWN_EGG);
        if (kin != null) {
            kin.setRole(role);
            kin.setTribe(TribeDefinition.ofOrDefault(stack));
            kin.setAnchor(spawnPos);
            if (player == null || !player.isCreative()) stack.shrink(1);
        }
        return kin;
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        return tribe == null ? super.getName(stack)
                : net.minecraft.network.chat.Component.translatable(getDescriptionId(stack) + ".named", tribe.displayNameComponent());
    }

    /** Stamps a spawn egg with a tribe (for the creative tab / structures). */
    public static ItemStack forTribe(KinRole role, TribeDefinition tribe) {
        return TribeDefinition.stamp(new ItemStack(TribeRegistry.EGGS.get(role).get()), tribe);
    }

    public static EntityType<?> type() { return TribeRegistry.TRIBAL_KIN.get(); }
}
