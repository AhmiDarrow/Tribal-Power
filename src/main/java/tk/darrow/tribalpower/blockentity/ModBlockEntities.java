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
                    ModBlocks.RESONANCE_TOTEM_SPIRIT.get()
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
}
