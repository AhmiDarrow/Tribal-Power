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

/** A page that is not in a book yet. Paper, one chalk mark, and three to seven empowered reagents. */
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
    }
}
