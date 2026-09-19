package tk.darrow.tribalpower.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import tk.darrow.tribalpower.gate.GateKeystoneBlockEntity;
import tk.darrow.tribalpower.gate.GatePortal;
import tk.darrow.tribalpower.gate.GateRegistry;
import tk.darrow.tribalpower.gate.GateTint;
import tk.darrow.tribalpower.grit.GritItems;
import tk.darrow.tribalpower.grit.MineralGritItem;

/**
 * Two tints (design 3.1 section 15).
 *
 * <p>Mineral grit is one greyscale sprite coloured per material, so tin grit and lead grit are one texture
 * and two colours rather than two textures nobody drew.
 *
 * <p>A gate's plane takes the colour of where it goes -- green for the Overworld, teal for The March, red
 * for the Nether -- so a player can tell two gates apart at a glance without opening anything.
 *
 * <p>March turf, March Leaf and March Leaves are painted grey and take the biome's grass / foliage colour,
 * so the six March biomes differ underfoot without six sets of textures.
 */
public final class TribalColors {
    /** Steppe turf and canopy: what the tinted March blocks look like in the hand. */
    private static final int MARCH_GRASS = 0x4A8F7A, MARCH_FOLIAGE = 0x3D9A8C;

    private TribalColors() {}

    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            if (layer != 0) return 0xFFFFFFFF;
            String material = MineralGritItem.materialOf(stack);
            // Item colours are ARGB. A 0xRRGGBB value is alpha 0, so the sprite vanishes.
            int rgb = material == null ? 0xC8C8C8 : MineralGritItem.tint(material);
            return 0xFF000000 | rgb;
        }, GritItems.MINERAL_GRIT.get());
        event.register((stack, layer) -> 0xFF000000 | MARCH_GRASS,
                tk.darrow.tribalpower.item.ModItems.MARCH_GRASS.get(), tk.darrow.tribalpower.item.ModItems.MARCH_LEAF.get());
        event.register((stack, layer) -> 0xFF000000 | MARCH_FOLIAGE, tk.darrow.tribalpower.item.ModItems.MARCH_LEAVES.get());
    }

    public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, layer) -> {
            // Block colours are handed a tint getter that is only sometimes a Level; without one there is
            // no block entity to ask, so the plane falls back to the colour of nowhere in particular.
            if (!(level instanceof net.minecraft.world.level.Level world) || pos == null) return GateTint.UNKNOWN;
            GateKeystoneBlockEntity keystone = GatePortal.keystoneFor(world, pos);
            return keystone == null ? GateTint.UNKNOWN : keystone.destinationTint();
        }, GateRegistry.GATE_PORTAL.get());
        event.register((state, level, pos, layer) -> level == null || pos == null ? MARCH_GRASS
                        : net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos),
                tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get(), tk.darrow.tribalpower.block.ModBlocks.MARCH_LEAF.get());
        event.register((state, level, pos, layer) -> level == null || pos == null ? MARCH_FOLIAGE
                        : net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos),
                tk.darrow.tribalpower.block.ModBlocks.MARCH_LEAVES.get());
    }
}
