package tk.darrow.tribalpower.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import tk.darrow.tribalpower.world.MarchSkyMath;

/** High-definition celestial loom, with continuous day/night transitions. */
public final class MarchSkyRenderer {
    private static final ResourceLocation[] LAYERS = {
        ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_night.png"),
        ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_day.png")
    };
    private MarchSkyRenderer() {}
    public static boolean draw(ClientLevel level, float partial, Matrix4f modelView, Camera camera, boolean foggy, Runnable setupFog) {
        setupFog.run();
        if (foggy || camera.getFluidInCamera() != FogType.NONE) return true;
        float day = MarchSkyMath.dayness(level.getDayTime(), partial);
        float[] weights = {1-day, day};
        PanoramicSky.draw(level, partial, modelView, LAYERS, weights);
        return true;
    }
}
