package tk.darrow.tribalpower.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/**
 * Server-side species abilities of bonded animals (design 3.0 §5).
 * Lantern Fox: Night Vision for an owner within 8 blocks (refreshed every 2 s), ore glints within 6 blocks sent to
 * the owner every 4 s, and a travelling {@code spirit_light} block. Mossback and Dawn Stag abilities are interaction
 * driven and live on {@link LatticeAnimal}.
 */
public final class FamiliarAbilities {
    public static final int NIGHT_VISION_RANGE=8,ORE_RANGE=6,LIGHT_PERIOD=10;
    private FamiliarAbilities(){}
    public static void tick(LatticeAnimal animal) {
        if(!(animal.level() instanceof ServerLevel level) || animal.profile()!=CreatureProfile.LANTERN_FOX)return;
        long time=level.getGameTime();
        var owner=animal.getOwner();
        if(time%40==0 && owner instanceof ServerPlayer player && player.level()==level && player.distanceToSqr(animal)<=NIGHT_VISION_RANGE*NIGHT_VISION_RANGE)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,300,0,true,false,true));
        if(time%80==0 && owner instanceof ServerPlayer player && player.level()==level && player.distanceToSqr(animal)<=48*48)markOres(level,animal,player);
        if(time%LIGHT_PERIOD==0)carryLight(level,animal);
    }
    /** End-rod glints on every ore block within {@link #ORE_RANGE}, visible only to the owner. */
    public static int markOres(ServerLevel level,LatticeAnimal animal,ServerPlayer owner) {
        BlockPos center=animal.blockPosition();int marked=0;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-ORE_RANGE,-ORE_RANGE,-ORE_RANGE),center.offset(ORE_RANGE,ORE_RANGE,ORE_RANGE))) {
            if(!level.getBlockState(pos).is(Tags.Blocks.ORES))continue;
            level.sendParticles(owner,ParticleTypes.END_ROD,true,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.35,.35,.35,0);
            if(++marked>=96)break;
        }
        return marked;
    }
    /** Place a Spirit Light where the fox stands (if that block is plain air) and clear the previous one. */
    public static void carryLight(ServerLevel level,LatticeAnimal animal) {
        BlockPos pos=animal.blockPosition();
        BlockPos last=animal.lastLight();
        if(pos.equals(last))return;
        var state=level.getBlockState(pos);
        boolean placed=false;
        if(state.isAir() && !state.is(FamiliarRegistry.SPIRIT_LIGHT.get()) && level.getWorldBorder().isWithinBounds(pos)) {
            level.setBlock(pos,FamiliarRegistry.SPIRIT_LIGHT.get().defaultBlockState(),3);placed=true;
        }
        if(last!=null && level.hasChunkAt(last) && level.getBlockState(last).is(FamiliarRegistry.SPIRIT_LIGHT.get()))level.setBlock(last,Blocks.AIR.defaultBlockState(),3);
        animal.setLastLight(placed?pos.immutable():null);
    }
    /** Remove the fox's light on death or despawn. */
    public static void clearLight(LatticeAnimal animal) {
        BlockPos last=animal.lastLight();
        if(last==null || !(animal.level() instanceof ServerLevel level))return;
        if(level.hasChunkAt(last) && level.getBlockState(last).is(FamiliarRegistry.SPIRIT_LIGHT.get()))level.setBlock(last,Blocks.AIR.defaultBlockState(),3);
        animal.setLastLight(null);
    }
}
