package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.guide.GuideBookFactory;

import java.util.List;

/**
 * Spirit Codex — in-game guidebook for shamanic technomancy systems.
 * Opens a multi-page written book generated from guide JSON chapters.
 */
public class SpiritCodexItem extends Item {
    public SpiritCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack book = GuideBookFactory.createBook();
            serverPlayer.openItemGui(book, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spirit_codex.desc"));
    }
}
