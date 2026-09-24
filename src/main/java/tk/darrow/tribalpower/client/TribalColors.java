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
        // a flask's window is tinted the colour of what it holds
        event.register((stack, layer) -> {
            if (layer != 1) return 0xFFFFFFFF;
            var fluid = tk.darrow.tribalpower.item.SpiritFlaskItem.contents(stack);
            return fluid.isEmpty() ? 0xFFFFFFFF : fluidColour(fluid);
        }, tk.darrow.tribalpower.item.ModItems.SPIRIT_FLASK.get(), tk.darrow.tribalpower.item.ModItems.GREATER_SPIRIT_FLASK.get());
    }

    private static final java.util.Map<net.minecraft.world.level.material.Fluid, Integer> FLUID_AVERAGE = new java.util.HashMap<>();

    /** A fluid's colour for a tint: its own tint when it has one (water), else the average of its still texture (lava). */
    public static int fluidColour(net.neoforged.neoforge.fluids.FluidStack fluid) {
        var extensions = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
        int tint = extensions.getTintColor(fluid);
        if ((tint & 0xFFFFFF) != 0xFFFFFF) return 0xFF000000 | tint;
        return FLUID_AVERAGE.computeIfAbsent(fluid.getFluid(), f -> {
            try {
                var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
                var image = sprite.contents().getOriginalImage();
                long r = 0, g = 0, b = 0, n = 0;
                for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) == 0) continue;
                    r += abgr & 0xFF; g += abgr >> 8 & 0xFF; b += abgr >> 16 & 0xFF; n++;
                }
                return n == 0 ? 0xFFFFFFFF : 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            } catch (Exception e) {
                return 0xFFFFFFFF;
            }
        });
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
