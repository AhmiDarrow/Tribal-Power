package tk.darrow.tribalpower.integration;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianRegistry;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.rite.world.WorldRite;
import tk.darrow.tribalpower.rite.world.WorldRiteRegistry;
import tk.darrow.tribalpower.song.Anointment;
import tk.darrow.tribalpower.song.Reagents;

/** What the recipe viewers show for the things that are not recipes: rites, guardian calls and anointing. One source for JEI and EMI. */
public final class CompatDisplays {
    private CompatDisplays() {}

    /** A world rite: its tablet, the seal its circle wants, and the Pulse it draws. */
    public record Rite(WorldRite rite, ItemStack tablet, ItemStack seal, int cost) {}
    /** A guardian's call: the altar, the reagents laid on it, and what rises. */
    public record Call(Guardian guardian, ItemStack altar, ItemStack reagents, ItemStack rises) {}
    /** An anointment: any reagent of its Note, worked into an anointable weapon. */
    public record Anoint(Anointment anointment, List<ItemStack> reagents, List<ItemStack> weapons) {}

    public static ItemStack seal(Attunement element) {
        return new ItemStack(switch (element) {
            case EARTH -> ModItems.EARTH_SEAL.get();
            case FIRE -> ModItems.FIRE_SEAL.get();
            case WATER -> ModItems.WATER_SEAL.get();
            case AIR -> ModItems.AIR_SEAL.get();
            case SPIRIT -> ModItems.SPIRIT_SEAL.get();
            case LOOM -> ModItems.LOOM_SEAL.get();
        });
    }

    public static List<Rite> rites() {
        List<Rite> out = new ArrayList<>();
        for (WorldRite rite : WorldRite.values())
            out.add(new Rite(rite, new ItemStack(WorldRiteRegistry.TABLETS.get(rite).get()), seal(rite.element()), rite.cost()));
        return out;
    }

    public static List<Call> calls() {
        List<Call> out = new ArrayList<>();
        for (Guardian guardian : Guardian.values())
            out.add(new Call(guardian, new ItemStack(GuardianRegistry.ALTAR_ITEM.get()), new ItemStack(guardian.callItem(), TribalConfig.guardianCallCost()),
                    new ItemStack(GuardianRegistry.EGGS.get(guardian).get())));
        return out;
    }

    public static List<Anoint> anointments() {
        List<ItemStack> weapons = new ArrayList<>();
        weapons.add(new ItemStack(ModItems.SPIRITGEAR_BLADE.get()));
        ModItems.SPIRITGEAR_WEAPONS.values().forEach(w -> weapons.add(new ItemStack(w.get())));
        List<Anoint> out = new ArrayList<>();
        for (Anointment anointment : Anointment.values()) {
            var profiles = Reagents.byNote().getOrDefault(anointment.note, List.of());
            List<ItemStack> reagents = new ArrayList<>();
            for (var profile : profiles) reagents.add(new ItemStack(Reagents.item(profile), TribalConfig.anointReagentCost()));
            out.add(new Anoint(anointment, reagents, weapons));
        }
        return out;
    }

    public static String anointmentKey(Anointment anointment) { return "anointment.tribalpower." + anointment.name().toLowerCase(java.util.Locale.ROOT); }
}
