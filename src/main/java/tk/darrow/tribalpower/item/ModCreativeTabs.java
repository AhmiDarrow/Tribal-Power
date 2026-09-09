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
                        tk.darrow.tribalpower.camp.CampRegistry.ITEMS.getEntries().forEach(item->out.accept(item.get()));
                        out.accept(ModItems.SPIRIT_CODEX.get());
                        CreatureItems.REAGENTS.values().forEach(item -> out.accept(item.get()));
                        CreatureItems.EGGS.values().forEach(item -> out.accept(item.get()));
                        out.accept(ModItems.SPIRIT_SHARD.get());
                        out.accept(ModItems.BONE_CHIME.get());
                        out.accept(ModItems.COPPER_RESONATOR.get());
                        out.accept(ModItems.PULSE_CELL.get());
                        out.accept(PulseCellItem.createFilled(PulseCellItem.CAPACITY));
                        out.accept(ModItems.RITUAL_CHALK.get());
                        out.accept(ModItems.ECHO_SHARD.get());
                        out.accept(ModItems.ATTUNED_ECHO.get());
                        out.accept(ModItems.BOUND_ECHO.get());
                        out.accept(ModItems.MANIFESTED_INGOT.get());
                        out.accept(ModItems.BLANK_SEAL.get());
                        out.accept(ModItems.EARTH_SEAL.get());
                        out.accept(ModItems.FIRE_SEAL.get());
                        out.accept(ModItems.WATER_SEAL.get());
                        out.accept(ModItems.AIR_SEAL.get());
                        out.accept(ModItems.SPIRIT_SEAL.get());
                        out.accept(ModItems.SPIRITGEAR_PICKAXE.get());
                        out.accept(ModItems.SPIRITGEAR_AXE.get());
                        out.accept(ModItems.SPIRITGEAR_SHOVEL.get());
                        out.accept(ModItems.SPIRITGEAR_BLADE.get());
                        out.accept(ModItems.DRUMHEART.get());
                        out.accept(ModItems.LEY_COLLECTOR.get());
                        out.accept(ModItems.PULSE_RESONATOR.get());
                        out.accept(ModItems.RESONANCE_TOTEM_EARTH.get());
                        out.accept(ModItems.RESONANCE_TOTEM_FIRE.get());
                        out.accept(ModItems.RESONANCE_TOTEM_WATER.get());
                        out.accept(ModItems.RESONANCE_TOTEM_AIR.get());
                        out.accept(ModItems.RESONANCE_TOTEM_SPIRIT.get());
                        out.accept(ModItems.RESONANCE_TOTEM_LOOM.get());
                        out.accept(ModItems.SONG_BENCH.get());
                        out.accept(ModItems.LATTICE_CONDUCTOR.get());
                        out.accept(ModItems.ECHO_SHATTER.get());
                        out.accept(ModItems.ECHO_ATTUNE.get());
                        out.accept(ModItems.ECHO_BIND.get());
                        out.accept(ModItems.ECHO_MANIFEST.get());
                        out.accept(ModItems.ECHO_UNWEAVE.get());
                        out.accept(ModItems.ANCESTRAL_CACHE.get());
                        out.accept(ModItems.DEEP_CACHE.get());
                        out.accept(ModItems.RITE_PEDESTAL.get());
                        out.accept(ModItems.GATE_DRUM.get());
                        out.accept(ModItems.SPIRIT_DOOR.get());
                        out.accept(ModItems.MARCH_STONE.get());
                        out.accept(ModItems.MARCH_COBBLE.get());
                        out.accept(ModItems.MARCH_SOIL.get());
                        out.accept(ModItems.MARCH_GRASS.get());
                        out.accept(ModItems.MARCH_MOSS.get());
                        out.accept(ModItems.MARCH_LOG.get());
                        out.accept(ModItems.MARCH_PLANKS.get());
                        out.accept(ModItems.MARCH_LEAVES.get());
                        out.accept(ModItems.MARCH_LEAF.get());
                        out.accept(ModItems.MARCH_ORE.get());
                        out.accept(ModItems.MARCH_CRYSTAL.get());
                        out.accept(ModItems.SPIRIT_REED.get());
                        out.accept(ModItems.ECHO_BLOOM.get());
                        out.accept(ModItems.LEY_THISTLE.get());
                        out.accept(ModItems.IRON_GRIT.get());
                        out.accept(ModItems.GOLD_GRIT.get());
                        out.accept(ModItems.COPPER_GRIT.get());
                        out.accept(ModItems.SPIRITWEAVE.get());
                        out.accept(ModItems.RESONANT_CORE.get());
                        out.accept(ModItems.GREATER_PULSE_CELL.get());
                        out.accept(ModItems.SPIRIT_STAFF.get());
                        out.accept(ModItems.WAYFARER_SATCHEL.get());
                        out.accept(ModItems.RESONANCE_MAUL.get());
                        out.accept(ModItems.SPIRITWEAVE_HOOD.get());
                        out.accept(ModItems.SPIRITWEAVE_ROBE.get());
                        out.accept(ModItems.SPIRITWEAVE_LEGGINGS.get());
                        out.accept(ModItems.SPIRITWEAVE_BOOTS.get());
                        out.accept(ModItems.RITUAL_BRAZIER.get());
                        out.accept(ModItems.LATTICE_TUNER.get());
                        out.accept(ModItems.ITEM_RELAY.get());
                        out.accept(ModItems.FLUID_RELAY.get());
                        out.accept(ModItems.LONGREACH_ITEM_RELAY.get());
                        out.accept(ModItems.LONGREACH_FLUID_RELAY.get());
                        out.accept(ModItems.ASTRAL_ITEM_RELAY.get());
                        out.accept(ModItems.ASTRAL_FLUID_RELAY.get());
                        out.accept(ModItems.PULSE_ADAPTER.get());
                        out.accept(ModItems.SPIRIT_CISTERN.get());
                        out.accept(ModItems.WAYSTONE_COMPASS.get());
                        out.accept(ModItems.HORIZON_COMPASS.get());
                        out.accept(ModItems.ASTRAL_COMPASS.get());
                        tk.darrow.tribalpower.rite.world.WorldRiteRegistry.displayItems(out);
                        tk.darrow.tribalpower.ley.LeyRegistry.displayItems(out);
                        tk.darrow.tribalpower.logic.LogicRegistry.displayItems(out);
                        tk.darrow.tribalpower.familiar.FamiliarRegistry.displayItems(out);
                        tk.darrow.tribalpower.camp.identity.CampIdentityRegistry.displayItems(out);
                        tk.darrow.tribalpower.world.structure.MarchRegistry.displayItems(out);
                        tk.darrow.tribalpower.tribe.TribeRegistry.displayItems(out);
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
