package tk.darrow.tribalpower.familiar;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import tk.darrow.tribalpower.camp.CampHooks;
import tk.darrow.tribalpower.camp.identity.CampStanding;
import tk.darrow.tribalpower.entity.LatticeAnimal;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/**
 * Use on an adult, unbonded tameable familiar. Gentle animals: 60% keep the charm on failure.
 * Remnants: Voice standing with their tribe, 40%, charm spent on failure. The lattice does not reroll.
 */
public class BondingCharmItem extends Item {
    public static final float CHANCE=0.6F,HOSTILE_CHANCE=0.4F;
    public BondingCharmItem(Properties properties) { super(properties); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if(!(target instanceof Familiar familiar))return InteractionResult.PASS;
        if(familiar.asMob().isBaby() || familiar.isBonded()) {
            if(!player.level().isClientSide)player.displayClientMessage(Component.translatable(familiar.asMob().isBaby()?"message.tribalpower.familiar.too_young":"message.tribalpower.familiar.already_bonded",familiar.asMob().getDisplayName()),true);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if(!FamiliarRoster.tameable(familiar.profile())) {
            if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.tribalpower.familiar.will_not",familiar.asMob().getDisplayName()),true);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        TribeDefinition tribe=FamiliarRoster.voiceTribe(familiar.profile());
        if(tribe!=null) {
            if(!(player instanceof ServerPlayer server) || CampStanding.effectiveStanding(server,tribe)<TribeRank.VOICE.threshold()) {
                if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.tribalpower.familiar.need_voice",tribe.displayNameComponent(),familiar.asMob().getDisplayName()),true);
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        if(player.level() instanceof ServerLevel level)attempt(level,player,familiar,stack);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    public static boolean attempt(ServerLevel level,Player player,LatticeAnimal animal,ItemStack charm) { return attempt(level,player,(Familiar)animal,charm,false); }
    public static boolean attempt(ServerLevel level,Player player,LatticeAnimal animal,ItemStack charm,boolean force) { return attempt(level,player,(Familiar)animal,charm,force); }
    public static boolean attempt(ServerLevel level,Player player,Familiar familiar,ItemStack charm) { return attempt(level,player,familiar,charm,false); }
    /** Bonding with an optional forced success (used by tests and rites). Creative players keep the charm. */
    public static boolean attempt(ServerLevel level,Player player,Familiar familiar,ItemStack charm,boolean force) {
        boolean hostile=FamiliarRoster.voiceTribe(familiar.profile())!=null;
        float chance=hostile?HOSTILE_CHANCE:CHANCE;
        boolean success=force || level.random.nextFloat()<chance;
        return conclude(level,player,familiar,charm,success,hostile);
    }
    /** Deterministic finish used by tests: {@code success} is the roll. */
    public static boolean conclude(ServerLevel level,Player player,Familiar familiar,ItemStack charm,boolean success,boolean hostile) {
        var mob=familiar.asMob();
        double x=mob.getX(),y=mob.getY()+mob.getBbHeight()*.8,z=mob.getZ();
        if(success) {
            familiar.bond(player);
            if(!player.getAbilities().instabuild)charm.shrink(1);
            level.sendParticles(ParticleTypes.HEART,x,y,z,7,.5,.4,.5,.02);
            level.playSound(null,mob.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.NEUTRAL,.8F,1.3F);
            player.displayClientMessage(Component.translatable("message.tribalpower.familiar.bonded",mob.getDisplayName()),false);
            FamiliarSlots.afterBond(familiar,player);
            CampHooks.award(level,player.getUUID(),"first_bond");
        }
        else {
            if(hostile && !player.getAbilities().instabuild)charm.shrink(1);
            level.sendParticles(ParticleTypes.SMOKE,x,y,z,7,.5,.4,.5,.02);
            level.playSound(null,mob.blockPosition(),SoundEvents.FOX_SNIFF,SoundSource.NEUTRAL,.8F,.9F);
        }
        return success;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.bonding_charm.desc"));
    }
}
