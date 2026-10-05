package tk.darrow.tribalpower.item;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.camp.CampRegistry;
import tk.darrow.tribalpower.camp.identity.CampIdentityRegistry;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.device.DeviceRegistry;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.familiar.FamiliarRegistry;
import tk.darrow.tribalpower.gate.GateRegistry;
import tk.darrow.tribalpower.generator.GeneratorRegistry;
import tk.darrow.tribalpower.grit.GritItems;
import tk.darrow.tribalpower.guardian.GuardianRegistry;
import tk.darrow.tribalpower.healing.HealingRegistry;
import tk.darrow.tribalpower.kit.KitRegistry;
import tk.darrow.tribalpower.ley.LeyRegistry;
import tk.darrow.tribalpower.logic.LogicRegistry;
import tk.darrow.tribalpower.lore.LoreRegistry;
import tk.darrow.tribalpower.quest.QuestRegistry;
import tk.darrow.tribalpower.rite.world.WorldRiteRegistry;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRegistry;
import tk.darrow.tribalpower.wildlife.Wildlife;
import tk.darrow.tribalpower.world.MarchDecor;
import tk.darrow.tribalpower.world.MarchOres;
import tk.darrow.tribalpower.world.MarchTrees;
import tk.darrow.tribalpower.world.MarchWoods;
import tk.darrow.tribalpower.world.structure.MarchRegistry;

/**
 * Four tabs side by side after the vanilla ones. JEI sorts a mod's items by creative order and EMI follows
 * it too, so the tabs and the order inside them are how the mod reads in an item list: base materials before
 * what is made from them, variants together, chapters roughly in Spirit Codex order. Every item belongs to
 * exactly one tab (CreativeTabGameTests holds this); a few technical items have no tab on purpose.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TribalPower.MOD_ID);

    private static final ResourceLocation MAIN_ID = tab("main");
    private static final ResourceLocation GEAR_ID = tab("gear");
    private static final ResourceLocation HEARTH_ID = tab("hearth");
    private static final ResourceLocation MARCH_ID = tab("march");

    /** Vanilla's colour order for dyed blocks. */
    private static final List<DyeColor> COLOURS = List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
            DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
            DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);

    /** Getting started, power, workshops, materials, storage, travel, patterns, logic and the camp. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tribalpower.main"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .withTabsAfter(GEAR_ID)
                    .icon(() -> ModItems.SPIRIT_CODEX.get().getDefaultInstance())
                    .displayItems((params, out) -> main(out))
                    .build()
    );

    /** Spiritgear, Spiritweave, charms, songs, rites, healing and familiars. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR = TABS.register("gear", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tribalpower.gear"))
                    .withTabsBefore(MAIN_ID)
                    .withTabsAfter(HEARTH_ID)
                    .icon(() -> ModItems.SPIRITGEAR_PICKAXE.get().getDefaultInstance())
                    .displayItems((params, out) -> gear(out))
                    .build()
    );

    /** Cuisine, creature drops and every spawn egg. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HEARTH = TABS.register("hearth", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tribalpower.hearth"))
                    .withTabsBefore(GEAR_ID)
                    .withTabsAfter(MARCH_ID)
                    .icon(() -> CuisineRegistry.HEARTH_POT_ITEM.get().getDefaultInstance())
                    .displayItems((params, out) -> hearth(out))
                    .build()
    );

    /** March blocks and building, decor, the tribes, lore and the record. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MARCH = TABS.register("march", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tribalpower.march"))
                    .withTabsBefore(HEARTH_ID)
                    .icon(() -> ModItems.MARCH_GRASS.get().getDefaultInstance())
                    .displayItems((params, out) -> march(out))
                    .build()
    );

    private ModCreativeTabs() {}

    private static void main(CreativeModeTab.Output out) {
        // Getting started
        out.accept(ModItems.SPIRIT_CODEX.get());
        out.accept(ModItems.RITUAL_CHALK.get());
        out.accept(ModItems.BUILDERS_CHALK.get());
        out.accept(ModItems.BONE_CHIME.get());
        out.accept(ModItems.COPPER_RESONATOR.get());
        // Pulse cells, the empty one beside a full one
        out.accept(ModItems.PULSE_CELL.get());
        out.accept(PulseCellItem.createFilled(PulseCellItem.CAPACITY));
        out.accept(ModItems.GREATER_PULSE_CELL.get());
        out.accept(ModItems.GRAND_PULSE_CELL.get());
        // Generators and totems
        out.accept(ModItems.DRUMHEART.get());
        out.accept(ModItems.LEY_COLLECTOR.get());
        out.accept(ModItems.PULSE_RESONATOR.get());
        out.accept(ModItems.RESONANCE_TOTEM_EARTH.get());
        out.accept(ModItems.RESONANCE_TOTEM_FIRE.get());
        out.accept(ModItems.RESONANCE_TOTEM_WATER.get());
        out.accept(ModItems.RESONANCE_TOTEM_AIR.get());
        out.accept(ModItems.RESONANCE_TOTEM_SPIRIT.get());
        out.accept(ModItems.RESONANCE_TOTEM_LOOM.get());
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TribeRegistry.KINSHIP_TOTEM_ITEM.get()));
        GeneratorRegistry.displayItems(out);
        out.accept(ModItems.PULSE_CAIRN.get());
        out.accept(ModItems.LATTICE_CONDUCTOR.get());
        out.accept(ModItems.PULSE_ADAPTER.get());
        out.accept(ModItems.LATTICE_CONVERTER.get());
        // Workshops and Echo stations
        out.accept(ModItems.ECHO_SHATTER.get());
        out.accept(ModItems.ECHO_ATTUNE.get());
        out.accept(ModItems.ECHO_BIND.get());
        out.accept(ModItems.ECHO_MANIFEST.get());
        out.accept(ModItems.ECHO_UNWEAVE.get());
        out.accept(ModItems.EMBER_KILN.get());
        out.accept(ModItems.SPIRIT_ANVIL.get());
        ModItems.TRIBAL_BENCHES.values().forEach(item -> out.accept(item.get()));
        // Materials: grit to Echo to metal, then the seals
        out.accept(ModItems.SPIRIT_SHARD.get());
        out.accept(ModItems.IRON_GRIT.get());
        out.accept(ModItems.GOLD_GRIT.get());
        out.accept(ModItems.COPPER_GRIT.get());
        GritItems.displayItems(out);
        out.accept(ModItems.ECHO_SHARD.get());
        out.accept(ModItems.ATTUNED_ECHO.get());
        out.accept(ModItems.BOUND_ECHO.get());
        out.accept(ModItems.MANIFESTED_INGOT.get());
        out.accept(ModItems.MANIFESTED_BLOCK.get());
        out.accept(ModItems.RESONANT_CORE.get());
        out.accept(ModItems.BLANK_SEAL.get());
        out.accept(ModItems.EARTH_SEAL.get());
        out.accept(ModItems.FIRE_SEAL.get());
        out.accept(ModItems.WATER_SEAL.get());
        out.accept(ModItems.AIR_SEAL.get());
        out.accept(ModItems.SPIRIT_SEAL.get());
        out.accept(ModItems.LOOM_SEAL.get());
        out.accept(ModItems.LOOM_THREAD.get());
        // Storage
        out.accept(ModItems.ANCESTRAL_CACHE.get());
        out.accept(ModItems.DEEP_CACHE.get());
        out.accept(ModItems.SPIRIT_CISTERN.get());
        out.accept(ModItems.SPIRIT_FLASK.get());
        out.accept(ModItems.GREATER_SPIRIT_FLASK.get());
        out.accept(ModItems.WAYFARER_SATCHEL.get());
        // Relays, cargo and the hand tools for machines
        out.accept(ModItems.ITEM_RELAY.get());
        out.accept(ModItems.FLUID_RELAY.get());
        out.accept(ModItems.LONGREACH_ITEM_RELAY.get());
        out.accept(ModItems.LONGREACH_FLUID_RELAY.get());
        out.accept(ModItems.ASTRAL_ITEM_RELAY.get());
        out.accept(ModItems.ASTRAL_FLUID_RELAY.get());
        out.accept(ModItems.LATTICE_TUNER.get());
        out.accept(ModItems.TOTEM_WRENCH.get());
        out.accept(ModItems.WEAVERS_WAND.get());
        // Travel
        out.accept(ModItems.GATE_DRUM.get());
        out.accept(ModItems.WAYSTONE_COMPASS.get());
        out.accept(ModItems.HORIZON_COMPASS.get());
        out.accept(ModItems.ASTRAL_COMPASS.get());
        GateRegistry.displayItems(out);
        // Patterns
        out.accept(ModItems.STONE_FONT.get());
        out.accept(ModItems.RESONANCE_MESH.get());
        out.accept(ModItems.ANCHOR_STONE.get());
        // Logic plates
        LogicRegistry.displayItems(out);
        // Camp machines and devices, in Codex order
        accept(out, "grove_tender", "wind_snare");
        out.accept(DeviceRegistry.SEAL_LOOM_ITEM.get());
        out.accept(DeviceRegistry.RECIPE_SEAL.get());
        out.accept(DeviceRegistry.TIDE_PUMP_ITEM.get());
        out.accept(DeviceRegistry.WARD_DRUM_ITEM.get());
        accept(out, "hush_totem", "wayanchor", "summoning_cradle");
        out.accept(CampRegistry.EFFIGY.get());
        accept(out, "spirit_lantern", "rain_chime", "offering_table");
        out.accept(KitRegistry.VINE_LIFT_ITEM.get());
        out.accept(KitRegistry.SONGKEEPER_DRUM_ITEM.get());
        CampIdentityRegistry.displayItems(out);
    }

    private static void gear(CreativeModeTab.Output out) {
        // Spiritgear tools, then the Blade and its family
        out.accept(ModItems.SPIRITGEAR_PICKAXE.get());
        out.accept(ModItems.SPIRITGEAR_AXE.get());
        out.accept(ModItems.SPIRITGEAR_SHOVEL.get());
        out.accept(ModItems.SPIRITGEAR_HOE.get());
        out.accept(ModItems.SPIRITGEAR_SHEARS.get());
        out.accept(ModItems.SPIRITGEAR_BLADE.get());
        ModItems.SPIRITGEAR_WEAPONS.values().forEach(weapon -> out.accept(weapon.get()));
        out.accept(ModItems.RESONANCE_MAUL.get());
        // Spiritweave, the cloth before the clothes
        out.accept(ModItems.SPIRITWEAVE.get());
        out.accept(ModItems.SPIRITWEAVE_HOOD.get());
        out.accept(ModItems.SPIRITWEAVE_ROBE.get());
        out.accept(ModItems.SPIRITWEAVE_LEGGINGS.get());
        out.accept(ModItems.SPIRITWEAVE_BOOTS.get());
        // Charms, staff, sight and urns
        out.accept(ModItems.SKY_CHARM.get());
        out.accept(ModItems.EMBER_CHARM.get());
        out.accept(ModItems.TIDE_CHARM.get());
        out.accept(ModItems.ROOT_CHARM.get());
        out.accept(ModItems.LANTERN_CHARM.get());
        out.accept(ModItems.SPINDLE_CHARM.get());
        out.accept(ModItems.WARD_CHARM.get());
        out.accept(ModItems.HEARTH_CHARM.get());
        out.accept(ModItems.VEIL_CHARM.get());
        out.accept(ModItems.CHORUS_CHARM.get());
        out.accept(ModItems.GATHERING_CHARM.get());
        out.accept(ModItems.SPIRIT_STAFF.get());
        LeyRegistry.displayItems(out);
        KitRegistry.SOUL_URNS.values().forEach(urn -> out.accept(urn.get()));
        // Songs
        out.accept(ModItems.SONG_BENCH.get());
        out.accept(ModItems.REAGENT_POUCH.get());
        out.accept(ModItems.SONG_SHEET.get());
        out.accept(ModItems.SONGBOOK.get());
        out.accept(ModItems.BOUND_SONGBOOK.get());
        out.accept(ModItems.CHORUS_SONGBOOK.get());
        out.accept(ModItems.PULSE_BOW.get());
        out.accept(KitRegistry.PULSE_CROSSBOW.get());
        out.accept(ModItems.VERSE_ARROW.get());
        // Rites
        out.accept(ModItems.RITUAL_BRAZIER.get());
        out.accept(ModItems.RITE_PEDESTAL.get());
        WorldRiteRegistry.displayItems(out);
        // Healing
        out.accept(ModItems.SPIRITGEAR_RATTLE.get());
        HealingRegistry.displayItems(out);
        // Familiars
        FamiliarRegistry.displayItems(out);
    }

    private static void hearth(CreativeModeTab.Output out) {
        // Cuisine: the pot, crops and seeds, dishes, fare, feasts
        CuisineRegistry.displayItems(out);
        // Game and catch
        out.accept(ModItems.RAW_GAME.get());
        out.accept(ModItems.ROAST_GAME.get());
        out.accept(ModItems.MARCH_LEATHER.get());
        out.accept(Wildlife.RAW_GLIMMERFIN.get());
        out.accept(Wildlife.COOKED_GLIMMERFIN.get());
        out.accept(Wildlife.RAW_SILT_EEL.get());
        out.accept(Wildlife.SMOKED_SILT_EEL.get());
        out.accept(Wildlife.DRIFT_JELLY.get());
        out.accept(Wildlife.GLIMMERFIN_BUCKET.get());
        // Reagents in bestiary order, then what guardians and the Unsung leave behind
        for (CreatureProfile profile : CreatureProfile.values())
            if (profile.animal && !profile.boss()) out.accept(CreatureItems.REAGENTS.get(profile).get());
        for (CreatureProfile profile : CreatureProfile.values())
            if (!profile.animal && !profile.boss()) out.accept(CreatureItems.REAGENTS.get(profile).get());
        for (CreatureProfile profile : CreatureProfile.values())
            if (profile.boss()) out.accept(CreatureItems.REAGENTS.get(profile).get());
        GuardianRegistry.CORES.values().forEach(core -> out.accept(core.get()));
        out.accept(ModItems.UNSUNG_HEART.get());
        // Spawn eggs: companions and wildlife
        TribeRegistry.EGGS.values().forEach(egg -> out.accept(egg.get()));
        out.accept(ModItems.MARCH_WALKER_SPAWN_EGG.get());
        out.accept(ModItems.SPIRIT_WISP_SPAWN_EGG.get());
        for (CreatureProfile profile : CreatureProfile.values())
            if (profile.animal && !profile.boss()) out.accept(CreatureItems.EGGS.get(profile).get());
        Wildlife.EGGS.forEach(egg -> out.accept(egg.get()));
        // hunters and remnants
        for (CreatureProfile profile : CreatureProfile.values())
            if (!profile.animal && !profile.boss()) out.accept(CreatureItems.EGGS.get(profile).get());
        // guardians and bosses
        for (CreatureProfile profile : CreatureProfile.values())
            if (profile.boss()) out.accept(CreatureItems.EGGS.get(profile).get());
        GuardianRegistry.EGGS.values().forEach(egg -> out.accept(egg.get()));
        out.accept(MarchRegistry.THE_UNSUNG_EGG.get());
    }

    private static void march(CreativeModeTab.Output out) {
        // Stone, each with its shapes, then polished, then bricks
        out.accept(ModItems.MARCH_STONE.get());
        shapes(out, "march_stone", true);
        out.accept(ModItems.MARCH_COBBLE.get());
        shapes(out, "march_cobble", true);
        stoneFamily(out, "march_stone");
        out.accept(ModItems.MOONSTONE.get());
        shapes(out, "moonstone", true);
        stoneFamily(out, "moonstone");
        out.accept(ModItems.MOSS_AGATE.get());
        shapes(out, "moss_agate", true);
        stoneFamily(out, "moss_agate");
        // Ground
        out.accept(ModItems.MARCH_SOIL.get());
        out.accept(ModItems.MARCH_GRASS.get());
        out.accept(ModItems.MARCH_PATH.get());
        out.accept(ModItems.MARCH_MOSS.get());
        out.accept(MarchDecor.ITEMS.get("march_silt").get());
        // Ores and crystal
        out.accept(ModItems.MARCH_ORE.get());
        MarchOres.ITEMS.values().forEach(item -> out.accept(item.get()));
        out.accept(ModItems.MARCH_CRYSTAL.get());
        out.accept(ModItems.MARCH_QUARTZ.get());
        // March wood, then each grown wood with its leaves and sapling
        out.accept(ModItems.MARCH_LOG.get());
        out.accept(ModItems.MARCH_PLANKS.get());
        accept(out, "march_planks_stairs", "march_planks_slab", "march_fence", "march_fence_gate", "march_door",
                "march_trapdoor", "march_pressure_plate", "march_button");
        out.accept(ModItems.MARCH_LEAVES.get());
        out.accept(ModItems.MARCH_LEAF.get());
        out.accept(ModItems.MARCH_SAPLING.get());
        MarchWoods.ITEMS.values().forEach(item -> out.accept(item.get()));
        // Plants
        out.accept(MarchTrees.WILLOW_STRAND_ITEM.get());
        out.accept(ModItems.SPIRIT_REED.get());
        out.accept(ModItems.ECHO_BLOOM.get());
        out.accept(ModItems.LEY_THISTLE.get());
        accept(out, "march_lily_pad", "moon_lily", "ribbon_weed", "veil_lichen", "echo_roots", "lantern_cap");
        // Quartz glass: clear, then vanilla's colour order; each block and pane sits beside its lit twin
        glass(out, "quartz_glass", "horizontal_quartz_glass_pane");
        for (DyeColor dye : COLOURS)
            glass(out, dye.getSerializedName() + "_quartz_glass", dye.getSerializedName() + "_horizontal_quartz_glass_pane");
        // Lights and furniture
        out.accept(ModItems.GLOW_REED.get());
        out.accept(ModItems.SHARD_LAMP.get());
        out.accept(ModItems.ECHO_SCONCE.get());
        out.accept(ModItems.EMBER_BOWL.get());
        out.accept(ModItems.MARCH_STOOL.get());
        out.accept(ModItems.MARCH_TABLE.get());
        out.accept(ModItems.SPIRIT_URN.get());
        out.accept(ModItems.WOVEN_MAT.get());
        out.accept(ModItems.WALL_SHELF.get());
        out.accept(ModItems.HANGING_RACK.get());
        out.accept(ModItems.SPIRIT_DOOR.get());
        out.accept(ModItems.WIND_CHARM.get());
        // What the March builds: drum circles, guardian altars, carved stones and murals
        out.accept(MarchRegistry.SILENT_DRUM_ITEM.get());
        out.accept(GuardianRegistry.ALTAR_ITEM.get());
        out.accept(LoreRegistry.CARVED_STONE_ITEM.get());
        out.accept(LoreRegistry.MURAL_ITEM.get());
        // The tribes: hearths, banners and their patterns, marks, relics
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TribeRegistry.TRIBE_HEARTH_ITEM.get()));
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TribeRegistry.TRIBE_BANNER_ITEM.get()));
        LoreRegistry.PATTERN_ITEMS.values().forEach(item -> out.accept(item.get()));
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TribeRegistry.TRIBE_MARK.get()));
        QuestRegistry.displayItems(out);
        // Lore and the record
        out.accept(MarchRegistry.LORE_TABLET_ITEM.get());
        out.accept(ModItems.MUSIC_DISC_DRUM_CIRCLE.get());
    }

    /** Polished stone, then bricks with their cracked and chiseled forms, each followed by its shapes. */
    private static void stoneFamily(CreativeModeTab.Output out, String stone) {
        accept(out, "polished_" + stone);
        shapes(out, "polished_" + stone, false);
        accept(out, stone + "_bricks", "cracked_" + stone + "_bricks", "chiseled_" + stone + "_bricks");
        shapes(out, stone + "_brick", true);
    }

    /** Stairs, slab, wall when it has one, pressure plate and button. */
    private static void shapes(CreativeModeTab.Output out, String prefix, boolean wall) {
        accept(out, prefix + "_stairs", prefix + "_slab");
        if (wall) accept(out, prefix + "_wall");
        accept(out, prefix + "_pressure_plate", prefix + "_button");
    }

    /** One colour of quartz glass: block, standing pane and flat sheet, each followed by its lit twin. */
    private static void glass(CreativeModeTab.Output out, String block, String sheet) {
        accept(out, block, "lit_" + block, block + "_pane", "lit_" + block + "_pane", sheet, "lit_" + sheet);
    }

    private static void accept(CreativeModeTab.Output out, String... paths) {
        for (String path : paths) out.accept(item(path));
    }

    private static Item item(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, path);
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("The creative tabs name " + id + ", which is not registered"));
    }

    private static ResourceLocation tab(String path) {
        return ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, path);
    }
}
