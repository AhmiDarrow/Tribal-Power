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
        TribalKinEntity kin = TribeRegistry.TRIBAL_KIN.get().spawn((net.minecraft.server.level.ServerLevel) level, spawnPos, MobSpawnType.SPAWN_EGG);
        if (kin != null) {
            kin.setRole(role);
            kin.setTribe(TribeDefinition.ofOrDefault(stack));
            kin.setAnchor(spawnPos);
            if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) stack.shrink(1);
        }
        return InteractionResult.CONSUME;
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
