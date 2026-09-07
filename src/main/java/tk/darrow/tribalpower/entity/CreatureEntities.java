package tk.darrow.tribalpower.entity;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.*;
public final class CreatureEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"tribalpower");
    public static final Map<CreatureProfile,DeferredHolder<EntityType<?>,EntityType<LatticeAnimal>>> ANIMALS=new EnumMap<>(CreatureProfile.class);
    public static final Map<CreatureProfile,DeferredHolder<EntityType<?>,EntityType<LatticeMonster>>> MONSTERS=new EnumMap<>(CreatureProfile.class);
    static {
        for(var p:CreatureProfile.values()) {
            if(p.animal) ANIMALS.put(p,ENTITIES.register(p.id,()->EntityType.Builder.of(LatticeAnimal::new,MobCategory.CREATURE).sized(p.width,p.height).clientTrackingRange(10).build("tribalpower:"+p.id)));
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
        ANIMALS.forEach((p,t)->e.register(t.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,LatticeAnimal::canSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE));
        MONSTERS.forEach((p,t)->e.register(t.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
}
