package tk.darrow.tribalpower.item;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import java.util.List;

public class WaystoneCompassItem extends Item {
    private final int tier;
    public WaystoneCompassItem(Properties properties, int tier) { super(properties); this.tier = tier; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            BlockPos floor = context.getClickedPos();
            if (context.getClickedFace() != Direction.UP || !context.getLevel().getBlockState(floor).isFaceSturdy(context.getLevel(), floor, Direction.UP)) return InteractionResult.FAIL;
            CustomData.update(DataComponents.CUSTOM_DATA, context.getItemInHand(), tag -> {
                tag.putLong("Waypoint", floor.above().asLong()); tag.putString("Dimension", context.getLevel().dimension().location().toString());
            });
            player.displayClientMessage(Component.translatable("message.tribalpower.waystone.bound"), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        if (player.isShiftKeyDown() || player.isPassenger() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation dim = ResourceLocation.tryParse(data.getString("Dimension"));
        var target = dim == null ? null : serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (!data.contains("Waypoint") || target == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.waystone.unbound"), true); return InteractionResultHolder.fail(stack);
        }
        BlockPos feet = BlockPos.of(data.getLong("Waypoint"));
        if ((tier < 3 && target != level) || (tier == 1 && player.blockPosition().distSqr(feet) > 128*128)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.waystone.range"), true); return InteractionResultHolder.fail(stack);
        }
        // Reject invalid coordinates before requesting any destination chunks.
        if (!tk.darrow.tribalpower.world.TravelSafety.withinBounds(target, feet)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.waystone.blocked"), true);
            return InteractionResultHolder.fail(stack);
        }
        // A player explicitly invoking travel may load a destination; automation never does.
        target.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
        Vec3 landing = Vec3.atBottomCenterOf(feet);
        AABB bounds = player.getBoundingBox().move(landing.subtract(player.position()));
        boolean hazard = tk.darrow.tribalpower.world.TravelSafety.hasHazard(target, feet);
        if (!target.getBlockState(feet.below()).isFaceSturdy(target, feet.below(), Direction.UP)
                || hazard || target.hasNeighborSignal(feet.below()) || !target.noCollision(bounds) || !target.getFluidState(feet).isEmpty() || !target.getFluidState(feet.above()).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.waystone.blocked"), true); return InteractionResultHolder.fail(stack);
        }
        var charge = SpiritgearHelper.reservePulse(player, tier == 1 ? 20 : tier == 2 ? 40 : 100);
        if (charge == null) { SpiritgearHelper.notifyStarved(player); return InteractionResultHolder.fail(stack); }
        if (serverPlayer.changeDimension(new DimensionTransition(target, landing, Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING)) == null) {
            charge.refund();
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.fallDistance = 0; player.getCooldowns().addCooldown(this, 100);
        tk.darrow.tribalpower.effect.SpiritEffects.ring(target, landing.add(0,0.2,0), tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT, 1, 20);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) { lines.add(Component.translatable("item.tribalpower.waystone_compass.desc")); }
}
