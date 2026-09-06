package tk.darrow.tribalpower.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TribalPower.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritWispEntity>> SPIRIT_WISP =
            ENTITIES.register("spirit_wisp", () -> EntityType.Builder.of(SpiritWispEntity::new, MobCategory.AMBIENT)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .build("tribalpower:spirit_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<MarchWalkerEntity>> MARCH_WALKER =
            ENTITIES.register("march_walker", () -> EntityType.Builder.of(MarchWalkerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.5F)
                    .clientTrackingRange(10)
                    .build("tribalpower:march_walker"));

    private ModEntities() {}
}
