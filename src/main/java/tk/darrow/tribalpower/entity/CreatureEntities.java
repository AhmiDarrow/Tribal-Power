package tk.darrow.tribalpower.entity;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.*;
public final class CreatureEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"tribalpower");
    public static final Map<CreatureProfile,DeferredHolder<EntityType<?>,EntityType<LatticeAnimal>>> ANIMALS=new EnumMap<>(CreatureProfile.class);
    public static final Map<CreatureProfile,DeferredHolder<EntityType<?>,EntityType<LatticeMonster>>> MONSTERS=new EnumMap<>(CreatureProfile.class);
    static {
        for(var p:CreatureProfile.values()) {
            // Fire immunity follows the profile, not the temperament: the lava dwellers are passive.
            if(p.animal) ANIMALS.put(p,ENTITIES.register(p.id,()-> {
                var builder=EntityType.Builder.of(LatticeAnimal::new,MobCategory.CREATURE).sized(p.width,p.height).clientTrackingRange(10);
                if(p.attack.equals("ember") || p.attack.equals("bolt"))builder.fireImmune();
                return builder.build("tribalpower:"+p.id);
            }));
            else MONSTERS.put(p,ENTITIES.register(p.id,()-> {
                var builder=EntityType.Builder.of(LatticeMonster::new,MobCategory.MONSTER).sized(p.width,p.height).clientTrackingRange(10);
                if(p.attack.equals("ember") || p.attack.equals("bolt"))builder.fireImmune();
                return builder.build("tribalpower:"+p.id);
            }));
        }
    }
    public static EntityType<? extends Mob> type(CreatureProfile p) { return p.animal?ANIMALS.get(p).get():MONSTERS.get(p).get(); }
    public static void attributes(EntityAttributeCreationEvent e) {
        for(var p:CreatureProfile.values()) {
            var a=Mob.createMobAttributes().add(Attributes.MAX_HEALTH,p.health).add(Attributes.MOVEMENT_SPEED,p.speed).add(Attributes.ARMOR,p.armor).add(Attributes.FOLLOW_RANGE,24).add(Attributes.ATTACK_DAMAGE,p.damage).add(Attributes.FLYING_SPEED,.35);
            e.put(type(p),a.build());
        }
    }
    public static void placements(RegisterSpawnPlacementsEvent e) {
        // Each creature is handed the rule for where it actually lives. One shared rule (turf, daylight,
        // no fluid) meant the cave dwellers could never spawn in a cave and the lava dwellers could never
        // spawn near lava: both were registered into biomes and appeared nowhere.
        ANIMALS.forEach((p,t)->{
            var habitat=CreatureHabitat.of(p);
            e.register(t.get(),SpawnPlacementTypes.NO_RESTRICTIONS,heightmapFor(habitat),
                    (type,level,reason,pos,random)->habitat.allowsAnimal(level,pos),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        MONSTERS.forEach((p,t)->{
            var habitat=CreatureHabitat.of(p);
            e.register(t.get(),SpawnPlacementTypes.NO_RESTRICTIONS,heightmapFor(habitat),
                    (type,level,reason,pos,random)->habitat.allowsMonster(level,pos,random,type),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    /** Cave and lava dwellers are placed by the motion-blocking floor, not the surface above them. */
    private static Heightmap.Types heightmapFor(CreatureHabitat habitat) {
        return habitat==CreatureHabitat.CAVE||habitat==CreatureHabitat.LAVA
                ?Heightmap.Types.MOTION_BLOCKING:Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
    }
}
