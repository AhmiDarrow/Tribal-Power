package tk.darrow.tribalpower.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import tk.darrow.tribalpower.TribalPower;

/**
 * The March's own block tags, written by tools/generate_march_breadth.py. The ground of fifty-nine biomes is too
 * many blocks to list in code, so what a beast may stand on, what a cave dweller may stand on and what a plant may
 * grow from are data.
 */
public final class MarchTags {
    /** Turf the March's beasts may rise on, beyond grass, moss, soil and snow. */
    public static final TagKey<Block> MARCH_TURF = tag("march_turf");
    /** Stone the cave dwellers may stand on, beyond the Overworld's and the March's own. */
    public static final TagKey<Block> CAVE_FOOTING = tag("cave_footing");
    /** Ground the March's plants may grow from, beyond its grass, moss and soil. */
    public static final TagKey<Block> PLANT_GROUND = tag("plant_ground");

    private MarchTags() {}

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, name));
    }
}
