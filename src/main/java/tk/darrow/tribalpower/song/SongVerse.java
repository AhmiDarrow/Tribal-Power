package tk.darrow.tribalpower.song;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.entity.CreatureProfile;

/**
 * One written song. The first reagent picks the shape, the totem picks the voice, and each later
 * reagent only dresses that shape: the same note again hits harder, a new note adds one rider.
 */
/** See {@link #castCost}: a Spindle boon sings cheaper. */
public record SongVerse(List<String> reagents, Attunement voice) {
    public static final int MIN_SHEET = 3;
    public static final int MAX_SHEET = 7;
    public static final String REAGENTS = "Reagents";
    public static final String VOICE = "Voice";

    public SongVerse {
        reagents = List.copyOf(reagents);
    }

    public static @Nullable SongVerse read(CompoundTag tag) {
        if (!tag.contains(REAGENTS, Tag.TAG_LIST)) return null;
        ListTag list = tag.getList(REAGENTS, Tag.TAG_STRING);
        List<String> ids = new ArrayList<>(list.size());
        for (int i = 0; i < list.size() && i < MAX_SHEET; i++) {
            String id = list.getString(i);
            if (Reagents.byId(id) != null) ids.add(id);
        }
        if (ids.isEmpty()) return null;
        return new SongVerse(ids, Attunement.byName(tag.getString(VOICE)));
    }

    public void write(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String id : reagents) list.add(StringTag.valueOf(id));
        tag.put(REAGENTS, list);
        tag.putString(VOICE, voice.getSerializedName());
    }

    public boolean sheetLength() {
        return reagents.size() >= MIN_SHEET && reagents.size() <= MAX_SHEET;
    }

    public @Nullable CreatureProfile leadProfile() {
        return reagents.isEmpty() ? null : Reagents.byId(reagents.get(0));
    }

    public Note lead() {
        CreatureProfile profile = leadProfile();
        return profile == null ? Note.QUIET : Note.of(profile);
    }

    public SongShape shape() {
        return lead().shape();
    }

    /** How many times the lead reagent is seated. Repeating it is what makes the song hit harder. */
    public int power() {
        if (reagents.isEmpty()) return 1;
        String lead = reagents.get(0);
        int copies = 0;
        for (String id : reagents) if (id.equals(lead)) copies++;
        return Math.max(1, copies);
    }

    /** Up to two notes that are not the lead. Anything past that only lengthens the song. */
    public List<Note> riders() {
        List<Note> riders = new ArrayList<>(2);
        Note lead = lead();
        for (int i = 1; i < reagents.size() && riders.size() < 2; i++) {
            CreatureProfile profile = Reagents.byId(reagents.get(i));
            if (profile == null) continue;
            Note note = Note.of(profile);
            if (note != lead && !riders.contains(note)) riders.add(note);
        }
        return riders;
    }

    public int duration() {
        int extra = Math.max(0, reagents.size() - MIN_SHEET);
        return 80 + extra * 20 + (power() - 1) * 10;
    }

    public double reach() {
        return 10.0 + Math.max(0, reagents.size() - MIN_SHEET);
    }

    /** Pulse a cast spends. A longer sheet costs more. The bow has its own price. */
    /** What a song costs this singer: the Spindle boon's Loom-stitchers sing cheaper. */
    public static int castCost(net.minecraft.world.entity.player.Player player, SongVerse verse) {
        int cost = verse.castPulse();
        if (tk.darrow.tribalpower.effect.ModEffects.hasBoon(player, tk.darrow.tribalpower.tribe.TribeDefinition.SPINDLE))
            cost = Math.max(1, (int) Math.round(cost * (1 - tk.darrow.tribalpower.config.TribalConfig.spindleBoonDiscount())));
        return cost;
    }

    public int castPulse() {
        return 8 + 4 * Math.max(1, reagents.size());
    }

    public Component name() {
        return Component.translatable(shape().key()).append(Component.literal(" · "))
                .append(Component.translatable("attunement.tribalpower." + voice.getSerializedName()));
    }

    public Component ingredients() {
        var line = Component.empty();
        for (int i = 0; i < reagents.size(); i++) {
            if (i > 0) line.append(", ");
            CreatureProfile profile = Reagents.byId(reagents.get(i));
            line.append(profile == null
                    ? Component.literal(reagents.get(i))
                    : Component.translatable("item.tribalpower." + profile.reagent));
        }
        return line;
    }
}
