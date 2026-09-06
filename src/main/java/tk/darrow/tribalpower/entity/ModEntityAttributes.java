package tk.darrow.tribalpower.entity;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SPIRIT_WISP.get(), SpiritWispEntity.createAttributes().build());
        event.put(ModEntities.MARCH_WALKER.get(), MarchWalkerEntity.createAttributes().build());
    }

    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.MARCH_WALKER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MarchWalkerEntity::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.SPIRIT_WISP.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpiritWispEntity::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
