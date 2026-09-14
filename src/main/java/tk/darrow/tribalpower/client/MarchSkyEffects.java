package tk.darrow.tribalpower.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MarchSkyEffects extends DimensionSpecialEffects {
    public MarchSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(.10 + brightness * .72, .13 + brightness * .77, .20 + brightness * .8);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float time, float partial) {
        float[] base = super.getSunriseColor(time, partial);
        return base == null ? null : new float[] { .34F, .56F, .63F, base[3] * .65F };
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        return MarchSkyRenderer.draw(level, partialTick, modelViewMatrix, camera, isFoggy, setupFog);
    }
}
