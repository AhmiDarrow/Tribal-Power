package tk.darrow.tribalpower.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import tk.darrow.tribalpower.TribalPower;

/**
 * {@code tribalpower:mod_version}: true when a mod is loaded at or above a version. Compat data that needs
 * something a mod only added later waits for it, instead of failing the load or refusing the game.
 */
public record ModVersionCondition(String modid, String minimum) implements ICondition {
    public static final MapCodec<ModVersionCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("modid").forGetter(ModVersionCondition::modid),
            Codec.STRING.fieldOf("minimum").forGetter(ModVersionCondition::minimum)
    ).apply(i, ModVersionCondition::new));

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, TribalPower.MOD_ID);

    static {
        CONDITIONS.register("mod_version", () -> CODEC);
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    @Override
    public boolean test(IContext context) {
        return ModList.get().getModContainerById(modid)
                .map(mod -> mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion(minimum)) >= 0)
                .orElse(false);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
