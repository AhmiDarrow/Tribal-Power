package tk.darrow.tribalpower.verification;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import tk.darrow.tribalpower.entity.*;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.world.AuroraMath;
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class BestiaryGameTests {
    @GameTest(template="empty")
    public static void everyCreatureHasAttributesAndMarchHabitat(GameTestHelper h) {
        int animals=0,monsters=0;
        var biomes=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(var p:CreatureProfile.values()) {
            var entity=CreatureEntities.type(p).create(h.getLevel());
            h.assertTrue(entity!=null && entity.getMaxHealth()==p.health,"Creature must construct with its authored attributes: "+p.id);
            if(p.attack.equals("ember") || p.attack.equals("bolt"))h.assertTrue(entity.fireImmune(),"Fire spirits should resist fire");
            boolean habitat=false;
            for(String name:java.util.List.of("march_steppe","march_highlands","march_crystal_fields")) {
                var biome=biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:"+name));
                if(biome!=null)for(var entry:biome.getMobSettings().getMobs(p.animal?MobCategory.CREATURE:MobCategory.MONSTER).unwrap())if(entry.type==entity.getType())habitat=true;
            }
            h.assertTrue(habitat,"Every creature must have a reachable March habitat: "+p.id);
            h.assertTrue(CreatureItems.REAGENTS.get(p).get()!=Items.AIR && CreatureItems.EGGS.get(p).get()!=Items.AIR,"Every creature needs its registered reagent and spawn egg");
            if(p.animal)animals++;else monsters++;
        }
        // 3.0 roster (design §9): the Tribal Kin and The Unsung join the three animals and ten hostiles.
        var kin=tk.darrow.tribalpower.tribe.TribeRegistry.TRIBAL_KIN.get().create(h.getLevel());
        h.assertTrue(kin!=null && kin.getMaxHealth()==30 && kin.getType().getCategory()==MobCategory.MISC,"Tribal Kin must construct as a persistent MISC mob");
        var unsung=tk.darrow.tribalpower.world.structure.MarchRegistry.THE_UNSUNG.get().create(h.getLevel());
        h.assertTrue(unsung!=null && unsung.getMaxHealth()==400 && unsung.fireImmune() && unsung.getType().getCategory()==MobCategory.MONSTER,"The Unsung must construct with 400 HP, fire immunity and the MONSTER category");
        h.assertTrue(animals==3 && monsters==10,"Roster must contain three animals and ten hostiles");h.succeed();
    }
    @GameTest(template="empty")
    public static void brushingIsRenewableAndCannotSpamOrHarvestBabies(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        try {
            player.getAbilities().instabuild=false;player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BRUSH));
            var animal=h.spawn(CreatureEntities.ANIMALS.get(CreatureProfile.DAWN_STAG).get(),new BlockPos(3,2,3));animal.setNoAi(true);
            animal.setBaby(true);animal.mobInteract(player,InteractionHand.MAIN_HAND);
            h.assertTrue(animal.forageCooldown()==0,"Babies must not produce brush reagents");animal.setAge(0);
            animal.mobInteract(player,InteractionHand.MAIN_HAND);animal.mobInteract(player,InteractionHand.MAIN_HAND);
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,animal.getBoundingBox().inflate(2));
            h.assertTrue(drops.stream().filter(e->e.getItem().is(CreatureItems.REAGENTS.get(CreatureProfile.DAWN_STAG).get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Repeated brush use must yield one item only");
            var saved=new CompoundTag();animal.addAdditionalSaveData(saved);animal.readAdditionalSaveData(saved);
            h.assertTrue(animal.forageCooldown()==1200,"Harvest cooldown must survive saving");
            saved.putInt("ForageCooldown",Integer.MAX_VALUE);animal.readAdditionalSaveData(saved);h.assertTrue(animal.forageCooldown()==1200,"Malformed cooldown must remain bounded");
            var fox=CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get().create(h.getLevel());
            animal.setInLove(null);fox.setInLove(null);h.assertFalse(animal.canMate(fox),"Different species sharing one implementation must not interbreed");
            var child=animal.getBreedOffspring(h.getLevel(),animal);h.assertTrue(child!=null && child.getType()==animal.getType(),"Breeding must preserve species");h.succeed();
        } finally {h.getLevel().getServer().getPlayerList().remove(player);}
    }
    @GameTest(template="empty")
    public static void rangedSpiritsCannotCastThroughWalls(GameTestHelper h) {
        var caster=h.spawn(CreatureEntities.MONSTERS.get(CreatureProfile.CINDER_IMP).get(),new BlockPos(2,2,2));
        var target=h.spawn(EntityType.PIG,new BlockPos(8,2,2));caster.setNoAi(true);target.setNoAi(true);
        h.assertTrue(caster.canCastAt(target),"Clear target in range should be visible");
        for(int y=1;y<6;y++)for(int z=0;z<5;z++)h.setBlock(5,y,z,Blocks.STONE);
        h.assertFalse(caster.canCastAt(target),"Solid cover must interrupt a ranged spell");
        var far=h.absolutePos(new BlockPos(15,2,2));target.setPos(far.getX(),far.getY(),far.getZ());
        h.assertFalse(caster.canCastAt(target),"Targets beyond twelve blocks must not be hit");h.succeed();
    }
    @GameTest(template="empty")
    public static void auroraFadesWithMinecraftTimeAndWeather(GameTestHelper h) {
        h.assertTrue(AuroraMath.visibility(18000,0,0,0)>.99,"Aurora should peak at midnight");
        h.assertTrue(AuroraMath.visibility(6000,0,0,0)==0,"Aurora must disappear at noon");
        h.assertTrue(AuroraMath.visibility(18000,0,1,0)==0 && AuroraMath.visibility(18000,0,0,1)==0,"Rain and thunder must hide the aurora");
        for(long t=-48000;t<=48000;t+=37) {float a=AuroraMath.visibility(t,.5F,.2F,0);h.assertTrue(Float.isFinite(a)&&a>=0&&a<=1,"Sky opacity must remain bounded");}
        h.succeed();
    }
}
