package tk.darrow.tribalpower.song;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import tk.darrow.tribalpower.entity.CreatureProfile;

/**
 * One slot per reagent, two counts in that slot. The pouch never holds anything else. Raw stays
 * available for hearths and trades. Empowered is what a sheet or a verse arrow spends.
 */
public final class ReagentPouch {
    public static final int CAP = 999;
    private static final String RAW = "Raw";
    private static final String EMPOWERED = "Empowered";

    private ReagentPouch() {}

    public static int raw(ItemStack pouch, CreatureProfile profile) {
        return count(pouch, RAW, profile);
    }

    public static int empowered(ItemStack pouch, CreatureProfile profile) {
        return count(pouch, EMPOWERED, profile);
    }

    public static int addRaw(ItemStack pouch, CreatureProfile profile, int amount) {
        return add(pouch, RAW, profile, amount);
    }

    public static int addEmpowered(ItemStack pouch, CreatureProfile profile, int amount) {
        return add(pouch, EMPOWERED, profile, amount);
    }

    /** Moves raw into empowered, up to {@code amount}. Returns how many moved. */
    public static int empower(ItemStack pouch, CreatureProfile profile, int amount) {
        if (amount <= 0) return 0;
        int moved = Math.min(amount, raw(pouch, profile));
        int room = CAP - empowered(pouch, profile);
        moved = Math.min(moved, room);
        if (moved <= 0) return 0;
        set(pouch, RAW, profile, raw(pouch, profile) - moved);
        set(pouch, EMPOWERED, profile, empowered(pouch, profile) + moved);
        return moved;
    }

    public static boolean consumeEmpowered(ItemStack pouch, CreatureProfile profile, int amount) {
        if (amount <= 0) return true;
        if (empowered(pouch, profile) < amount) return false;
        set(pouch, EMPOWERED, profile, empowered(pouch, profile) - amount);
        return true;
    }

    /** Pulls raw back out, up to {@code amount}. */
    public static int takeRaw(ItemStack pouch, CreatureProfile profile, int amount) {
        int moved = Math.min(Math.max(amount, 0), raw(pouch, profile));
        if (moved > 0) set(pouch, RAW, profile, raw(pouch, profile) - moved);
        return moved;
    }

    public static int kinds(ItemStack pouch) {
        int kinds = 0;
        for (CreatureProfile profile : CreatureProfile.values()) {
            if (raw(pouch, profile) > 0 || empowered(pouch, profile) > 0) kinds++;
        }
        return kinds;
    }

    private static int count(ItemStack pouch, String side, CreatureProfile profile) {
        CompoundTag tag = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag sideTag = tag.getCompound(side);
        return Math.clamp(sideTag.getInt(profile.reagent), 0, CAP);
    }

    private static int add(ItemStack pouch, String side, CreatureProfile profile, int amount) {
        if (amount <= 0) return 0;
        int have = count(pouch, side, profile);
        int moved = Math.min(amount, CAP - have);
        if (moved > 0) set(pouch, side, profile, have + moved);
        return moved;
    }

    private static void set(ItemStack pouch, String side, CreatureProfile profile, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, pouch, tag -> {
            CompoundTag sideTag = tag.getCompound(side);
            if (value <= 0) sideTag.remove(profile.reagent);
            else sideTag.putInt(profile.reagent, Math.min(value, CAP));
            if (sideTag.isEmpty()) tag.remove(side);
            else tag.put(side, sideTag);
        });
    }
}
