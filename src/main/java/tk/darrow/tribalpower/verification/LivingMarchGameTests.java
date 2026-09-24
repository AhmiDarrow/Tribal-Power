package tk.darrow.tribalpower.verification;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.AfflictionEffect;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.event.Festivals;
import tk.darrow.tribalpower.event.LeySurges;
import tk.darrow.tribalpower.event.MarchEvents;
import tk.darrow.tribalpower.event.MarchEventsSavedData;
import tk.darrow.tribalpower.event.MarchStatePayload;
import tk.darrow.tribalpower.event.MarchWeather;
import tk.darrow.tribalpower.event.WanderingSpiritEntity;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeStanding;

/** The living March: the scheduler, weather's reach, surges and their sickness, festivals, wandering spirits. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class LivingMarchGameTests {
    private LivingMarchGameTests() {}

    @GameTest(template = "empty")
    public static void theSchedulerStartsEndsAndRemembersWeather(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MarchEventsSavedData data = MarchEventsSavedData.get(level.getServer());
        long now = level.getGameTime();
        data.setWeather(MarchWeather.ASHFALL, now + 400);
        h.assertTrue(data.weatherActive(MarchWeather.ASHFALL, now), "Set weather runs");
        h.assertTrue(MarchStatePayload.of(level).weather(MarchWeather.ASHFALL), "The payload carries it");
        h.assertFalse(MarchStatePayload.of(level).weather(MarchWeather.WHITEOUT), "Only what runs is carried");
        h.assertFalse(data.expireWeather(now + 100), "Nothing runs out early");
        h.assertTrue(data.expireWeather(now + 400), "It runs out on time");
        h.assertFalse(data.weatherActive(MarchWeather.ASHFALL, now + 400), "And is gone");
        // The state survives a save.
        data.setWeather(MarchWeather.FEN_MIST, now + 900);
        data.setSurge(Attunement.WATER, now + 300);
        var saved = data.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        var loaded = MarchEventsSavedData.load(saved, level.registryAccess());
        h.assertTrue(loaded.weatherActive(MarchWeather.FEN_MIST, now) && loaded.surgeVoice(now) == Attunement.WATER, "Weather and surge survive a save");
        data.setWeather(MarchWeather.FEN_MIST, 0);
        data.setSurge(null, 0);
        // The scheduler's own step runs without a player and rolls a surge when it is due.
        data.setLastSurgeEnd(now - TribalConfig.surgeEveryMinutes() * 1200L - 1);
        MarchEvents.tick(level, now);
        long warning = TribalConfig.eventWarningSeconds() * 20L;
        h.assertTrue(warning == 0 ? data.surgeVoice(now) != null : data.surgePending(now) != null && data.surgeVoice(now) == null, "A due surge is announced first");
        h.assertTrue(data.surgeVoice(now + warning) != null, "And runs once the warning is over");
        h.assertTrue(data.expireSurge(data.surgeUntil()), "And ends when its time is up");
        data.setSurge(null, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void weatherFavoursOneVoiceAndHampersAnother(GameTestHelper h) {
        for (MarchWeather weather : MarchWeather.values()) {
            h.assertTrue(weather.generatorScale(weather.favours) == TribalConfig.weatherGeneratorBonus(), weather + " favours " + weather.favours);
            h.assertTrue(weather.generatorScale(weather.hampers) == TribalConfig.weatherGeneratorPenalty(), weather + " hampers " + weather.hampers);
            h.assertTrue(weather.generatorScale(Attunement.LOOM) == 1.0, weather + " leaves the Loom alone");
            h.assertTrue(MarchWeather.forBiome(weather.biome) == weather, "Each weather knows its biome");
        }
        // Outside the March the sky is always plain.
        h.assertTrue(MarchWeather.at(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1))) == null, "No March weather in the Overworld");
        h.assertTrue(MarchWeather.generatorScale(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), Attunement.FIRE) == 1.0, "Generators here are untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aSurgeRaisesTheYieldOfItsVoiceAndSickensBareCrossings(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        ServerLevel level = h.getLevel();
        MarchEventsSavedData data = MarchEventsSavedData.get(level.getServer());
        try {
            long now = level.getGameTime();
            player.getAbilities().invulnerable = false; // the mock player is untouchable by default
            data.setSurge(Attunement.FIRE, now + 6000);
            var fire = new LeyField.Reading(1, 2, 1 << Attunement.FIRE.ordinal());
            var water = new LeyField.Reading(1, 2, 1 << Attunement.WATER.ordinal());
            h.assertTrue(LeySurges.multiplier(level, fire) == TribalConfig.surgeYieldMultiplier(), "A collector on the surging voice yields more");
            h.assertTrue(LeySurges.multiplier(level, water) == 1.0, "Other voices are unchanged");
            h.assertTrue(LeySurges.multiplier(level, LeyField.Reading.QUIET) == 1.0, "No thread, no surge");
            // Standing on the cell's nexus with no collector near: sick. Anywhere else: fine.
            BlockPos nexus = LeySurges.nexusOf(level, h.absolutePos(new BlockPos(1, 1, 1)));
            player.moveTo(nexus.getX() + 0.5, nexus.getY(), nexus.getZ() + 0.5);
            h.assertTrue(LeySurges.onBareCrossing(player), "At the nexus it is a bare crossing");
            LeySurges.sicknessTick(player);
            h.assertTrue(ModEffects.afflicted(player, AfflictionEffect.Kind.LEY_SICKNESS), "The surge sickens whoever stands on it");
            player.removeAllEffects();
            player.moveTo(nexus.getX() + 20.5, nexus.getY(), nexus.getZ() + 0.5);
            h.assertFalse(LeySurges.onBareCrossing(player), "Twenty blocks off is not the crossing");
            data.setSurge(null, 0);
            player.moveTo(nexus.getX() + 0.5, nexus.getY(), nexus.getZ() + 0.5);
            LeySurges.sicknessTick(player);
            h.assertFalse(ModEffects.afflicted(player, AfflictionEffect.Kind.LEY_SICKNESS), "Without a surge the crossing is safe");
        } finally {
            data.setSurge(null, 0);
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyTribeGetsAFestivalAndJoiningPaysOnce(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            int cycle = TribalConfig.festivalCycleDays();
            Set<TribeDefinition> seen = new HashSet<>();
            int festivalDays = 0;
            for (long day = 0; day < cycle; day++) {
                TribeDefinition tribe = Festivals.tribeOn(day);
                if (tribe != null) { seen.add(tribe); festivalDays++; }
            }
            h.assertTrue(seen.size() == TribeDefinition.values().length, "Every tribe has a festival in the cycle, got " + seen.size());
            h.assertTrue(festivalDays == TribeDefinition.values().length, "One day each");
            h.assertTrue(Festivals.tribeOn(cycle) == Festivals.tribeOn(0), "The cycle repeats");
            long day = 0;
            TribeDefinition tribe = Festivals.tribeOn(day);
            int before = TribeStanding.get(player.server, player.getUUID(), tribe);
            h.assertTrue(Festivals.join(player, tribe, day), "Joining on the day is welcome");
            h.assertTrue(TribeStanding.get(player.server, player.getUUID(), tribe) - before == TribalConfig.festivalStanding(), "The gift pays festival standing");
            h.assertTrue(player.getInventory().countItem(tk.darrow.tribalpower.cuisine.CuisineRegistry.dish(tribe)) == 2, "And two of the dish");
            h.assertFalse(Festivals.join(player, tribe, day), "Only once a festival");
            h.assertTrue(Festivals.joined(player, tribe, day), "The world remembers who joined");
            TribeDefinition other = TribeDefinition.values()[(tribe.ordinal() + 1) % 9];
            h.assertFalse(Festivals.join(player, other, day), "Not another tribe's festival");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aWanderingSpiritLeavesAGiftAndGoes(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        ServerLevel level = h.getLevel();
        WanderingSpiritEntity spirit = MarchEvents.spawnSpirit(level, h.absolutePos(new BlockPos(3, 3, 3)));
        h.assertTrue(spirit != null && spirit.isAlive(), "A spirit can be set down");
        int items = 0;
        for (var stack : player.getInventory().items) if (!stack.isEmpty()) items++;
        spirit.gift(level, player);
        int after = 0;
        for (var stack : player.getInventory().items) if (!stack.isEmpty()) after++;
        h.assertTrue(after > items, "The spirit leaves a gift");
        h.runAtTickTime(5, () -> {
            h.assertFalse(spirit.isAlive(), "And goes");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(8)).isEmpty(), "Nothing spills on the ground");
            player.remove(Entity.RemovalReason.DISCARDED);
            h.succeed();
        });
    }
}
