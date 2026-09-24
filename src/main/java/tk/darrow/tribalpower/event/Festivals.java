package tk.darrow.tribalpower.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.item.SpiritgearHelper;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHooks;
import tk.darrow.tribalpower.tribe.TribeStanding;

/**
 * Each tribe's festival: one day in a cycle, the tribes taking turns. The camp lights up, the Kin gather at the
 * hearth, the Elder has festival words and a gift, and joining in (the Elder's feast, a rite in the camp, a
 * feast eaten by the hearth) pays standing well beyond an ordinary day.
 */
public final class Festivals {
    private Festivals() {}

    public static long day(Level level) {
        return (level.isClientSide ? level.getDayTime() : level.getServer().overworld().getDayTime()) / 24000L;
    }

    /** Whose festival a day is, or null: the cycle is split into nine slots and the first day of each is the festival. */
    public static TribeDefinition tribeOn(long day) {
        if (!TribalConfig.festivalsEnabled()) return null;
        int tribes = TribeDefinition.values().length;
        int cycle = Math.max(tribes, TribalConfig.festivalCycleDays());
        int slot = (cycle + tribes - 1) / tribes;
        long inCycle = Math.floorMod(day, cycle);
        if (inCycle % slot != 0) return null;
        int index = (int) (inCycle / slot);
        return index < tribes ? TribeDefinition.values()[index] : null;
    }

    /** Whose festival it is today, or null. */
    public static TribeDefinition today(Level level) {
        return level.isClientSide ? MarchStatePayload.latest.festival() : tribeOn(day(level));
    }

    public static boolean active(TribeDefinition tribe, Level level) { return today(level) == tribe; }

    /** The Elder's gift: standing and two of the tribe's dish, once per festival. */
    public static boolean join(ServerPlayer player, TribeDefinition tribe, long day) {
        if (tribeOn(day) != tribe) return false;
        MarchEventsSavedData data = MarchEventsSavedData.get(player.server);
        if (!data.join(player.getUUID(), tribe.ordinal(), day)) return false;
        TribeStanding.add(player, tribe, TribalConfig.festivalStanding());
        SpiritgearHelper.give(player, new ItemStack(CuisineRegistry.dish(tribe), 2));
        player.displayClientMessage(Component.translatable("message.tribalpower.festival.joined", tribe.displayNameComponent()).withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    public static boolean joined(ServerPlayer player, TribeDefinition tribe, long day) {
        return MarchEventsSavedData.get(player.server).joined(player.getUUID(), tribe.ordinal(), day);
    }

    /** A rite performed in a camp on its festival day: the festival's thanks on top of the rite's. */
    public static void riteBonus(ServerPlayer player, TribeDefinition tribe) {
        if (!active(tribe, player.level())) return;
        TribeStanding.add(player, tribe, TribalConfig.festivalRiteStanding());
        player.displayClientMessage(Component.translatable("message.tribalpower.festival.rite", tribe.displayNameComponent()).withStyle(ChatFormatting.GOLD), true);
    }

    /** A feast eaten within a festival camp: the tribe counts you at its table. */
    public static void feastBonus(ServerPlayer player, BlockPos at) {
        ServerLevel level = player.serverLevel();
        for (TribeDefinition tribe : TribeHooks.hearthsNear(level, at, 24)) {
            if (!active(tribe, level)) continue;
            // once per festival: the feast key sits above the join keys in the same record
            if (!MarchEventsSavedData.get(player.server).join(player.getUUID(), tribe.ordinal() + 16, day(level))) continue;
            TribeStanding.add(player, tribe, TribalConfig.festivalFeastStanding());
            player.displayClientMessage(Component.translatable("message.tribalpower.festival.feast", tribe.displayNameComponent()).withStyle(ChatFormatting.GOLD), true);
        }
    }
}
