package tk.darrow.tribalpower.song;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.item.GearCell;
import tk.darrow.tribalpower.item.SpiritgearHelper;

/**
 * A bow that sings. A full draw spends Pulse and throws a sonic bolt. A verse arrow in the inventory
 * rides that bolt and is spent. With no arrow, the bolt is only the note.
 */
public class PulseBowItem extends Item {
    public static final int PLAIN_PULSE = 6;
    public static final int VERSE_PULSE = 6;

    public PulseBowItem(Properties properties) {
        super(properties.stacksTo(1).durability(384));
    }

    public static int cost(float pull, boolean verse) {
        if (pull < 0.1F) return 0;
        int scaled = Math.max(1, Math.round(PLAIN_PULSE * pull));
        return verse ? scaled + VERSE_PULSE : scaled;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bow = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        float pull = BowItem.getPowerForTime(getUseDuration(bow, entity) - timeLeft);
        if (pull < 0.1F) return;
        ItemStack arrow = findVerse(player);
        int price = cost(pull, arrow != null);
        if (level.isClientSide) return;
        if (!GearCell.spend(player, bow, price)) {
            SpiritgearHelper.notifyStarved(player);
            return;
        }
        // Read the verse before spending the arrow: the last one in a stack is empty afterwards.
        SongVerse verse = arrow == null ? null : VerseArrowItem.verse(arrow);
        if (arrow != null) takeVerse(player, arrow);
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            SonicBolt.shoot(server, bow, verse, pull);
        }
        bow.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.3F + pull * 0.3F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /** Shrinks one verse arrow. The last one leaves the slot, so a later draw cannot sing it again. */
    public static void takeVerse(Player player, ItemStack arrow) {
        if (player.getAbilities().instabuild) return;
        arrow.shrink(1);
        if (arrow.isEmpty()) player.getInventory().removeItem(arrow);
    }

    /** The first verse arrow in the inventory, hotbar included. */
    public static @Nullable ItemStack findVerse(Player player) {
        Predicate<ItemStack> verse = stack -> !stack.isEmpty()
                && stack.getItem() instanceof VerseArrowItem && VerseArrowItem.verse(stack) != null;
        for (ItemStack stack : player.getInventory().items) {
            if (verse.test(stack)) return stack;
        }
        ItemStack off = player.getOffhandItem();
        return verse.test(off) ? off : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.pulse_bow.desc", PLAIN_PULSE, PLAIN_PULSE + VERSE_PULSE));
    }
}
