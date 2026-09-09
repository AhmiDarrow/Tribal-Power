package tk.darrow.tribalpower.familiar;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import tk.darrow.tribalpower.entity.LatticeAnimal;

/**
 * Use on an adult, unbonded Lantern Fox, Mossback or Dawn Stag: 60% chance per attempt. Success bonds the animal
 * and consumes the charm (hearts); failure keeps the charm (smoke).
 */
public class BondingCharmItem extends Item {
    public static final float CHANCE=0.6F;
    public BondingCharmItem(Properties properties) { super(properties); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if(!(target instanceof LatticeAnimal animal))return InteractionResult.PASS;
        if(animal.isBaby() || animal.isBonded()) {
            if(!player.level().isClientSide)player.displayClientMessage(Component.translatable(animal.isBaby()?"message.tribalpower.familiar.too_young":"message.tribalpower.familiar.already_bonded",animal.getDisplayName()),true);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if(player.level() instanceof ServerLevel level)attempt(level,player,animal,stack);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    /** Server-side bonding roll. @return true when the animal bonded. */
    public static boolean attempt(ServerLevel level,Player player,LatticeAnimal animal,ItemStack charm) { return attempt(level,player,animal,charm,false); }
    /** Bonding with an optional forced success (used by tests and rites). Creative players keep the charm. */
    public static boolean attempt(ServerLevel level,Player player,LatticeAnimal animal,ItemStack charm,boolean force) {
        boolean success=force || level.random.nextFloat()<CHANCE;
        double x=animal.getX(),y=animal.getY()+animal.getBbHeight()*.8,z=animal.getZ();
        if(success) {
            animal.bond(player);
            if(!player.getAbilities().instabuild)charm.shrink(1);
            level.sendParticles(ParticleTypes.HEART,x,y,z,7,.5,.4,.5,.02);
            level.playSound(null,animal.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.NEUTRAL,.8F,1.3F);
            player.displayClientMessage(Component.translatable("message.tribalpower.familiar.bonded",animal.getDisplayName()),false);
            CampHooks.award(level,player.getUUID(),"first_bond");
        }
        else {
            level.sendParticles(ParticleTypes.SMOKE,x,y,z,7,.5,.4,.5,.02);
            level.playSound(null,animal.blockPosition(),SoundEvents.FOX_SNIFF,SoundSource.NEUTRAL,.8F,.9F);
        }
        return success;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.bonding_charm.desc"));
    }
}
