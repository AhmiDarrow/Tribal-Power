package tk.darrow.tribalpower.song;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.item.GearCell;
import tk.darrow.tribalpower.item.SpiritgearHelper;

/**
 * The caster. Right-click sings the open page. Crouch and right-click turns the page. Pulse comes
 * from a cell seated in the book, then from cells in the inventory.
 */
public class SongbookItem extends Item {
    private final SongbookTier tier;

    public SongbookItem(SongbookTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public SongbookTier tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(book);
        if (player.isShiftKeyDown()) {
            SongPages.cycle(book);
            SongVerse open = SongPages.selected(book);
            int count = SongPages.pages(book).size();
            player.displayClientMessage(open == null
                    ? Component.translatable("message.tribalpower.songbook.empty")
                    : Component.translatable("message.tribalpower.songbook.page", SongPages.open(book) + 1, count, open.name()), true);
            return InteractionResultHolder.consume(book);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(book);
        SongVerse verse = SongPages.selected(book);
        if (verse == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.songbook.empty"), true);
            return InteractionResultHolder.fail(book);
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return InteractionResultHolder.fail(book);
        if ((verse.shape() == SongShape.BOLT || verse.shape() == SongShape.BIND) && SongCast.look(server, verse.reach()) == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.no_target"), true);
            return InteractionResultHolder.fail(book);
        }
        int cost = SongVerse.castCost(player, verse);
        if (!GearCell.spend(player, book, cost)) {
            SpiritgearHelper.notifyStarved(player);
            return InteractionResultHolder.fail(book);
        }
        if (!SongCast.play(server, verse)) {
            GearCell.refund(player, book, cost);
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.no_target"), true);
            return InteractionResultHolder.fail(book);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(book);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.songbook.desc", tier.pages, tier.longest));
        SongVerse open = SongPages.selected(stack);
        int count = SongPages.pages(stack).size();
        if (open == null) lines.add(Component.translatable("message.tribalpower.songbook.empty"));
        else lines.add(Component.translatable("message.tribalpower.songbook.page", SongPages.open(stack) + 1, count, open.name()));
    }
}
