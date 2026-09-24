package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import tk.darrow.tribalpower.event.MarchWeather;

/** What March weather looks and sounds like from inside it: particles round the player, a close fog in its colour, a sound now and then. */
public final class MarchWeatherClient {
    private static MarchWeather current;
    private static float blend;

    private MarchWeatherClient() {}

    private static MarchWeather here() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        return MarchWeather.at(mc.level, mc.player.blockPosition());
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        MarchWeather weather = here();
        current = weather != null ? weather : current;
        blend = Mth.clamp(blend + (weather != null ? 0.02F : -0.02F), 0, 1);
        if (weather == null) return;
        var random = mc.level.random;
        for (int i = 0; i < 12; i++) {
            double x = mc.player.getX() + (random.nextDouble() - 0.5) * 24, y = mc.player.getY() + random.nextDouble() * 12 - 2, z = mc.player.getZ() + (random.nextDouble() - 0.5) * 24;
            double fall = weather == MarchWeather.WHITEOUT ? -0.12 : weather == MarchWeather.ASHFALL ? -0.05 : weather == MarchWeather.FEN_MIST ? 0.0 : 0.01;
            mc.level.addParticle(weather.particle, x, y, z, (random.nextDouble() - 0.5) * 0.08, fall, (random.nextDouble() - 0.5) * 0.08);
        }
        if (mc.player.tickCount % 90 == 0) {
            var sound = switch (weather) {
                case ASHFALL -> SoundEvents.CAMPFIRE_CRACKLE;
                case GLIMMER_STORM -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case WHITEOUT -> SoundEvents.ELYTRA_FLYING;
                case FEN_MIST -> SoundEvents.AMBIENT_UNDERWATER_LOOP;
            };
            mc.level.playLocalSound(BlockPos.containing(mc.player.position()), sound, net.minecraft.sounds.SoundSource.WEATHER, 0.35F, weather == MarchWeather.WHITEOUT ? 0.5F : 0.8F, false);
        }
    }

    public static void fog(ViewportEvent.RenderFog event) {
        if (blend <= 0 || current == null || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float far = Mth.lerp(blend, event.getFarPlaneDistance(), current.sight);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.25F));
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (blend <= 0 || current == null || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        event.setRed(Mth.lerp(blend, event.getRed(), current.red));
        event.setGreen(Mth.lerp(blend, event.getGreen(), current.green));
        event.setBlue(Mth.lerp(blend, event.getBlue(), current.blue));
    }

    public static void reset() { current = null; blend = 0; }
}
