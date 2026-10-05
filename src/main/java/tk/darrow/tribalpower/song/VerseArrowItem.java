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

/**
 * One to three empowered reagents, sung at a totem, become four of these. The Pulse Bow and Crossbow spend them. The
 * arrow carries its short verse: each copy of the lead reagent adds a point of power, any other reagent rides along.
 */
public class VerseArrowItem extends Item {
    private static final String REAGENT = "Reagent";
    /** Most reagents one arrow carries: the lead up to three times, or the lead with up to two riders. */
    public static final int MAX_REAGENTS = 3;

    public VerseArrowItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(CreatureProfile profile, Attunement voice, int count) {
        return create(new SongVerse(List.of(profile.reagent), voice), count);
    }

    /**
     * Arrows carrying a short verse. A one-reagent verse is written the way every arrow used to be, so it stacks with
     * arrows fletched before; a longer one also lists its reagents in order, which is what sets its power and riders.
     */
    public static ItemStack create(SongVerse verse, int count) {
        ItemStack stack = new ItemStack(tk.darrow.tribalpower.item.ModItems.VERSE_ARROW.get(), count);
        CompoundTag tag = new CompoundTag();
        tag.putString(REAGENT, verse.reagents().get(0));
        if (verse.reagents().size() > 1) verse.write(tag);
        else tag.putString(SongVerse.VOICE, verse.voice().getSerializedName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static @Nullable SongVerse verse(ItemStack stack) {
        if (!(stack.getItem() instanceof VerseArrowItem)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        SongVerse fletched = SongVerse.read(tag);
        if (fletched != null) {
            return fletched.reagents().size() <= MAX_REAGENTS ? fletched
                    : new SongVerse(fletched.reagents().subList(0, MAX_REAGENTS), fletched.voice());
        }
        // a one-reagent arrow, and every arrow fletched before arrows carried more, sings its reagent once: power 1
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
        if (verse.reagents().size() > 1) lines.add(verse.ingredients());
        lines.add(Component.translatable("item.tribalpower.verse_arrow.power", verse.power(), bonus(SonicBolt.verseBonus(verse))));
    }

    /** The damage a verse adds, as a tooltip says it: a whole number when it is one. */
    public static String bonus(double amount) {
        long whole = Math.round(amount);
        return Math.abs(amount - whole) < 1.0E-6 ? String.valueOf(whole) : String.format(java.util.Locale.ROOT, "%.1f", amount);
    }
}
