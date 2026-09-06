package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Spiritgear axe — Pulse preserves durability on chops and stripping.
 */
public class SpiritgearAxeItem extends AxeItem {
    public SpiritgearAxeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(AxeItem.createAttributes(Tiers.IRON, 6.0F, -3.1F)));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean ok = super.mineBlock(stack, level, state, pos, entity);
        if (ok && !level.isClientSide && entity instanceof Player player && !player.getAbilities().instabuild
                && state.getDestroySpeed(level, pos) != 0.0F) {
            if (SpiritgearHelper.tryConsumePulse(player, SpiritgearHelper.MINE_COST)) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                SpiritgearHelper.notifyFueled(player);
            } else {
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                SpiritgearHelper.notifyStarved(player);
            }
        }
        return ok;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && player != null && !player.level().isClientSide && !player.getAbilities().instabuild) {
            ItemStack stack = context.getItemInHand();
            if (SpiritgearHelper.tryConsumePulse(player, SpiritgearHelper.USE_COST)) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            } else {
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                SpiritgearHelper.notifyStarved(player);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
    }
}
