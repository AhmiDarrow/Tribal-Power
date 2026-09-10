package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrumheartBlockEntity>> DRUMHEART =
            BLOCK_ENTITIES.register("drumheart", () -> BlockEntityType.Builder.of(
                    DrumheartBlockEntity::new, ModBlocks.DRUMHEART.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeyCollectorBlockEntity>> LEY_COLLECTOR =
            BLOCK_ENTITIES.register("ley_collector", () -> BlockEntityType.Builder.of(
                    LeyCollectorBlockEntity::new, ModBlocks.LEY_COLLECTOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseResonatorBlockEntity>> PULSE_RESONATOR =
            BLOCK_ENTITIES.register("pulse_resonator", () -> BlockEntityType.Builder.of(
                    PulseResonatorBlockEntity::new, ModBlocks.PULSE_RESONATOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncestralCacheBlockEntity>> ANCESTRAL_CACHE =
            BLOCK_ENTITIES.register("ancestral_cache", () -> BlockEntityType.Builder.of(
                    AncestralCacheBlockEntity::new, ModBlocks.ANCESTRAL_CACHE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonanceTotemBlockEntity>> RESONANCE_TOTEM =
            BLOCK_ENTITIES.register("resonance_totem", () -> BlockEntityType.Builder.of(
                    ResonanceTotemBlockEntity::create,
                    ModBlocks.RESONANCE_TOTEM_EARTH.get(),
                    ModBlocks.RESONANCE_TOTEM_FIRE.get(),
                    ModBlocks.RESONANCE_TOTEM_WATER.get(),
                    ModBlocks.RESONANCE_TOTEM_AIR.get(),
                    ModBlocks.RESONANCE_TOTEM_SPIRIT.get(),
                    ModBlocks.RESONANCE_TOTEM_LOOM.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SongBenchBlockEntity>> SONG_BENCH =
            BLOCK_ENTITIES.register("song_bench", () -> BlockEntityType.Builder.of(
                    SongBenchBlockEntity::new, ModBlocks.SONG_BENCH.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LatticeConductorBlockEntity>> LATTICE_CONDUCTOR =
            BLOCK_ENTITIES.register("lattice_conductor", () -> BlockEntityType.Builder.of(
                    LatticeConductorBlockEntity::new, ModBlocks.LATTICE_CONDUCTOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GateDrumBlockEntity>> GATE_DRUM =
            BLOCK_ENTITIES.register("gate_drum", () -> BlockEntityType.Builder.of(
                    GateDrumBlockEntity::new, ModBlocks.GATE_DRUM.get()
            ).build(null));

    private ModBlockEntities() {}
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiritCisternBlockEntity>> SPIRIT_CISTERN =
            BLOCK_ENTITIES.register("spirit_cistern", () -> BlockEntityType.Builder.of(SpiritCisternBlockEntity::new, ModBlocks.SPIRIT_CISTERN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessRelayBlockEntity>> WIRELESS_RELAY =
            BLOCK_ENTITIES.register("wireless_relay", () -> BlockEntityType.Builder.of(WirelessRelayBlockEntity::new,
                    ModBlocks.ITEM_RELAY.get(), ModBlocks.FLUID_RELAY.get(), ModBlocks.LONGREACH_ITEM_RELAY.get(), ModBlocks.LONGREACH_FLUID_RELAY.get(),
                    ModBlocks.ASTRAL_ITEM_RELAY.get(), ModBlocks.ASTRAL_FLUID_RELAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseAdapterBlockEntity>> PULSE_ADAPTER =
            BLOCK_ENTITIES.register("pulse_adapter", () -> BlockEntityType.Builder.of(PulseAdapterBlockEntity::new, ModBlocks.PULSE_ADAPTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualBrazierBlockEntity>> RITUAL_BRAZIER =
            BLOCK_ENTITIES.register("ritual_brazier", () -> BlockEntityType.Builder.of(RitualBrazierBlockEntity::new, ModBlocks.RITUAL_BRAZIER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoStationBlockEntity>> ECHO_STATION =
            BLOCK_ENTITIES.register("echo_station", () -> BlockEntityType.Builder.of(EchoStationBlockEntity::new,
                    ModBlocks.ECHO_SHATTER.get(), ModBlocks.ECHO_ATTUNE.get(), ModBlocks.ECHO_BIND.get(), ModBlocks.ECHO_MANIFEST.get(), ModBlocks.ECHO_UNWEAVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonanceMeshBlockEntity>> RESONANCE_MESH =
            BLOCK_ENTITIES.register("resonance_mesh", () -> BlockEntityType.Builder.of(ResonanceMeshBlockEntity::new, ModBlocks.RESONANCE_MESH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneFontBlockEntity>> STONE_FONT =
            BLOCK_ENTITIES.register("stone_font", () -> BlockEntityType.Builder.of(StoneFontBlockEntity::new, ModBlocks.STONE_FONT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseCairnBlockEntity>> PULSE_CAIRN =
            BLOCK_ENTITIES.register("pulse_cairn", () -> BlockEntityType.Builder.of(PulseCairnBlockEntity::new, ModBlocks.PULSE_CAIRN.get()).build(null));

    /**
     * The Rite Pedestal gains a block entity in 3.1 so it can hold and render an item and answer a
     * comparator. Pedestals placed in 3.0 worlds have no saved block entity; modern chunk loading creates
     * one on demand from the block, so they wake up empty rather than broken (design 3.1 section 16).
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitePedestalBlockEntity>> RITE_PEDESTAL =
            BLOCK_ENTITIES.register("rite_pedestal", () -> BlockEntityType.Builder.of(RitePedestalBlockEntity::new, ModBlocks.RITE_PEDESTAL.get()).build(null));
}
