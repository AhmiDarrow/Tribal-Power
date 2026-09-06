package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Spiritgear pickaxe — Pulse from inventory Pulse Cells preserves durability while mining.
 */
public class SpiritgearPickaxeItem extends PickaxeItem {
    public SpiritgearPickaxeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(PickaxeItem.createAttributes(Tiers.IRON, 1.0F, -2.8F)));
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
    }
}
