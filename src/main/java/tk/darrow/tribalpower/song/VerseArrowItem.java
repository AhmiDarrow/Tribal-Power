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
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.entity.CreatureProfile;

/** One empowered reagent, sung at a totem, becomes four of these. The Pulse Bow spends them. */
public class VerseArrowItem extends Item {
    private static final String REAGENT = "Reagent";

    public VerseArrowItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(CreatureProfile profile, Attunement voice, int count) {
        ItemStack stack = new ItemStack(tk.darrow.tribalpower.item.ModItems.VERSE_ARROW.get(), count);
        CompoundTag tag = new CompoundTag();
        tag.putString(REAGENT, profile.reagent);
        tag.putString(SongVerse.VOICE, voice.getSerializedName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static @Nullable SongVerse verse(ItemStack stack) {
        if (!(stack.getItem() instanceof VerseArrowItem)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CreatureProfile profile = Reagents.byId(tag.getString(REAGENT));
        if (profile == null) return null;
        return new SongVerse(List.of(profile.reagent), Attunement.byName(tag.getString(SongVerse.VOICE)));
    }

    @Override
    public Component getName(ItemStack stack) {
        SongVerse verse = verse(stack);
        if (verse == null || verse.leadProfile() == null) return super.getName(stack);
        return Component.translatable("item.tribalpower.verse_arrow.named",
                Component.translatable("item.tribalpower." + verse.leadProfile().reagent));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        SongVerse verse = verse(stack);
        if (verse == null) return;
        lines.add(Component.translatable(verse.shape().key()));
        lines.add(Component.translatable("attunement.tribalpower." + verse.voice().getSerializedName()));
    }
}
