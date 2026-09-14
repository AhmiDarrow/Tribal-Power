package tk.darrow.tribalpower.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeAnimal;
import tk.darrow.tribalpower.entity.LatticeMonster;

/**
 * Server-side species abilities of bonded familiars.
 * Lantern Fox: Night Vision, ore glints, travelling {@code spirit_light}.
 * Storm Moth: sitting {@code spirit_click} redstone pulse.
 * Cinder Imp: Pulse refund only while the owner hand-drums.
 * Mourning Bell: cleanses common hexes (never Wither).
 * Echo Weaver: five-slot pouch that gathers nearby drops.
 * Rift Hound Track: glints the last foe that hurt the owner.
 */
public final class FamiliarAbilities {
    public static final int NIGHT_VISION_RANGE=8,ORE_RANGE=6,LIGHT_PERIOD=10;
    public static final int IMP_RANGE=8,IMP_REFUND=4,IMP_TEMPO_BONUS=2;
    public static final int BELL_RANGE=8,CLICK_PERIOD=20,CLICK_TICKS=2,CLICK_TRUE_TICKS=4;
    public static final int WEAVER_RANGE=4,WEAVER_GATHER=8,TRACK_RANGE=48;
    private FamiliarAbilities(){}

    public static void tick(LatticeAnimal animal) {
        if(!(animal.level() instanceof ServerLevel level) || animal.profile()!=CreatureProfile.LANTERN_FOX)return;
        long time=level.getGameTime();
        boolean night=!level.isDay();
        int nvPeriod=animal.lattice().abilityPeriod(40,night);
        int orePeriod=animal.lattice().abilityPeriod(80,night);
        var owner=animal.getOwner();
        if(time%nvPeriod==0 && owner instanceof ServerPlayer player && player.level()==level && player.distanceToSqr(animal)<=NIGHT_VISION_RANGE*NIGHT_VISION_RANGE)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,300,0,true,false,true));
        if(time%orePeriod==0 && owner instanceof ServerPlayer player && player.level()==level && player.distanceToSqr(animal)<=48*48)markOres(level,animal,player);
        if(time%LIGHT_PERIOD==0)carryLight(level,animal);
    }

    public static void tickMonster(LatticeMonster monster) {
        if(!(monster.level() instanceof ServerLevel level) || !monster.isBonded() || monster.isBaby())return;
        switch(monster.profile()) {
            case STORM_MOTH -> { if(monster.isSitting() && level.getGameTime()%CLICK_PERIOD==0)placeClick(level,monster); }
            case MOURNING_BELL -> { if(!monster.isSitting())cleanse(level,monster); }
            case ECHO_WEAVER -> { if(!monster.isSitting())forage(level,monster); }
            case RIFT_HOUND -> track(level,monster);
            default -> { }
        }
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
            var light=FamiliarRegistry.SPIRIT_LIGHT.get().defaultBlockState().setValue(SpiritLightBlock.BRIGHT,animal.lattice().expressed(FamiliarData.Mark.GLOW_VEIN));
            level.setBlock(pos,light,3);placed=true;
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

    /** Sitting Storm Moth: one invisible redstone-15 click. Click-true holds two extra ticks. */
    public static boolean placeClick(ServerLevel level,LatticeMonster moth) {
        BlockPos pos=moth.blockPosition();
        var state=level.getBlockState(pos);
        if(!state.isAir() && !state.is(FamiliarRegistry.SPIRIT_CLICK.get()))return false;
        if(!level.getWorldBorder().isWithinBounds(pos))return false;
        int hold=moth.lattice().expressed(FamiliarData.Mark.CLICK_TRUE)?CLICK_TRUE_TICKS:CLICK_TICKS;
        level.setBlock(pos,FamiliarRegistry.SPIRIT_CLICK.get().defaultBlockState(),3);
        level.scheduleTick(pos,FamiliarRegistry.SPIRIT_CLICK.get(),hold);
        moth.setLastClick(pos.immutable());
        return true;
    }
    public static void clearClick(LatticeMonster moth) {
        BlockPos last=moth.lastClick();
        if(last==null || !(moth.level() instanceof ServerLevel level))return;
        if(level.hasChunkAt(last) && level.getBlockState(last).is(FamiliarRegistry.SPIRIT_CLICK.get()))level.removeBlock(last,false);
        moth.setLastClick(null);
    }

    /**
     * Refund Pulse into the Drumheart the owner just struck by hand. Sitting Imps and redstone beats do nothing.
     * At most one Imp refunds per beat so two helpers cannot farm Pulse.
     */
    public static int onOwnerDrum(ServerLevel level,BlockPos drum,Player player,DrumheartBlockEntity be) {
        AABB box=new AABB(drum).inflate(IMP_RANGE);
        for(LatticeMonster imp:level.getEntitiesOfClass(LatticeMonster.class,box,m->m.isAlive()
                && m.profile()==CreatureProfile.CINDER_IMP && m.isOwnedBy(player) && !m.isSitting() && !m.isBaby())) {
            if(imp.distanceToSqr(drum.getX()+.5,drum.getY()+.5,drum.getZ()+.5)>IMP_RANGE*IMP_RANGE)continue;
            int extra=IMP_REFUND+(imp.lattice().expressed(FamiliarData.Mark.TEMPO)?IMP_TEMPO_BONUS:0);
            int gained=be.insertPulse(extra,false);
            if(gained>0)level.sendParticles(ParticleTypes.FLAME,imp.getX(),imp.getY()+imp.getBbHeight()*.6,imp.getZ(),4,.2,.2,.2,.01);
            return gained;
        }
        return 0;
    }

    /** Poison, weakness, hunger, mining fatigue, blindness, nausea. Still also slowness. Never Wither. */
    public static int cleanse(ServerLevel level,LatticeMonster bell) {
        var owner=bell.getOwner();
        if(!(owner instanceof ServerPlayer player) || player.level()!=level)return 0;
        if(player.distanceToSqr(bell)>BELL_RANGE*BELL_RANGE)return 0;
        long time=level.getGameTime();
        int period=bell.lattice().abilityPeriod(80,!level.isDay());
        if(time%period!=0 && time!=0)return 0;
        return cleanse(player,bell.lattice().expressed(FamiliarData.Mark.STILL));
    }
    public static int cleanse(LivingEntity target,boolean still) {
        int cleared=0;
        if(target.removeEffect(MobEffects.POISON))cleared++;
        if(target.removeEffect(MobEffects.WEAKNESS))cleared++;
        if(target.removeEffect(MobEffects.HUNGER))cleared++;
        if(target.removeEffect(MobEffects.DIG_SLOWDOWN))cleared++;
        if(target.removeEffect(MobEffects.BLINDNESS))cleared++;
        if(target.removeEffect(MobEffects.CONFUSION))cleared++;
        if(still && target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN))cleared++;
        return cleared;
    }

    public static int forage(ServerLevel level,LatticeMonster weaver) {
        double range=weaver.lattice().expressed(FamiliarData.Mark.GATHER)?WEAVER_GATHER:WEAVER_RANGE;
        int taken=0;
        for(ItemEntity drop:level.getEntitiesOfClass(ItemEntity.class,weaver.getBoundingBox().inflate(range),
                e->e.isAlive() && !e.hasPickUpDelay() && !e.getItem().isEmpty())) {
            ItemStack stack=drop.getItem();
            ItemStack leftover=weaver.pouch().addItem(stack.copy());
            if(leftover.getCount()==stack.getCount())continue;
            taken++;
            if(leftover.isEmpty())drop.discard();
            else drop.setItem(leftover);
            if(taken>=5)break;
        }
        var owner=weaver.getOwner();
        if(owner instanceof ServerPlayer player && player.level()==level && !weaver.pouch().isEmpty()
                && player.distanceToSqr(weaver)<=range*range)
            tossToOwner(weaver,player);
        return taken;
    }
    private static void tossToOwner(LatticeMonster weaver,Player owner) {
        for(int i=0;i<LatticeMonster.POUCH_SLOTS;i++) {
            ItemStack stack=weaver.pouch().getItem(i);
            if(stack.isEmpty())continue;
            if(owner.addItem(stack))weaver.pouch().setItem(i,ItemStack.EMPTY);
            // Inventory full: leave the stack in the pouch. Spawning it with the default pickup delay
            // lets forage pocket it again on the next beat.
            return;
        }
    }

    public static void track(ServerLevel level,LatticeMonster hound) {
        var owner=hound.getOwner();
        if(!(owner instanceof ServerPlayer player) || player.level()!=level)return;
        LivingEntity foe=owner.getLastHurtByMob();
        if(foe!=null && foe.isAlive() && !hound.isOwnedBy(foe))hound.setLastOwnerAttacker(foe.getUUID());
        if(!hound.lattice().expressed(FamiliarData.Mark.TRACK) || hound.lastOwnerAttacker()==null)return;
        if(level.getGameTime()%20!=0)return;
        var id=hound.lastOwnerAttacker();
        LivingEntity living=null;
        for(LivingEntity candidate:level.getEntitiesOfClass(LivingEntity.class,hound.getBoundingBox().inflate(TRACK_RANGE),e->e.isAlive() && e.getUUID().equals(id))) {
            living=candidate;break;
        }
        if(living==null)return;
        level.sendParticles(player,ParticleTypes.END_ROD,true,living.getX(),living.getY()+living.getBbHeight()*.6,living.getZ(),6,.25,.4,.25,.01);
    }
}
