package tk.darrow.tribalpower.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TribalPower.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tribalpower"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.SPIRIT_CODEX.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(ModItems.SPIRIT_CODEX.get());
                        out.accept(ModItems.SPIRIT_SHARD.get());
                        out.accept(ModItems.BONE_CHIME.get());
                        out.accept(ModItems.COPPER_RESONATOR.get());
                        out.accept(ModItems.PULSE_CELL.get());
                        out.accept(ModItems.RITUAL_CHALK.get());
                        out.accept(ModItems.SHATTERED_ORE.get());
                        out.accept(ModItems.ATTUNED_ORE.get());
                        out.accept(ModItems.BOUND_ORE.get());
                        out.accept(ModItems.MANIFEST_INGOT.get());
                        out.accept(ModItems.BLANK_SEAL.get());
                        out.accept(ModItems.EARTH_SEAL.get());
                        out.accept(ModItems.FIRE_SEAL.get());
                        out.accept(ModItems.WATER_SEAL.get());
                        out.accept(ModItems.AIR_SEAL.get());
                        out.accept(ModItems.SPIRIT_SEAL.get());
                        out.accept(ModItems.SPIRITGEAR_PICKAXE.get());
                        out.accept(ModItems.SPIRITGEAR_BLADE.get());
                        out.accept(ModItems.DRUMHEART.get());
                        out.accept(ModItems.LEY_COLLECTOR.get());
                        out.accept(ModItems.RESONANCE_TOTEM_EARTH.get());
                        out.accept(ModItems.RESONANCE_TOTEM_FIRE.get());
                        out.accept(ModItems.RESONANCE_TOTEM_WATER.get());
                        out.accept(ModItems.RESONANCE_TOTEM_AIR.get());
                        out.accept(ModItems.RESONANCE_TOTEM_SPIRIT.get());
                        out.accept(ModItems.SONG_BENCH.get());
                        out.accept(ModItems.LATTICE_CONDUCTOR.get());
                        out.accept(ModItems.ECHO_SHATTER.get());
                        out.accept(ModItems.ECHO_ATTUNE.get());
                        out.accept(ModItems.ECHO_BIND.get());
                        out.accept(ModItems.ECHO_MANIFEST.get());
                        out.accept(ModItems.ANCESTRAL_CACHE.get());
                        out.accept(ModItems.DEEP_CACHE.get());
                        out.accept(ModItems.RITE_PEDESTAL.get());
                        out.accept(ModItems.GATE_DRUM.get());
                        out.accept(ModItems.SPIRIT_DOOR.get());
                        out.accept(ModItems.MARCH_STONE.get());
                        out.accept(ModItems.MARCH_COBBLE.get());
                        out.accept(ModItems.MARCH_LOG.get());
                        out.accept(ModItems.MARCH_PLANKS.get());
                        out.accept(ModItems.MARCH_LEAF.get());
                        out.accept(ModItems.SPIRIT_REED.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
