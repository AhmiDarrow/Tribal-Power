package tk.darrow.tribalpower.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.SpiritWispEntity;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * The living March: a scheduler that runs on the March level with or without anyone near, rolling weather,
 * surges and festival days, and telling every player in the March when something changes.
 */
public final class MarchEvents {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<WanderingSpiritEntity>> WANDERING_SPIRIT = ENTITIES.register("wandering_spirit", () ->
            EntityType.Builder.of(WanderingSpiritEntity::new, MobCategory.AMBIENT).sized(0.4F, 0.4F).clientTrackingRange(10).build("tribalpower:wandering_spirit"));
    /** How often the scheduler looks, in ticks. */
    public static final int CHECK = 100;

    private MarchEvents() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(WANDERING_SPIRIT.get(), SpiritWispEntity.createAttributes().build()));
        modBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(MarchStatePayload.TYPE, MarchStatePayload.STREAM_CODEC, (payload, context) -> MarchStatePayload.latest = payload));
        NeoForge.EVENT_BUS.addListener(MarchEvents::levelTick);
        NeoForge.EVENT_BUS.addListener(MarchEvents::playerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> { if (event.getEntity() instanceof ServerPlayer p) MarchStatePayload.send(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> { if (event.getEntity() instanceof ServerPlayer p) MarchStatePayload.send(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> { if (event.getEntity() instanceof ServerPlayer p) MarchStatePayload.send(p); });
    }

    // ---- the scheduler --------------------------------------------------------------------------------------------

    private static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(ModDimensions.THE_MARCH)) return;
        if (level.getGameTime() % CHECK != 0) return;
        tick(level, level.getGameTime());
    }

    /** One scheduler step at the given time. Public so tests can run the clock. Returns whether anything changed. */
    public static boolean tick(ServerLevel level, long now) {
        MarchEventsSavedData data = MarchEventsSavedData.get(level.getServer());
        boolean changed = false;
        // weather: run out, then roll each kind that is not running
        if (data.expireWeather(now)) changed = true;
        if (TribalConfig.weatherEnabled() && (now < data.lastWeatherRoll() || now - data.lastWeatherRoll() >= CHECK)) {
            data.setLastWeatherRoll(now);
            double perCheck = TribalConfig.weatherChancePerHour() * CHECK / 1000.0;
            for (MarchWeather weather : MarchWeather.values()) {
                if (data.weatherActive(weather, now) || data.weatherPending(weather, now) || level.random.nextDouble() >= perCheck) continue;
                int minutes = TribalConfig.weatherMinutesMin() + level.random.nextInt(Math.max(1, TribalConfig.weatherMinutesMax() - TribalConfig.weatherMinutesMin() + 1));
                long warning = TribalConfig.eventWarningSeconds() * 20L;
                data.setWeather(weather, now + warning, now + warning + minutes * 1200L);
                if (warning > 0) announce(level, Component.translatable("message.tribalpower.weather.gathers", Component.translatable(weather.key()), span(warning)).withStyle(ChatFormatting.YELLOW));
                changed = true;
            }
        }
        // a weather whose warning has run out sets in now
        for (MarchWeather weather : MarchWeather.values()) {
            long from = data.weatherFrom(weather);
            if (data.weatherActive(weather, now) && from > now - CHECK && from <= now) {
                announce(level, Component.translatable("message.tribalpower.weather.begins", Component.translatable(weather.key())).withStyle(ChatFormatting.GRAY));
                changed = true;
            }
        }
        // surge: one voice at a time, a rest between
        if (data.expireSurge(now)) {
            announce(level, Component.translatable("message.tribalpower.surge.ends").withStyle(ChatFormatting.GRAY));
            changed = true;
        }
        if (TribalConfig.surgesEnabled() && data.surgeVoice(now) == null && data.surgePending(now) == null && now - data.lastSurgeEnd() >= TribalConfig.surgeEveryMinutes() * 1200L) {
            Attunement voice = Attunement.values()[level.random.nextInt(Attunement.values().length)];
            long warning = TribalConfig.eventWarningSeconds() * 20L;
            data.setSurge(voice, now + warning, now + warning + TribalConfig.surgeMinutes() * 1200L);
            if (warning > 0) announce(level, Component.translatable("message.tribalpower.surge.gathers", Component.translatable("attunement.tribalpower." + voice.getSerializedName()), span(warning))
                    .withStyle(ChatFormatting.YELLOW));
            changed = true;
        }
        if (data.surgeVoice(now) != null && data.surgeFrom() > now - CHECK && data.surgeFrom() <= now) {
            announce(level, Component.translatable("message.tribalpower.surge.begins", Component.translatable("attunement.tribalpower." + data.surgeVoice(now).getSerializedName()))
                    .withStyle(ChatFormatting.AQUA));
            changed = true;
        }
        // festivals: computed from the day; announce the day it turns
        long day = Festivals.day(level);
        if (day != data.lastFestivalDay()) {
            data.setLastFestivalDay(day);
            TribeDefinition festival = Festivals.tribeOn(day);
            if (festival != null) announce(level, Component.translatable("message.tribalpower.festival.begins", festival.displayNameComponent()).withStyle(ChatFormatting.GOLD));
            changed = true;
        }
        if (changed) for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) MarchStatePayload.send(player);
        return changed;
    }

    /** A span of ticks as "two minutes" or "40 seconds". */
    public static Component span(long ticks) {
        long seconds = Math.max(1, ticks / 20);
        return seconds % 60 == 0 ? Component.translatable("message.tribalpower.time.minutes", seconds / 60)
                : Component.translatable("message.tribalpower.time.seconds", seconds);
    }

    private static void announce(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) { player.sendSystemMessage(message); player.displayClientMessage(message, true); }
    }

    // ---- the players -----------------------------------------------------------------------------------------------

    private static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(ModDimensions.THE_MARCH)) return;
        if (player.tickCount % 40 == 0) LeySurges.sicknessTick(player);
        if (player.tickCount % CHECK == 0) wanderingSpirit(player);
    }

    /** Now and then a spirit drifts near: rare, and never two near one player. */
    private static void wanderingSpirit(ServerPlayer player) {
        if (!TribalConfig.wanderingSpiritsEnabled() || player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        if (level.random.nextInt(Math.max(1, TribalConfig.wanderingSpiritOneIn())) != 0) return;
        if (!level.getEntitiesOfClass(WanderingSpiritEntity.class, player.getBoundingBox().inflate(64)).isEmpty()) return;
        double angle = level.random.nextDouble() * Math.PI * 2, distance = 12 + level.random.nextInt(12);
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance)).above(1 + level.random.nextInt(3));
        if (!level.getBlockState(at).isAir() || Math.abs(at.getY() - player.getY()) > 8) return;
        spawnSpirit(level, at);
    }

    /** Sets a wandering spirit down at a position. */
    public static WanderingSpiritEntity spawnSpirit(ServerLevel level, BlockPos at) {
        WanderingSpiritEntity spirit = WANDERING_SPIRIT.get().create(level);
        if (spirit == null) return null;
        spirit.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        spirit.setCustomName(Component.translatable("entity.tribalpower.wandering_spirit"));
        spirit.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        level.addFreshEntity(spirit);
        return spirit;
    }
}
