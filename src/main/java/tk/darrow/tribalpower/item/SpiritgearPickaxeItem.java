package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Spiritgear tools consume durability as a stand-in for Pulse drain until full gear pulse is wired.
 */
public class SpiritgearPickaxeItem extends PickaxeItem {
    public SpiritgearPickaxeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(PickaxeItem.createAttributes(Tiers.IRON, 1.0F, -2.8F)));
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, BlockState state,
                             net.minecraft.core.BlockPos pos, LivingEntity entity) {
        boolean ok = super.mineBlock(stack, level, state, pos, entity);
        if (ok && !level.isClientSide && entity instanceof Player player && !player.getAbilities().instabuild) {
            // Extra pulse-flavored wear: already handled by durability; message occasionally.
            if (level.random.nextInt(20) == 0) {
                player.displayClientMessage(Component.translatable("message.tribalpower.spiritgear.pulse"), true);
            }
        }
        return ok;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
    }
}
