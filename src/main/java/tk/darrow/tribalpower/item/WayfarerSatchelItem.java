package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import java.util.List;

/** A key to the player's saved vault, never an item-contained inventory that can be duplicated. */
public class WayfarerSatchelItem extends Item {
    public WayfarerSatchelItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!DeepCacheManager.tryAuthorize(serverPlayer)) {
                player.displayClientMessage(Component.translatable("message.tribalpower.deep_cache.need_pulse"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            var container = DeepCacheManager.openContainer(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.sixRows(id, inv, container),
                    Component.translatable("block.tribalpower.deep_cache")));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.wayfarer_satchel.desc"));
    }
}
