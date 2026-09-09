package tk.darrow.tribalpower.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tk.darrow.tribalpower.api.pulse.Attunement;

/** Bounded, server-authored effects: no shaders, entity spam, or client-only classes. */
public final class SpiritEffects {
    private SpiritEffects() {}
    public static Vector3f color(Attunement element) {
        return switch (element) {
            case EARTH -> new Vector3f(0.53F, 0.72F, 0.36F);
            case FIRE -> new Vector3f(1F, 0.48F, 0.23F);
            case WATER -> new Vector3f(0.28F, 0.75F, 0.91F);
            case AIR -> new Vector3f(0.81F, 0.92F, 0.83F);
            case SPIRIT -> new Vector3f(0.70F, 0.48F, 0.95F);
            case LOOM -> new Vector3f(0.38F, 0.82F, 0.79F);
        };
    }
    public static void ring(ServerLevel level, Vec3 center, Attunement element, double radius, int points) {
        DustParticleOptions dust = new DustParticleOptions(color(element), 0.85F);
        int count = Math.max(4, Math.min(24, points));
        double phase = level.getGameTime() * 0.025;
        for (int i = 0; i < count; i++) {
            double a = phase + i * Math.PI * 2 / count;
            level.sendParticles(dust, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }
    public static void beam(ServerLevel level, Vec3 from, Vec3 to, Attunement element) {
        DustParticleOptions dust = new DustParticleOptions(color(element), 0.9F);
        int count = Math.min(32, Math.max(2, (int)(from.distanceTo(to) * 2)));
        for (int i = 0; i <= count; i++) {
            Vec3 p = from.lerp(to, i / (double)count);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
