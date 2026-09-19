package tk.darrow.tribalpower.client.wildlife;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import tk.darrow.tribalpower.wildlife.Wildlife;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * The March's insects. They are particles, not entities: spawned on this client only, around this player
 * only, never saved, never sent over the network, and never more than a fixed number alive at once (fewer
 * on the Decreased particle setting, none on Minimal). They make the air feel alive for close to nothing.
 */
public final class MarchInsects {
    private static int live;
    private static int tick;
    /** Changing level clears particles without removing them one by one, so the count restarts with the level. */
    private static ClientLevel counted;

    private MarchInsects() {}

    public enum Kind { FIREFLY, REED_DARTER, GLASSWING, CINDER_GNAT }

    public static void providers(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Wildlife.FIREFLY.get(), sprites -> provider(sprites, Kind.FIREFLY));
        event.registerSpriteSet(Wildlife.REED_DARTER.get(), sprites -> provider(sprites, Kind.REED_DARTER));
        event.registerSpriteSet(Wildlife.GLASSWING.get(), sprites -> provider(sprites, Kind.GLASSWING));
        event.registerSpriteSet(Wildlife.CINDER_GNAT.get(), sprites -> provider(sprites, Kind.CINDER_GNAT));
    }

    private static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, Kind kind) {
        return (type, level, x, y, z, dx, dy, dz) -> new Insect(level, x, y, z, sprites, kind);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != counted) {
            counted = level;
            live = 0;
        }
        if (level == null || mc.player == null || mc.isPaused() || !level.dimension().equals(ModDimensions.THE_MARCH)) return;
        ParticleStatus setting = mc.options.particles().get();
        if (setting == ParticleStatus.MINIMAL) return;
        int cap = setting == ParticleStatus.ALL ? 70 : 30;
        if (live >= cap || tick++ % 2 != 0) return;
        var random = level.random;
        long time = level.getDayTime() % 24000;
        boolean night = time > 13000 && time < 23000;
        for (int attempt = 0; attempt < 3; attempt++) {
            int x = Mth.floor(mc.player.getX()) + random.nextInt(41) - 20;
            int z = Mth.floor(mc.player.getZ()) + random.nextInt(41) - 20;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(surface - mc.player.getY()) > 24) continue;
            BlockPos top = new BlockPos(x, surface, z);
            boolean water = level.getFluidState(top.below()).is(FluidTags.WATER);
            String biome = level.getBiome(top).unwrapKey().map(key -> key.location().getPath()).orElse("");
            boolean rain = level.isRainingAt(top);
            SimpleParticleType type = null;
            double y = surface + 0.4 + random.nextDouble();
            if (biome.equals("march_ember_wastes")) {
                type = Wildlife.CINDER_GNAT.get();
            } else if (rain || biome.equals("march_snow_fields")) {
                continue;
            } else if (water) {
                type = night ? Wildlife.FIREFLY.get() : Wildlife.REED_DARTER.get();
            } else if (night) {
                if (random.nextFloat() < 0.7F) {
                    type = Wildlife.FIREFLY.get();
                    y += random.nextDouble() * 2;
                }
            } else if (level.getBlockState(top).getBlock() instanceof BushBlock) {
                type = Wildlife.GLASSWING.get();
                y += random.nextDouble();
            }
            if (type != null) level.addParticle(type, x + random.nextDouble(), y, z + random.nextDouble(), 0, 0, 0);
        }
    }

    static final class Insect extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final Kind kind;
        private final float phase;
        private final double homeY;

        Insect(ClientLevel level, double x, double y, double z, SpriteSet sprites, Kind kind) {
            super(level, x, y, z);
            this.sprites = sprites;
            this.kind = kind;
            this.phase = random.nextFloat() * Mth.TWO_PI;
            this.homeY = y;
            this.hasPhysics = false;
            this.gravity = 0;
            this.xd = this.yd = this.zd = 0;
            switch (kind) {
                case FIREFLY -> { lifetime = 120 + random.nextInt(100); quadSize = 0.07F; }
                case REED_DARTER -> { lifetime = 80 + random.nextInt(60); quadSize = 0.12F; }
                case GLASSWING -> { lifetime = 160 + random.nextInt(100); quadSize = 0.11F; }
                case CINDER_GNAT -> { lifetime = 60 + random.nextInt(40); quadSize = 0.05F; yd = 0.015; }
            }
            live++;
            pickSprite(sprites);
            setSprite(sprites.get(0, 1));
        }

        @Override
        public void remove() {
            if (!removed) live = Math.max(0, live - 1);
            super.remove();
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) return;
            float fade = Math.min(1, Math.min(age, lifetime - age) / 15F);
            switch (kind) {
                case FIREFLY -> {
                    xd = Mth.clamp(xd + (random.nextDouble() - 0.5) * 0.008, -0.03, 0.03);
                    zd = Mth.clamp(zd + (random.nextDouble() - 0.5) * 0.008, -0.03, 0.03);
                    yd = Mth.clamp(yd + (random.nextDouble() - 0.5) * 0.006, -0.015, 0.015);
                    float pulse = 0.5F + 0.5F * Mth.sin(age * 0.12F + phase);
                    alpha = fade * (0.25F + 0.75F * pulse);
                    setSprite(sprites.get(pulse > 0.6F ? 1 : 0, 1));
                }
                case REED_DARTER -> {
                    if (random.nextInt(25) == 0) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        xd = Math.cos(angle) * 0.18;
                        zd = Math.sin(angle) * 0.18;
                    }
                    xd *= 0.86;
                    zd *= 0.86;
                    yd = (homeY - y) * 0.1 + Mth.sin(age * 0.3F + phase) * 0.01;
                    setSprite(sprites.get(age % 2, 1));
                    alpha = fade;
                }
                case GLASSWING -> {
                    xd = Mth.sin(age * 0.05F + phase) * 0.03;
                    zd = Mth.cos(age * 0.043F + phase) * 0.03;
                    yd = Mth.sin(age * 0.4F + phase) * 0.035 + (homeY - y) * 0.02;
                    setSprite(sprites.get((age / 3) % 3, 2));
                    alpha = fade;
                }
                case CINDER_GNAT -> {
                    xd = Mth.clamp(xd + (random.nextDouble() - 0.5) * 0.01, -0.03, 0.03);
                    zd = Mth.clamp(zd + (random.nextDouble() - 0.5) * 0.01, -0.03, 0.03);
                    alpha = fade;
                    setSprite(sprites.get(age / 4 % 2, 1));
                }
            }
        }

        @Override
        protected int getLightColor(float partialTick) {
            return kind == Kind.FIREFLY || kind == Kind.CINDER_GNAT ? 0xF000F0 : super.getLightColor(partialTick);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
