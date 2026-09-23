package tk.darrow.tribalpower.song;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;

/** A collection tray. On the hotbar, reagents picked up off the ground land here instead of the inventory. */
public class ReagentPouchItem extends Item {
    public ReagentPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int slot = hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
            player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new PouchMenu(id, inventory, slot),
                    Component.translatable("item.tribalpower.reagent_pouch")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.reagent_pouch.desc", ReagentPouch.kinds(stack)));
    }
}
