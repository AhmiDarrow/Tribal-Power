package tk.darrow.tribalpower.song;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * A page that is not in a book yet. Paper, one chalk mark, and three to seven empowered reagents. Sing it once from
 * the hand -- the sheet is spent -- or bind it into a songbook at the Song Bench to keep it.
 */
public class SongSheetItem extends Item {
    public SongSheetItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(SongVerse verse) {
        ItemStack stack = new ItemStack(tk.darrow.tribalpower.item.ModItems.SONG_SHEET.get());
        CompoundTag tag = new CompoundTag();
        verse.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static @Nullable SongVerse verse(ItemStack stack) {
        if (!(stack.getItem() instanceof SongSheetItem)) return null;
        return SongVerse.read(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.InteractionHand hand) {
        ItemStack sheet = player.getItemInHand(hand);
        SongVerse verse = verse(sheet);
        if (verse == null || level.isClientSide || !(player instanceof net.minecraft.server.level.ServerPlayer server))
            return net.minecraft.world.InteractionResultHolder.pass(sheet);
        if (tk.darrow.tribalpower.effect.ModEffects.hushed(player)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.hushed"), true);
            return net.minecraft.world.InteractionResultHolder.fail(sheet);
        }
        if ((verse.shape() == SongShape.BOLT || verse.shape() == SongShape.BIND) && SongCast.look(server, verse.reach()) == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.song.no_target"), true);
            return net.minecraft.world.InteractionResultHolder.fail(sheet);
        }
        int cost = SongVerse.castCost(player, verse);
        if (!player.getAbilities().instabuild && !tk.darrow.tribalpower.item.GearCell.spend(player, sheet, cost)) {
            tk.darrow.tribalpower.item.SpiritgearHelper.notifyStarved(player);
            return net.minecraft.world.InteractionResultHolder.fail(sheet);
        }
        if (!SongCast.play(server, verse)) {
            if (!player.getAbilities().instabuild) tk.darrow.tribalpower.item.GearCell.refund(player, sheet, cost);
            player.displayClientMessage(Component.translatable("message.tribalpower.song.no_target"), true);
            return net.minecraft.world.InteractionResultHolder.fail(sheet);
        }
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.1F);
        sheet.consume(1, player);
        return net.minecraft.world.InteractionResultHolder.consume(sheet);
    }

    @Override
    public Component getName(ItemStack stack) {
        SongVerse verse = verse(stack);
        return verse == null ? super.getName(stack) : Component.translatable("item.tribalpower.song_sheet.named", verse.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        SongVerse verse = verse(stack);
        if (verse == null) {
            lines.add(Component.translatable("item.tribalpower.song_sheet.blank"));
            return;
        }
        lines.add(verse.ingredients());
        lines.add(Component.translatable("item.tribalpower.song_sheet.cost", verse.castPulse()));
        lines.add(Component.translatable("item.tribalpower.song_sheet.use").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
