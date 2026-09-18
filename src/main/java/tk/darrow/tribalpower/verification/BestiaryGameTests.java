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
            for(String name:java.util.List.of("march_steppe","march_highlands","march_crystal_fields","march_snow_fields","march_ember_wastes","march_reed_fen")) {
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
    public static void marchFaunaStandsOnSnowAndSplitsByBiome(GameTestHelper h) {
        h.setBlock(2, 5, 2, Blocks.GLOWSTONE);
        h.setBlock(2, 1, 2, tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get());
        h.setBlock(3, 1, 2, Blocks.SNOW_BLOCK);
        h.setBlock(4, 1, 2, tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get());
        h.setBlock(4, 2, 2, Blocks.SNOW);
        h.setBlock(5, 1, 2, tk.darrow.tribalpower.block.ModBlocks.MARCH_SOIL.get());
        h.setBlock(2, 2, 2, Blocks.AIR);
        h.setBlock(2, 3, 2, Blocks.AIR);
        h.setBlock(3, 2, 2, Blocks.AIR);
        h.setBlock(3, 3, 2, Blocks.AIR);
        h.setBlock(4, 3, 2, Blocks.AIR);
        h.setBlock(4, 4, 2, Blocks.AIR);
        h.setBlock(5, 2, 2, Blocks.AIR);
        h.setBlock(5, 3, 2, Blocks.AIR);
        var grass = h.absolutePos(new BlockPos(2, 2, 2));
        var snow = h.absolutePos(new BlockPos(3, 2, 2));
        var snowLayer = h.absolutePos(new BlockPos(4, 3, 2));
        var soil = h.absolutePos(new BlockPos(5, 2, 2));
        var random = h.getLevel().getRandom();
        h.assertTrue(MarchSpawns.turf(Blocks.SNOW.defaultBlockState()), "Snow layers are March turf");
        h.assertTrue(MarchSpawns.turf(tk.darrow.tribalpower.block.ModBlocks.MARCH_SOIL.get().defaultBlockState()),
                "Ember soil is March turf");
        h.assertTrue(LatticeAnimal.canSpawn(CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get(),
                h.getLevel(), MobSpawnType.NATURAL, grass, random), "Lantern foxes must stand on March grass");
        h.assertTrue(LatticeAnimal.canSpawn(CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get(),
                h.getLevel(), MobSpawnType.NATURAL, snow, random), "Lantern foxes must stand on snow");
        h.assertTrue(LatticeAnimal.canSpawn(CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get(),
                h.getLevel(), MobSpawnType.NATURAL, snowLayer, random), "Lantern foxes must stand on freeze snow");
        h.assertTrue(MarchWalkerEntity.checkSpawnRules(ModEntities.MARCH_WALKER.get(),
                h.getLevel(), MobSpawnType.NATURAL, snow, random), "Walkers must stand on snow");
        h.assertTrue(MarchWalkerEntity.checkSpawnRules(ModEntities.MARCH_WALKER.get(),
                h.getLevel(), MobSpawnType.NATURAL, soil, random), "Walkers must stand on Ember soil");
        h.assertTrue(SpiritWispEntity.checkSpawnRules(ModEntities.SPIRIT_WISP.get(),
                h.getLevel(), MobSpawnType.NATURAL, snow, random), "Wisps must stand on snow");
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var steppe = biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:march_steppe"));
        var snowFields = biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:march_snow_fields"));
        var fen = biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:march_reed_fen"));
        var ember = biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:march_ember_wastes"));
        var crystal = biomes.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:march_crystal_fields"));
        h.assertTrue(hasCreature(steppe, EntityType.RABBIT) && hasCreature(steppe, CreatureEntities.type(CreatureProfile.DAWN_STAG)),
                "Steppe must graze stags and rabbits");
        h.assertTrue(hasMob(steppe, MobCategory.AMBIENT, EntityType.BAT), "Steppe caves must keep bats");
        h.assertTrue(hasCreature(snowFields, EntityType.RABBIT) && hasCreature(snowFields, CreatureEntities.type(CreatureProfile.LANTERN_FOX)),
                "Snow Fields must keep foxes and rabbits");
        h.assertTrue(hasCreature(snowFields, EntityType.POLAR_BEAR), "Snow Fields must keep polar bears");
        h.assertTrue(hasCreature(crystal, EntityType.RABBIT) && hasCreature(crystal, CreatureEntities.type(CreatureProfile.LANTERN_FOX)),
                "Crystal Fields must keep foxes and rabbits");
        h.assertTrue(hasCreature(fen, EntityType.FROG) && !fen.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap().isEmpty(),
                "Reed Fen must keep frogs and fish");
        h.assertTrue(hasMob(fen, MobCategory.UNDERGROUND_WATER_CREATURE, EntityType.GLOW_SQUID),
                "Reed Fen caves must keep glow squid");
        h.assertTrue(!hasCreature(ember, CreatureEntities.type(CreatureProfile.DAWN_STAG)),
                "Ember Wastes must stay sparse — no grazing herds");
        h.succeed();
    }
    private static boolean hasCreature(net.minecraft.world.level.biome.Biome biome, EntityType<?> type) {
        return hasMob(biome, MobCategory.CREATURE, type);
    }
    private static boolean hasMob(net.minecraft.world.level.biome.Biome biome, MobCategory category, EntityType<?> type) {
        if (biome == null) return false;
        for (var entry : biome.getMobSettings().getMobs(category).unwrap()) {
            if (entry.type == type) return true;
        }
        return false;
    }
    @GameTest(template="empty")
    public static void brushingIsRenewableAndCannotSpamOrHarvestBabies(GameTestHelper h) {
        var player=VerificationPlayers.inLevel(h);
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
        h.assertTrue(tk.darrow.tribalpower.world.MarchSkyMath.dayness(6000,0)>.9,"March day sky must peak at noon");
        h.assertTrue(tk.darrow.tribalpower.world.MarchSkyMath.dayness(18000,0)<.1,"March day sky must rest at midnight so the aurora can take the night");
        h.assertTrue(tk.darrow.tribalpower.world.MarchSkyMath.sunAlpha(1,0)>.9 && tk.darrow.tribalpower.world.MarchSkyMath.moonAlpha(1,0)<.05,"Noon keeps the spirit-sun and hides the loom-moon");
        h.assertTrue(tk.darrow.tribalpower.world.MarchSkyMath.moonAlpha(0,0)>.9 && tk.darrow.tribalpower.world.MarchSkyMath.sunAlpha(0,0)<.05,"Midnight keeps the loom-moon and hides the spirit-sun");
        float[] noon=tk.darrow.tribalpower.world.MarchSkyMath.zenith(1,0);
        float[] night=tk.darrow.tribalpower.world.MarchSkyMath.zenith(0,0);
        h.assertTrue(noon[1]>night[1] && night[2]>night[0],"Day zenith is teal; night zenith is indigo");
        h.succeed();
    }
}
