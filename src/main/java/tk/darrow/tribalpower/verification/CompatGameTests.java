package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Other mods find the March through the common {@code c:} tags: a mount mod spawns its swamp birds in a biome
 * tagged c:is_swamp, a weather mod snows where c:is_snowy says. Every March biome carries a climate and enough
 * shape for that to work, read from the tags the server actually loaded.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class CompatGameTests {
    private CompatGameTests() {}

    private static final List<String> MARCH = List.of("march_steppe", "march_highlands", "march_glimmer_ridge",
            "march_snow_fields", "march_reed_fen", "march_shallows", "march_crystal_fields", "march_ember_wastes");

    private static TagKey<Biome> c(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    @GameTest(template = "empty")
    public static void everyMarchBiomeCarriesCommonTags(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String id : MARCH) {
            var holder = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", id)));
            boolean climate = holder.is(c("is_hot")) || holder.is(c("is_temperate")) || holder.is(c("is_cold"));
            h.assertTrue(climate, id + " has a climate tag (c:is_hot, c:is_temperate or c:is_cold)");
            h.assertFalse(holder.is(c("is_hot")) && holder.is(c("is_cold")), id + " is not both hot and cold");
        }
        var fen = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", "march_reed_fen")));
        h.assertTrue(fen.is(c("is_swamp")) && !fen.is(c("is_plains")), "The Reed Fen is a swamp, not plains");
        var wastes = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", "march_ember_wastes")));
        h.assertTrue(wastes.is(c("is_hot")) && wastes.is(c("is_dry")) && wastes.is(c("is_wasteland")), "The Ember Wastes are hot, dry wasteland");
        var snow = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", "march_snow_fields")));
        h.assertTrue(snow.is(c("is_snowy_plains")) && snow.is(c("is_cold")), "The Snow Fields are cold snowy plains");
        h.succeed();
    }

    /**
     * With Shamanic Mounts in the pack, wild mounts live in every March biome and can stand on March ground.
     * Passes trivially without it, like the AgriCraft tests: the verification run does not always carry the jar.
     */
    @GameTest(template = "empty")
    public static void shamanicMountsLiveInTheMarch(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("shamanicmounts")) { h.succeed(); return; }
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var spawns = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("shamanicmounts", "has_spawns"));
        for (String id : MARCH) {
            var holder = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", id)));
            h.assertTrue(holder.is(spawns), id + " is in shamanicmounts:has_spawns");
            boolean spawner = holder.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream()
                    .anyMatch(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getNamespace().equals("shamanicmounts"));
            h.assertTrue(spawner, id + " has a Shamanic Mounts creature spawner after biome modifiers");
        }
        var ground = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("shamanicmounts", "spawnable_on"));
        for (var block : List.of(tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get(), tk.darrow.tribalpower.block.ModBlocks.MARCH_STONE.get(),
                net.minecraft.world.level.block.Blocks.SMOOTH_BASALT))
            h.assertTrue(block.defaultBlockState().is(ground), block + " is ground a wild mount spawns on");
        h.succeed();
    }

    /** Quartz Glass and the Manifested Ingot answer to the common tags recipes from other mods ask for. */
    @GameTest(template = "empty")
    public static void glassAndIngotsCarryCommonTags(GameTestHelper h) {
        var glass = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tribalpower", "quartz_glass"));
        h.assertTrue(new ItemStack(glass).is(net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS), "Quartz Glass is c:glass_blocks");
        h.assertTrue(new ItemStack(tk.darrow.tribalpower.item.ModItems.MANIFESTED_INGOT.get()).is(net.neoforged.neoforge.common.Tags.Items.INGOTS),
                "The Manifested Ingot is c:ingots");
        h.succeed();
    }
}
