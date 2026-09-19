package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/** Spiritgear shovel — Pulse spares digs and paths; a linked totem voice adds a ground perk. */
public class SpiritgearShovelItem extends ShovelItem {
    public SpiritgearShovelItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(ShovelItem.createAttributes(Tiers.DIAMOND, 1.5F, -3.0F))
                .durability(SpiritGear.TOOL_DURABILITY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return SpiritGear.destroySpeed(stack, super.getDestroySpeed(stack, state));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.getAbilities().instabuild
                || state.getDestroySpeed(level, pos) == 0.0F) {
            return super.mineBlock(stack, level, state, pos, entity);
        }
        SpiritGear.Swing parent = SpiritGear.SWING.get();
        boolean aoe = parent != null && parent.aoe();
        boolean paid = parent != null ? parent.pulsePaid() : SpiritGear.consumeForMine(player, stack);
        if (parent == null) SpiritGear.beginSwing(player, stack, paid, false);
        boolean ok = super.mineBlock(stack, level, state, pos, entity);
        SpiritGear.finishDurability(player, stack, paid);
        if (ok && paid && !aoe && SpiritGear.voice(stack).orElse(null) == Attunement.EARTH
                && SpiritGearHooks.dirtLike(state)) {
            Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
            SpiritGearHooks.aoe(player, stack, pos, axis, SpiritGearHooks::dirtLike);
        }
        return ok;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && player instanceof ServerPlayer server && !server.level().isClientSide) {
            ItemStack stack = context.getItemInHand();
            boolean paid = server.getAbilities().instabuild
                    || GearCell.spend(server, stack, SpiritGear.useCost(stack));
            if (!server.getAbilities().instabuild) {
                if (paid) stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                else {
                    if (!SpiritGear.skipStarveHurt(stack)) stack.hurtAndBreak(1, server, EquipmentSlot.MAINHAND);
                    SpiritgearHelper.notifyStarved(server);
                }
            }
            if (paid && SpiritGear.voice(stack).orElse(null) == Attunement.AIR) {
                BlockPos center = context.getClickedPos();
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    UseOnContext neighbour = new UseOnContext(server, context.getHand(),
                            new net.minecraft.world.phys.BlockHitResult(
                                    context.getClickLocation().add(x, 0, z),
                                    context.getClickedFace(), center.offset(x, 0, z), false));
                    super.useOn(neighbour);
                }
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
