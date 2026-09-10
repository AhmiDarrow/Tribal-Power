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
 */
public final class TribalColors {
    private TribalColors() {}

    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            String material = MineralGritItem.materialOf(stack);
            return material == null ? 0xFFFFFF : MineralGritItem.tint(material);
        }, GritItems.MINERAL_GRIT.get());
    }

    public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, layer) -> {
            // Block colours are handed a tint getter that is only sometimes a Level; without one there is
            // no block entity to ask, so the plane falls back to the colour of nowhere in particular.
            if (!(level instanceof net.minecraft.world.level.Level world) || pos == null) return GateTint.UNKNOWN;
            GateKeystoneBlockEntity keystone = GatePortal.keystoneFor(world, pos);
            return keystone == null ? GateTint.UNKNOWN : keystone.destinationTint();
        }, GateRegistry.GATE_PORTAL.get());
    }
}
