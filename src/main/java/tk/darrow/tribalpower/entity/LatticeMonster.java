package tk.darrow.tribalpower.entity;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public class LatticeMonster extends Monster {
    public LatticeMonster(EntityType<? extends Monster> type,Level level) {
        super(type,level);xpReward=profile().health>=35?8:5;
        if(profile().flying) { moveControl=new FlyingMoveControl(this,12,true);setNoGravity(true); }
    }
    public CreatureProfile profile() { return CreatureProfile.of(getType()); }
    @Override protected PathNavigation createNavigation(Level level) {
        return CreatureProfile.of(getType()).flying?new FlyingPathNavigation(this,level):super.createNavigation(level);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,profile().ranged()?new SpiritCastGoal():new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(5,profile().flying?new WaterAvoidingRandomFlyingGoal(this,.8):new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public boolean canCastAt(LivingEntity target) { return target.isAlive() && distanceToSqr(target)<=144 && hasLineOfSight(target); }
    @Override public boolean doHurtTarget(Entity entity) {
        boolean hit=super.doHurtTarget(entity);
        if(hit && entity instanceof LivingEntity target) applyVoice(target);
        return hit;
    }
    private void applyVoice(LivingEntity target) {
        switch(profile().attack) {
            case "ember","bolt" -> target.igniteForSeconds(2);
            case "root","weave","chill" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
            case "venom" -> target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
            case "weaken" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,0));
            case "shove","gust" -> { target.knockback(.45,getX()-target.getX(),getZ()-target.getZ());target.hurtMarked=true; }
            default -> { }
        }
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) { return !profile().flying && super.causeFallDamage(distance,multiplier,source); }
    @Override protected SoundEvent getAmbientSound() { return profile().ranged()?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.SOUL_SAND_STEP; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.AMETHYST_BLOCK_BREAK; }
    /** Telegraph, line of sight and bounded range apply throughout the cast; no terrain edits. */
    private final class SpiritCastGoal extends Goal {
        private int cooldown,windup;
        SpiritCastGoal() { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() { return getTarget()!=null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { windup=0;getNavigation().stop(); }
        @Override public void tick() {
            var target=getTarget();if(target==null)return;
            if(cooldown>0)cooldown--;
            getLookControl().setLookAt(target,30,30);
            if(!canCastAt(target)) { windup=0;getNavigation().moveTo(target,1);return; }
            getNavigation().stop();if(cooldown>0)return;
            if(level() instanceof ServerLevel server && windup%4==0)server.sendParticles(ParticleTypes.END_ROD,getX(),getEyeY(),getZ(),2,.12,.12,.12,.01);
            if(++windup<16)return;
            windup=0;cooldown=70;
            if(target.hurt(damageSources().indirectMagic(LatticeMonster.this,LatticeMonster.this),(float)profile().damage))applyVoice(target);
            level().playSound(null,blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.HOSTILE,.7F,.8F);
        }
    }
}
